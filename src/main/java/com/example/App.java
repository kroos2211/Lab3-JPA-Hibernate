package com.example;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.service.GestionReservations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) {
        try (GestionReservations gestion = new GestionReservations()) {
            System.out.println("=== Création des utilisateurs ===");
            Utilisateur alice = new Utilisateur("Alice", "alice@example.com");
            Utilisateur bob = new Utilisateur("Bob", "bob@example.com");
            System.out.println("Alice et Bob préparés pour leurs réservations.");

            System.out.println("=== Création des salles et équipements ===");
            Equipement projecteur = new Equipement("Projecteur");
            Equipement tableau = new Equipement("Tableau");
            Equipement wifi = new Equipement("Wi-Fi");
            Salle atlas = new Salle("Atlas");
            Salle rif = new Salle("Rif");
            Salle agdal = new Salle("Agdal");
            atlas.addEquipement(projecteur);
            atlas.addEquipement(wifi);
            rif.addEquipement(tableau);
            rif.addEquipement(wifi);
            agdal.addEquipement(wifi);
            // Les salles sont enregistrées avant les réservations ; leur relation ne cascade pas depuis Utilisateur.
            gestion.ajouterSalle(atlas);
            gestion.ajouterSalle(rif);
            gestion.ajouterSalle(agdal);

            System.out.println("=== Création des réservations ===");
            LocalDateTime jour = LocalDateTime.of(2030, 5, 20, 9, 0);
            Reservation aliceAtlas = new Reservation(alice, atlas, jour, jour.plusHours(1));
            Reservation aliceRif = new Reservation(alice, rif, jour.plusHours(2), jour.plusHours(3));
            Reservation bobRif = new Reservation(bob, rif, jour.plusHours(4), jour.plusHours(5));
            gestion.ajouterUtilisateur(alice);
            gestion.ajouterUtilisateur(bob);

            System.out.println("=== Entités enregistrées ===");
            gestion.listerUtilisateurs().forEach(System.out::println);
            gestion.listerSalles().forEach(System.out::println);
            System.out.println(projecteur);
            System.out.println(tableau);
            System.out.println(wifi);

            System.out.println("=== Relation OneToMany et ManyToOne ===");
            for (Utilisateur utilisateur : gestion.listerUtilisateurs()) {
                System.out.println(utilisateur.getNom() + " : " + utilisateur.getReservations().size()
                        + " réservation(s)");
                gestion.reservationsParUtilisateur(utilisateur.getId()).forEach(System.out::println);
            }

            System.out.println("=== Relation ManyToMany ===");
            for (Salle salle : gestion.listerSalles()) {
                System.out.println(salle.getNom() + " : "
                        + nomsEquipements(gestion.equipementsParSalle(salle.getId())));
            }
            System.out.println("Wi-Fi partagé par " + wifi.getSalles().size() + " salles.");

            System.out.println("=== Cascade PERSIST : réservations enregistrées avec les utilisateurs ===");
            System.out.println("Réservations d'Alice : " + gestion.reservationsParUtilisateur(alice.getId()).size());
            System.out.println("Réservations de Bob : " + gestion.reservationsParUtilisateur(bob.getId()).size());
            System.out.println("Réservations totales : " + gestion.listerReservations().size());

            System.out.println("=== Recherche JPQL ===");
            System.out.println("Réservations d'Alice : " + gestion.reservationsParUtilisateur(alice.getId()).size());
            System.out.println("Salles avec Wi-Fi : " + nomsSalles(gestion.sallesAvecEquipement("Wi-Fi")));
            System.out.println("Équipements d'Atlas : " + nomsEquipements(gestion.equipementsParSalle(atlas.getId())));
            System.out.println("Réservations de Rif : " + gestion.reservationsParSalle(rif.getId()).size());
            System.out.println("Atlas disponible de 09:30 à 10:30 : "
                    + gestion.salleDisponible(atlas.getId(), jour.plusMinutes(30), jour.plusHours(1).plusMinutes(30)));
            System.out.println("Atlas disponible de 10:00 à 11:00 : "
                    + gestion.salleDisponible(atlas.getId(), jour.plusHours(1), jour.plusHours(2)));

            System.out.println("=== Modification d'une réservation ===");
            System.out.println("Avant : " + gestion.trouverReservation(aliceAtlas.getId()));
            gestion.modifierPeriodeReservation(aliceAtlas.getId(), jour.plusHours(1), jour.plusHours(2));
            System.out.println("Après : " + gestion.trouverReservation(aliceAtlas.getId()));

            System.out.println("=== Test orphanRemoval ===");
            gestion.retirerReservation(alice.getId(), aliceRif.getId());
            System.out.println("Réservation retirée supprimée : "
                    + (gestion.trouverReservation(aliceRif.getId()) == null));
            System.out.println("Réservations restantes d'Alice : "
                    + gestion.reservationsParUtilisateur(alice.getId()).size());

            System.out.println("=== Test CascadeType.ALL : suppression d'un utilisateur ===");
            gestion.supprimerUtilisateur(bob.getId());
            System.out.println("Bob supprimé : " + (gestion.trouverUtilisateur(bob.getId()) == null));
            System.out.println("Réservation de Bob supprimée : "
                    + (gestion.trouverReservation(bobRif.getId()) == null));

            System.out.println("=== Vérification des équipements partagés ===");
            System.out.println("Wi-Fi conservé : " + (gestion.trouverEquipement(wifi.getId()) != null));
            System.out.println("Salles avec Wi-Fi : " + nomsSalles(gestion.sallesAvecEquipement("Wi-Fi")));
            System.out.println("=== Fin de la démonstration ===");
        }
    }

    private static String nomsEquipements(List<Equipement> equipements) {
        return equipements.stream().map(Equipement::getNom).sorted().collect(Collectors.joining(", "));
    }

    private static String nomsSalles(List<Salle> salles) {
        return salles.stream().map(Salle::getNom).sorted().collect(Collectors.joining(", "));
    }
}
