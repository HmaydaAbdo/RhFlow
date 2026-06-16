package com.hrflow.projetrecrutement.repositories;

import com.hrflow.projetrecrutement.model.ProjetRecrutement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProjetRecrutementRepository
        extends JpaRepository<ProjetRecrutement, Long>,
                JpaSpecificationExecutor<ProjetRecrutement> {

    @EntityGraph(attributePaths = {
        "ficheDePoste",
        "ficheDePoste.direction",
        "ficheDePoste.direction.directeur",
        "besoinRecrutement",
        "besoinRecrutement.directeur"
    })
    Optional<ProjetRecrutement> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
        "ficheDePoste",
        "ficheDePoste.direction",
        "ficheDePoste.direction.directeur"
    })
    @Override
    Page<ProjetRecrutement> findAll(Specification<ProjetRecrutement> spec, Pageable pageable);

    boolean existsByBesoinRecrutementId(Long besoinId);

    /** Vérifie l'unicité de l'objet candidature en excluant le projet en cours de modification. */
    boolean existsByObjetCandidatureIgnoreCaseAndIdNot(String objetCandidature, Long id);

    /**
     * Lookup utilisé par l'ingestion n8n (IngestionService) : retrouve un projet
     * dont l'objet de candidature <strong>contient</strong> le fragment fourni
     * (insensible à la casse), <strong>tous statuts confondus</strong>.
     *
     * <p>Le format canonique des objets de candidature est généré par
     * {@code ProjetRecrutementService.generateObjetCandidature()} :
     * <pre>"Candidature – {intitule} – Réf. {id:0000}"</pre>
     * (ex : {@code "Candidature – Développeur Java Senior – Réf. 0001"}).
     *
     * <p>n8n n'extrait du sujet d'email que le fragment unique {@code "Réf. NNNN"}.
     * Comme {@code NNNN} est dérivé de l'ID du projet (unique par construction),
     * le {@code Containing} retourne 0 ou 1 résultat — jamais d'ambiguïté.
     *
     * <p>Le service applique ensuite la règle métier « doit être OUVERT » pour
     * distinguer 404 (introuvable) de 410 (fermé).
     */
    @EntityGraph(attributePaths = {
        "ficheDePoste",
        "ficheDePoste.direction"
    })
    Optional<ProjetRecrutement> findByObjetCandidatureContainingIgnoreCase(String fragment);

    @Query("""
        SELECT p FROM ProjetRecrutement p
        JOIN FETCH p.ficheDePoste f
        JOIN FETCH f.direction d
        JOIN FETCH d.directeur
        WHERE p.besoinRecrutement.id = :besoinId
    """)
    Optional<ProjetRecrutement> findByBesoinRecrutementId(Long besoinId);
}
