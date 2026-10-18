package com.example;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.service.GestionReservations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class GestionReservationsTest {
    private GestionReservations gestion;
    private final LocalDateTime debut = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Before
    public void preparer() {
        Map<String, String> options = new HashMap<>();
        options.put("javax.persistence.jdbc.url",
                "jdbc:h2:mem:lab3_" + UUID.randomUUID().toString().replace("-", "")
                        + ";DB_CLOSE_DELAY=-1");
        options.put("hibernate.hbm2ddl.auto", "create-drop");
        options.put("hibernate.show_sql", "false");
        options.put("hibernate.format_sql", "false");
        gestion = new GestionReservations(options);
    }

    @After
    public void terminer() {
        if (gestion != null) {
            gestion.close();
        }
    }

    @Test
    public void creationUtilisateur() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        assertNotNull(utilisateur.getId());
        assertEquals("Alice", gestion.trouverUtilisateur(utilisateur.getId()).getNom());
        assertEquals(1, gestion.listerUtilisateurs().size());
    }

    @Test
    public void creationSalle() {
        Salle salle = salle("Atlas");
        assertNotNull(salle.getId());
        assertEquals("Atlas", gestion.trouverSalle(salle.getId()).getNom());
        assertEquals(1, gestion.listerSalles().size());
    }

    @Test
    public void creationEquipement() {
        Equipement equipement = new Equipement("Tableau");
        gestion.ajouterEquipement(equipement);
        assertNotNull(equipement.getId());
        assertEquals("Tableau", gestion.trouverEquipement(equipement.getId()).getNom());
    }

    @Test
    public void creationReservation() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Salle salle = salle("Atlas");
        Reservation reservation = reservation(utilisateur, salle, debut, debut.plusHours(1));

        assertNotNull(reservation.getId());
        assertEquals(debut, gestion.trouverReservation(reservation.getId()).getDateDebut());
        assertEquals(1, gestion.listerReservations().size());
    }

    @Test
    public void relationOneToMany() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Salle salle = salle("Atlas");
        reservation(utilisateur, salle, debut, debut.plusHours(1));
        reservation(utilisateur, salle, debut.plusHours(2), debut.plusHours(3));

        assertEquals(2, utilisateur.getReservations().size());
        assertEquals(2, gestion.reservationsParUtilisateur(utilisateur.getId()).size());
    }

    @Test
    public void relationsManyToOne() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Salle salle = salle("Atlas");
        Reservation reservation = reservation(utilisateur, salle, debut, debut.plusHours(1));

        Reservation retrouvee = gestion.trouverReservation(reservation.getId());
        assertEquals(utilisateur.getId(), retrouvee.getUtilisateur().getId());
        assertEquals(salle.getId(), retrouvee.getSalle().getId());
        assertTrue(salle.getReservations().contains(reservation));
    }

    @Test
    public void relationManyToManyAvecEquipementPartage() {
        Equipement wifi = new Equipement("Wi-Fi");
        Salle atlas = new Salle("Atlas");
        Salle rif = new Salle("Rif");
        atlas.addEquipement(wifi);
        rif.addEquipement(wifi);
        gestion.ajouterSalle(atlas);
        gestion.ajouterSalle(rif);

        assertEquals(2, wifi.getSalles().size());
        assertEquals(2, gestion.sallesAvecEquipement("Wi-Fi").size());
        assertEquals(wifi.getId(), gestion.equipementsParSalle(atlas.getId()).get(0).getId());
    }

    @Test
    public void cascadePersistDesReservations() {
        Salle salle = salle("Atlas");
        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com");
        Reservation reservation = new Reservation(utilisateur, salle, debut, debut.plusHours(1));

        gestion.ajouterUtilisateur(utilisateur);

        assertNotNull(utilisateur.getId());
        assertNotNull(reservation.getId());
        assertEquals(1, gestion.reservationsParUtilisateur(utilisateur.getId()).size());
    }

    @Test
    public void cascadePersistDesEquipements() {
        Salle salle = new Salle("Atlas");
        Equipement projecteur = new Equipement("Projecteur");
        salle.addEquipement(projecteur);

        gestion.ajouterSalle(salle);

        assertNotNull(projecteur.getId());
        assertEquals(1, gestion.equipementsParSalle(salle.getId()).size());
    }

    @Test
    public void cascadeRemoveDesReservations() {
        Salle salle = salle("Atlas");
        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com");
        Reservation premiere = new Reservation(utilisateur, salle, debut, debut.plusHours(1));
        Reservation seconde = new Reservation(utilisateur, salle, debut.plusHours(2), debut.plusHours(3));
        gestion.ajouterUtilisateur(utilisateur);

        gestion.supprimerUtilisateur(utilisateur.getId());

        assertNull(gestion.trouverUtilisateur(utilisateur.getId()));
        assertNull(gestion.trouverReservation(premiere.getId()));
        assertNull(gestion.trouverReservation(seconde.getId()));
        assertNotNull(gestion.trouverSalle(salle.getId()));
    }

    @Test
    public void suppressionOrpheline() {
        Salle salle = salle("Atlas");
        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com");
        Reservation premiere = new Reservation(utilisateur, salle, debut, debut.plusHours(1));
        Reservation seconde = new Reservation(utilisateur, salle, debut.plusHours(2), debut.plusHours(3));
        gestion.ajouterUtilisateur(utilisateur);

        gestion.retirerReservation(utilisateur.getId(), premiere.getId());

        assertNull(gestion.trouverReservation(premiere.getId()));
        assertNotNull(gestion.trouverReservation(seconde.getId()));
        assertEquals(1, gestion.reservationsParUtilisateur(utilisateur.getId()).size());
    }

    @Test
    public void equipementPartageConserveApresSuppressionSalle() {
        Equipement wifi = new Equipement("Wi-Fi");
        Salle atlas = new Salle("Atlas");
        Salle rif = new Salle("Rif");
        atlas.addEquipement(wifi);
        rif.addEquipement(wifi);
        gestion.ajouterSalle(atlas);
        gestion.ajouterSalle(rif);

        gestion.supprimerSalle(atlas.getId());

        assertNotNull(gestion.trouverEquipement(wifi.getId()));
        assertEquals(1, gestion.sallesAvecEquipement("Wi-Fi").size());
        assertEquals("Rif", gestion.sallesAvecEquipement("Wi-Fi").get(0).getNom());
    }

    @Test
    public void requetesJpql() {
        Equipement wifi = new Equipement("Wi-Fi");
        Salle atlas = new Salle("Atlas");
        Salle rif = new Salle("Rif");
        atlas.addEquipement(wifi);
        gestion.ajouterSalle(atlas);
        gestion.ajouterSalle(rif);
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Reservation reservation = reservation(utilisateur, atlas, debut, debut.plusHours(1));

        assertEquals(reservation.getId(), gestion.reservationsParUtilisateur(utilisateur.getId()).get(0).getId());
        assertEquals("Atlas", gestion.sallesAvecEquipement("Wi-Fi").get(0).getNom());
        assertEquals("Wi-Fi", gestion.equipementsParSalle(atlas.getId()).get(0).getNom());
        assertEquals(1, gestion.reservationsParSalle(atlas.getId()).size());
        assertTrue(gestion.reservationsParSalle(rif.getId()).isEmpty());
    }

    @Test
    public void validationDesDates() {
        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com");
        Salle salle = new Salle("Atlas");

        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(utilisateur, salle, debut, debut));
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(utilisateur, salle, debut, debut.minusMinutes(1)));
        assertThrows(NullPointerException.class,
                () -> Reservation.validerPeriode(null, debut));
    }

    @Test
    public void disponibiliteEtPeriodesAdjacentes() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Salle salle = salle("Atlas");
        reservation(utilisateur, salle, debut, debut.plusHours(1));

        assertFalse(gestion.salleDisponible(salle.getId(), debut.plusMinutes(30), debut.plusHours(2)));
        assertFalse(gestion.salleDisponible(salle.getId(), debut.minusMinutes(1), debut.plusMinutes(1)));
        assertTrue(gestion.salleDisponible(salle.getId(), debut.plusHours(1), debut.plusHours(2)));
        assertTrue(gestion.salleDisponible(salle.getId(), debut.minusHours(1), debut));
        assertThrows(IllegalArgumentException.class,
                () -> gestion.salleDisponible(salle.getId(), debut, debut));
    }

    @Test
    public void rollbackEtReutilisationDuService() {
        utilisateur("Alice", "unique@example.com");

        assertThrows(PersistenceException.class,
                () -> utilisateur("Doublon", "unique@example.com"));
        assertEquals(1, gestion.listerUtilisateurs().size());

        utilisateur("Bob", "bob@example.com");
        assertEquals(2, gestion.listerUtilisateurs().size());
    }

    @Test
    public void associationsBidirectionnellesSynchronisees() {
        Utilisateur utilisateur = new Utilisateur("Alice", "alice@example.com");
        Salle salle = new Salle("Atlas");
        Equipement wifi = new Equipement("Wi-Fi");
        salle.addEquipement(wifi);
        Reservation reservation = new Reservation(utilisateur, salle, debut, debut.plusHours(1));

        assertTrue(utilisateur.getReservations().contains(reservation));
        assertTrue(salle.getReservations().contains(reservation));
        assertTrue(wifi.getSalles().contains(salle));
        salle.removeEquipement(wifi);
        utilisateur.removeReservation(reservation);
        salle.removeReservation(reservation);
        assertTrue(wifi.getSalles().isEmpty());
        assertTrue(utilisateur.getReservations().isEmpty());
        assertTrue(salle.getReservations().isEmpty());
    }

    @Test
    public void modificationReservation() {
        Utilisateur utilisateur = utilisateur("Alice", "alice@example.com");
        Salle salle = salle("Atlas");
        Reservation reservation = reservation(utilisateur, salle, debut, debut.plusHours(1));

        gestion.modifierPeriodeReservation(reservation.getId(), debut.plusHours(1), debut.plusHours(2));

        assertEquals(debut.plusHours(1), gestion.trouverReservation(reservation.getId()).getDateDebut());
        assertEquals(debut.plusHours(2), gestion.trouverReservation(reservation.getId()).getDateFin());
    }

    private Utilisateur utilisateur(String nom, String email) {
        Utilisateur utilisateur = new Utilisateur(nom, email);
        gestion.ajouterUtilisateur(utilisateur);
        return utilisateur;
    }

    private Salle salle(String nom) {
        Salle salle = new Salle(nom);
        gestion.ajouterSalle(salle);
        return salle;
    }

    private Reservation reservation(Utilisateur utilisateur, Salle salle,
                                    LocalDateTime dateDebut, LocalDateTime dateFin) {
        Reservation reservation = new Reservation(utilisateur, salle, dateDebut, dateFin);
        gestion.ajouterReservation(reservation);
        return reservation;
    }
}
