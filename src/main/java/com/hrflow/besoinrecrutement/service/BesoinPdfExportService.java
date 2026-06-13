package com.hrflow.besoinrecrutement.service;

import com.hrflow.ai.config.AiFallbackProperties;
import com.hrflow.besoinrecrutement.dto.BesoinPdfExportRequest;
import com.hrflow.besoinrecrutement.exception.BesoinRecrutementNotFoundException;
import com.hrflow.besoinrecrutement.model.BesoinRecrutement;
import com.hrflow.besoinrecrutement.model.PrioriteBesoin;
import com.hrflow.besoinrecrutement.repositories.BesoinRecrutementRepository;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.fichedeposte.model.NiveauEtudes;
import com.hrflow.storage.service.MinioService;
import com.hrflow.users.entities.User;
import com.hrflow.users.repositories.UserRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * Génère un document PDF narratif (Note de présentation) pour un besoin de
 * recrutement, à destination de la Direction Générale pour validation.
 *
 * <p>Document signé par DRH + DG + Directeur (initiateur du besoin).
 * Sémantiquement, le PDF matérialise la demande qui sera approuvée — donc
 * généré sur le besoin (et non sur le projet créé après acceptation).
 */
@Service
public class BesoinPdfExportService {

    private static final Logger log = LoggerFactory.getLogger(BesoinPdfExportService.class);

    private static final DateTimeFormatter DATE_FR =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);

    private final BesoinRecrutementRepository besoinRepository;
    private final AiFallbackProperties        aiProperties;
    private final TemplateEngine              templateEngine;
    private final MinioService                minioService;
    private final UserRepository              userRepository;

    public BesoinPdfExportService(
            BesoinRecrutementRepository besoinRepository,
            AiFallbackProperties aiProperties,
            TemplateEngine templateEngine,
            MinioService minioService,
            UserRepository userRepository) {
        this.besoinRepository = besoinRepository;
        this.aiProperties     = aiProperties;
        this.templateEngine   = templateEngine;
        this.minioService     = minioService;
        this.userRepository   = userRepository;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generate(Long besoinId, BesoinPdfExportRequest request) {
        BesoinRecrutement besoin = besoinRepository.findWithDetailsById(besoinId)
                .orElseThrow(() -> new BesoinRecrutementNotFoundException(besoinId));

        String html = renderTemplate(besoin, request);
        return convertToPdf(html);
    }

    // ── Template rendering ────────────────────────────────────────────────────

    private String renderTemplate(BesoinRecrutement besoin, BesoinPdfExportRequest request) {
        FicheDePoste                          fiche   = besoin.getFicheDePoste();
        AiFallbackProperties.CompanyProfile   company = aiProperties.company();

        Context ctx = new Context(Locale.FRENCH);

        ctx.setVariable("company",      company);
        ctx.setVariable("besoin",       besoin);
        ctx.setVariable("ficheDePoste", fiche);
        ctx.setVariable("fields",       request);
        ctx.setVariable("generatedAt",  LocalDate.now().format(DATE_FR));
        ctx.setVariable("besoinRef",    "BES-" + besoin.getId());

        // Paragraphe d'introduction
        ctx.setVariable("introParagraph", buildIntroParagraph(fiche, request));

        // Variables pré-calculées (évite la logique enum dans Thymeleaf)
        ctx.setVariable("prioriteLibelle",
                besoin.getPriorite() != null ? prioriteLibelle(besoin.getPriorite()) : null);
        ctx.setVariable("niveauEtudesLibelle",
                fiche.getNiveauEtudes() != null ? niveauEtudesLibelle(fiche.getNiveauEtudes()) : null);
        ctx.setVariable("dateSouhaiteeFormatted",
                besoin.getDateSouhaitee() != null ? besoin.getDateSouhaitee().format(DATE_FR) : null);

        // Signatures — toujours présentes (image si configurée, nom+fonction sinon)
        ctx.setVariable("drhSignature",       buildSignatureDto(findDrhUser()));
        ctx.setVariable("dgSignature",        buildSignatureDto(findDgUser()));
        ctx.setVariable("directeurSignature", buildSignatureDto(besoin.getDirecteur()));

        return templateEngine.process("besoin-export", ctx);
    }

    // ── Intro paragraph ───────────────────────────────────────────────────────

    private String buildIntroParagraph(FicheDePoste fiche, BesoinPdfExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("La présente note soumet à l’approbation de la Direction Générale ");
        sb.append("une demande de recrutement pour le poste de ")
          .append(fiche.getIntitulePoste());

        if (request.contexte().direction() && fiche.getDirection() != null) {
            sb.append(", initiée par la direction ")
              .append(fiche.getDirection().getNom());
        }
        sb.append(". ");

        boolean hasFiche = request.fiche().missionPrincipale()
                || request.fiche().activitesPrincipales()
                || request.fiche().competencesTechniques()
                || request.fiche().competencesManageriales()
                || request.fiche().niveauEtudes()
                || request.fiche().domaineFormation()
                || request.fiche().anneesExperience();

        sb.append("Ce document présente le contexte de la demande");
        if (hasFiche) {
            sb.append(" ainsi que les exigences du profil et les attributions du poste");
        }
        sb.append(".");
        return sb.toString();
    }

    // ── Signature helpers ─────────────────────────────────────────────────────

    private User findDrhUser() {
        return userRepository.findFirstByRoleName("DRH").orElse(null);
    }

    private User findDgUser() {
        return userRepository.findFirstByRoleName("DG").orElse(null);
    }

    private SignatureDto buildSignatureDto(User user) {
        if (user == null) return null;

        String dataUrl = null;
        if (user.getSignatureKey() != null) {
            try {
                byte[] bytes    = minioService.getBytes(user.getSignatureKey());
                String mimeType = user.getSignatureContentType() != null
                        ? user.getSignatureContentType() : "image/png";
                dataUrl = "data:" + mimeType + ";base64,"
                        + Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                log.warn("[PDF] Signature non chargée pour userId={} : {}",
                        user.getId(), e.getMessage());
            }
        }
        return new SignatureDto(dataUrl, user.getFullName());
    }

    /** DTO interne pour transmettre les données de signature au template Thymeleaf. */
    public record SignatureDto(String dataUrl, String fullName) {}

    // ── PDF conversion ────────────────────────────────────────────────────────

    private byte[] convertToPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    // ── Label helpers ─────────────────────────────────────────────────────────

    private String prioriteLibelle(PrioriteBesoin priorite) {
        return switch (priorite) {
            case HAUTE   -> "Haute";
            case NORMALE -> "Normale";
            case BASSE   -> "Basse";
        };
    }

    private String niveauEtudesLibelle(NiveauEtudes niveau) {
        return switch (niveau) {
            case PAS_IMPORTANT -> "Non précisé";
            case NIVEAU_BAC    -> "Niveau Bac";
            case BAC           -> "Baccalauréat";
            case BAC_PLUS_2    -> "Bac+2";
            case BAC_PLUS_3    -> "Bac+3";
            case BAC_PLUS_5    -> "Bac+5";
            case DOCTORAT      -> "Doctorat";
        };
    }
}
