package com.hrflow.seeders;


import com.hrflow.besoinrecrutement.repositories.BesoinRecrutementRepository;
import com.hrflow.direction.entities.Direction;
import com.hrflow.direction.repositories.DirectionRepository;
import com.hrflow.fichedeposte.model.FicheDePoste;
import com.hrflow.fichedeposte.model.NiveauEtudes;
import com.hrflow.fichedeposte.repositories.FicheDePosteRepository;

import com.hrflow.roles.entities.Role;
import com.hrflow.users.entities.User;
import com.hrflow.roles.repositories.RoleRepository;
import com.hrflow.users.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               DirectionRepository directionRepository,
                               FicheDePosteRepository ficheDePosteRepository,
                               BesoinRecrutementRepository besoinRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {

            // ========== SEED ROLES ==========
            List<String> roleNames = List.of("ADMIN", "DRH", "DIRECTEUR");

            Map<String, Role> roles = new HashMap<>();
            for (String name : roleNames) {
                Role role = roleRepository.findByRoleName(name)
                        .orElseGet(() -> roleRepository.save(new Role(name)));
                roles.put(name, role);
            }

            System.out.println("==> Roles seeded: " + roleNames);

            // ========== SEED USERS ==========
            record SeedUser(String email, String password, String fullName, String gsm, List<String> roleNames) {}

            List<SeedUser> seedUsers = List.of(
                    new SeedUser("admin@rh.ma",    "00000000", "Hmayda Abdessamad", "0677606492", List.of("ADMIN")),
                    new SeedUser("drh@rh.ma",       "00000000", "MOUSTAJID ISMAIL",  "0648454346", List.of("DRH", "DIRECTEUR")),
                    new SeedUser("directeur@rh.ma", "00000000", "MEHDI TROUSSI",     "0662150238", List.of("DIRECTEUR")),
                    new SeedUser("yamani@rh.ma",    "00000000", "YAMANI ABDLHAFID",  "0600000001", List.of("DIRECTEUR"))
            );

            for (SeedUser s : seedUsers) {
                if (userRepository.findByEmail(s.email()).isEmpty()) {
                    User user = new User();
                    user.setEmail(s.email());
                    user.setFullName(s.fullName());
                    user.setGsm(s.gsm());
                    user.setPassword(passwordEncoder.encode(s.password()));
                    user.setEnabled(true);
                    List<Role> userRoles = new ArrayList<>();
                    for (String rn : s.roleNames()) userRoles.add(roles.get(rn));
                    user.setRoles(userRoles);
                    userRepository.save(user);
                    System.out.println("==> User créé : " + s.email() + " (" + s.fullName() + ")");
                } else {
                    // Ajouter les rôles manquants si l'utilisateur existe déjà
                    // findWithRolesByEmail charge les rôles eagerly (évite LazyInitializationException)
                    User existing = userRepository.findWithRolesByEmail(s.email()).get();
                    List<String> existingRoleNames = existing.getRoles().stream()
                            .map(Role::getRoleName).toList();
                    boolean updated = false;
                    for (String rn : s.roleNames()) {
                        if (!existingRoleNames.contains(rn)) {
                            existing.getRoles().add(roles.get(rn));
                            updated = true;
                        }
                    }
                    if (updated) {
                        userRepository.save(existing);
                        System.out.println("==> Rôles mis à jour : " + s.email() + " -> " + s.roleNames());
                    }
                }
            }

            // ========== SEED DIRECTIONS ==========
            record SeedDirection(String nom, String directeurEmail) {}

            List<SeedDirection> seedDirections = List.of(
                    new SeedDirection("Finance",             "yamani@rh.ma"),
                    new SeedDirection("Ressources Humaines", "drh@rh.ma"),
                    new SeedDirection("Développement",       "directeur@rh.ma")
            );

            for (SeedDirection d : seedDirections) {
                if (!directionRepository.existsByNom(d.nom())) {
                    User directeur = userRepository.findByEmail(d.directeurEmail())
                            .orElseThrow(() -> new IllegalStateException("Directeur introuvable : " + d.directeurEmail()));
                    Direction direction = new Direction();
                    direction.setNom(d.nom());
                    direction.setDirecteur(directeur);
                    directionRepository.save(direction);
                    System.out.println("==> Direction créée : " + d.nom() + " (directeur: " + directeur.getFullName() + ")");
                }
            }

            // ========== SEED FICHES DE POSTE ==========
            record SeedFiche(
                    String directionNom,
                    String intitule,
                    String missionPrincipale,
                    String activitesPrincipales,
                    String activitesSecondaires,
                    NiveauEtudes niveauEtudes,
                    String domaineFormation,
                    int anneesExperience,
                    String competencesTechniques,
                    String competencesManageriales
            ) {}

            List<SeedFiche> seedFiches = List.of(

                    // --- Finance ---
                    new SeedFiche(
                            "Finance",
                            "Contrôleur de Gestion",
                            "Piloter la performance financière de la direction et assurer le suivi budgétaire.",
                            "Élaboration des tableaux de bord financiers, suivi des écarts budgétaires, analyse des coûts.",
                            "Participation aux audits internes, accompagnement des responsables opérationnels.",
                            NiveauEtudes.BAC_PLUS_5,
                            "Finance / Contrôle de Gestion",
                            5,
                            "Maîtrise d'Excel avancé, ERP (SAP ou Sage), analyse financière, modélisation budgétaire.",
                            "Rigueur, capacité d'analyse, sens de la communication, esprit de synthèse."
                    ),
                    new SeedFiche(
                            "Finance",
                            "Comptable Senior",
                            "Garantir la fiabilité des comptes et assurer la conformité comptable et fiscale.",
                            "Tenue de la comptabilité générale et analytique, établissement des états financiers, déclarations fiscales.",
                            "Participation à la clôture annuelle, coordination avec les commissaires aux comptes.",
                            NiveauEtudes.BAC_PLUS_3,
                            "Comptabilité / Finance",
                            4,
                            "Comptabilité générale, fiscalité marocaine, logiciels comptables (Sage, Ciel), Excel.",
                            "Précision, intégrité, organisation, respect des délais."
                    ),
                    new SeedFiche(
                            "Finance",
                            "Analyste Financier",
                            "Analyser la rentabilité des projets et fournir des recommandations financières stratégiques.",
                            "Modélisation financière, analyse de la trésorerie, évaluation des investissements, reporting.",
                            "Veille sur les évolutions réglementaires financières, support aux décisions d'investissement.",
                            NiveauEtudes.BAC_PLUS_5,
                            "Finance / Économie",
                            3,
                            "Modélisation financière, Excel avancé, Power BI, outils d'analyse de données.",
                            "Esprit analytique, force de proposition, capacité à synthétiser l'information."
                    ),

                    // --- Ressources Humaines ---
                    new SeedFiche(
                            "Ressources Humaines",
                            "Chargé de Recrutement",
                            "Gérer le processus de recrutement de bout en bout pour répondre aux besoins en talents.",
                            "Rédaction et diffusion des offres, sourcing, conduite des entretiens, suivi des candidatures.",
                            "Participation aux forums emploi, développement du vivier de candidats, reporting recrutement.",
                            NiveauEtudes.BAC_PLUS_3,
                            "Ressources Humaines / Psychologie",
                            2,
                            "ATS (logiciel de suivi des candidatures), LinkedIn Recruiter, techniques d'entretien.",
                            "Écoute active, sens du contact, organisation, réactivité."
                    ),
                    new SeedFiche(
                            "Ressources Humaines",
                            "Responsable Formation",
                            "Concevoir et piloter le plan de développement des compétences de l'entreprise.",
                            "Analyse des besoins en formation, élaboration du plan de formation, coordination avec les organismes externes.",
                            "Suivi des évaluations post-formation, gestion du budget formation, veille pédagogique.",
                            NiveauEtudes.BAC_PLUS_5,
                            "Ressources Humaines / Sciences de l'éducation",
                            4,
                            "Ingénierie pédagogique, outils LMS, législation formation professionnelle.",
                            "Sens pédagogique, leadership, capacité à fédérer, vision stratégique."
                    ),
                    new SeedFiche(
                            "Ressources Humaines",
                            "Gestionnaire de Paie",
                            "Assurer le traitement de la paie et la gestion administrative du personnel.",
                            "Calcul et édition des bulletins de paie, déclarations sociales, gestion des congés et absences.",
                            "Mise à jour des dossiers salariés, veille sur la législation sociale, support aux collaborateurs.",
                            NiveauEtudes.BAC_PLUS_2,
                            "Gestion de la Paie / Comptabilité",
                            3,
                            "Logiciels de paie (Sage Paie, SIRH), maîtrise du droit social marocain, Excel.",
                            "Discrétion, rigueur, réactivité, sens du service."
                    ),

                    // --- Développement ---
                    new SeedFiche(
                            "Développement",
                            "Développeur Full Stack",
                            "Concevoir et développer des applications web robustes et scalables.",
                            "Développement frontend (Angular) et backend (Spring Boot), intégration d'APIs REST, tests unitaires.",
                            "Participation aux revues de code, rédaction de documentation technique, veille technologique.",
                            NiveauEtudes.BAC_PLUS_3,
                            "Informatique / Génie Logiciel",
                            3,
                            "Java, Spring Boot, Angular, TypeScript, SQL/PostgreSQL, Git, Docker.",
                            "Autonomie, esprit d'équipe, rigueur, capacité d'adaptation."
                    ),
                    new SeedFiche(
                            "Développement",
                            "Chef de Projet Digital",
                            "Piloter les projets de transformation digitale et assurer leur livraison dans les délais et le budget.",
                            "Cadrage des projets, planification, coordination des équipes techniques et métiers, suivi des jalons.",
                            "Gestion des risques, reporting à la direction, conduite du changement.",
                            NiveauEtudes.BAC_PLUS_5,
                            "Informatique / Management de Projet",
                            5,
                            "Méthodes agiles (Scrum/Kanban), outils de gestion de projet (Jira, MS Project), AMOA.",
                            "Leadership, communication, prise de décision, gestion des conflits."
                    ),
                    new SeedFiche(
                            "Développement",
                            "Ingénieur DevOps",
                            "Automatiser et optimiser les processus de déploiement et d'exploitation des applications.",
                            "Mise en place et maintenance des pipelines CI/CD, gestion de l'infrastructure cloud, monitoring.",
                            "Support aux équipes de développement, gestion des incidents de production, sécurisation des environnements.",
                            NiveauEtudes.BAC_PLUS_5,
                            "Informatique / Systèmes et Réseaux",
                            4,
                            "Docker, Kubernetes, Jenkins, GitLab CI, Terraform, Linux, monitoring (Prometheus, Grafana).",
                            "Proactivité, esprit d'analyse, rigueur, culture de l'amélioration continue."
                    )
            );

            for (SeedFiche f : seedFiches) {
                Direction direction = directionRepository.findAll().stream()
                        .filter(d -> d.getNom().equals(f.directionNom()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Direction introuvable : " + f.directionNom()));

                // Insérer uniquement si aucune fiche avec ce même intitulé n'existe dans cette direction
                boolean exists = ficheDePosteRepository.findAll().stream()
                        .anyMatch(fp -> fp.getIntitulePoste().equals(f.intitule())
                                && fp.getDirection().getId().equals(direction.getId()));

                if (!exists) {
                    FicheDePoste fiche = new FicheDePoste();
                    fiche.setDirection(direction);
                    fiche.setIntitulePoste(f.intitule());
                    fiche.setMissionPrincipale(f.missionPrincipale());
                    fiche.setActivitesPrincipales(f.activitesPrincipales());
                    fiche.setNiveauEtudes(f.niveauEtudes());
                    fiche.setDomaineFormation(f.domaineFormation());
                    fiche.setAnneesExperience(f.anneesExperience());
                    fiche.setCompetencesTechniques(f.competencesTechniques());
                    fiche.setCompetencesManageriales(f.competencesManageriales());
                    ficheDePosteRepository.save(fiche);
                    System.out.println("==> Fiche de poste créée : [" + f.directionNom() + "] " + f.intitule());
                }
            }


        };
    }
}
