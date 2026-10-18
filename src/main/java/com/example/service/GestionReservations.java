package com.example.service;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class GestionReservations implements AutoCloseable {
    private final EntityManagerFactory factory;
    private final EntityManager em;

    public GestionReservations() {
        this(Collections.emptyMap());
    }

    public GestionReservations(Map<String, ?> proprietes) {
        factory = Persistence.createEntityManagerFactory("gestion-reservations-pu", proprietes);
        try {
            em = factory.createEntityManager();
        } catch (RuntimeException ex) {
            factory.close();
            throw ex;
        }
    }

    private <T> T enTransaction(Function<EntityManager, T> operation) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            T resultat = operation.apply(em);
            transaction.commit();
            return resultat;
        } catch (RuntimeException ex) {
            try {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
            } catch (RuntimeException rollbackFailure) {
                ex.addSuppressed(rollbackFailure);
            } finally {
                try {
                    em.clear();
                } catch (RuntimeException clearFailure) {
                    ex.addSuppressed(clearFailure);
                }
            }
            throw new PersistenceException("Échec de la transaction JPA", ex);
        }
    }

    public void ajouterUtilisateur(Utilisateur utilisateur) {
        Objects.requireNonNull(utilisateur, "L'utilisateur est obligatoire");
        enTransaction(manager -> {
            manager.persist(utilisateur); // CascadeType.ALL persiste aussi ses réservations.
            return null;
        });
    }

    public void ajouterSalle(Salle salle) {
        Objects.requireNonNull(salle, "La salle est obligatoire");
        enTransaction(manager -> {
            manager.persist(salle); // CascadeType.PERSIST persiste les nouveaux équipements.
            return null;
        });
    }

    public void ajouterEquipement(Equipement equipement) {
        Objects.requireNonNull(equipement, "L'équipement est obligatoire");
        enTransaction(manager -> {
            manager.persist(equipement);
            return null;
        });
    }

    public void ajouterReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation est obligatoire");
        enTransaction(manager -> {
            manager.persist(reservation);
            return null;
        });
    }

    public Utilisateur trouverUtilisateur(Long id) {
        return em.find(Utilisateur.class, id);
    }

    public Salle trouverSalle(Long id) {
        return em.find(Salle.class, id);
    }

    public Equipement trouverEquipement(Long id) {
        return em.find(Equipement.class, id);
    }

    public Reservation trouverReservation(Long id) {
        return em.find(Reservation.class, id);
    }

    public List<Utilisateur> listerUtilisateurs() {
        return em.createQuery("SELECT u FROM Utilisateur u ORDER BY u.id", Utilisateur.class)
                .getResultList();
    }

    public List<Salle> listerSalles() {
        return em.createQuery("SELECT s FROM Salle s ORDER BY s.id", Salle.class)
                .getResultList();
    }

    public List<Reservation> listerReservations() {
        return em.createQuery("SELECT r FROM Reservation r ORDER BY r.id", Reservation.class)
                .getResultList();
    }

    public List<Reservation> reservationsParUtilisateur(Long utilisateurId) {
        return em.createQuery(
                        "SELECT r FROM Reservation r WHERE r.utilisateur.id = :utilisateurId ORDER BY r.dateDebut",
                        Reservation.class)
                .setParameter("utilisateurId", utilisateurId)
                .getResultList();
    }

    public List<Salle> sallesAvecEquipement(String nomEquipement) {
        return em.createQuery(
                        "SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom",
                        Salle.class)
                .setParameter("nom", nomEquipement)
                .getResultList();
    }

    public List<Equipement> equipementsParSalle(Long salleId) {
        return em.createQuery(
                        "SELECT e FROM Salle s JOIN s.equipements e WHERE s.id = :salleId ORDER BY e.nom",
                        Equipement.class)
                .setParameter("salleId", salleId)
                .getResultList();
    }

    public List<Reservation> reservationsParSalle(Long salleId) {
        return em.createQuery(
                        "SELECT r FROM Reservation r WHERE r.salle.id = :salleId ORDER BY r.dateDebut",
                        Reservation.class)
                .setParameter("salleId", salleId)
                .getResultList();
    }

    public boolean salleDisponible(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.validerPeriode(debut, fin);
        if (trouverSalle(salleId) == null) {
            throw new EntityNotFoundException("Salle introuvable : " + salleId);
        }
        Long chevauchements = em.createQuery(
                        "SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :salleId "
                                + "AND r.dateDebut < :fin AND r.dateFin > :debut",
                        Long.class)
                .setParameter("salleId", salleId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return chevauchements == 0;
    }

    public Reservation modifierPeriodeReservation(Long reservationId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.validerPeriode(debut, fin);
        return enTransaction(manager -> {
            Reservation reservation = manager.find(Reservation.class, reservationId);
            if (reservation == null) {
                throw new EntityNotFoundException("Réservation introuvable : " + reservationId);
            }
            reservation.setPeriode(debut, fin);
            return reservation;
        });
    }

    public void retirerReservation(Long utilisateurId, Long reservationId) {
        enTransaction(manager -> {
            Utilisateur utilisateur = manager.find(Utilisateur.class, utilisateurId);
            Reservation reservation = manager.find(Reservation.class, reservationId);
            if (utilisateur == null || reservation == null
                    || !utilisateurId.equals(reservation.getUtilisateur().getId())) {
                throw new EntityNotFoundException("Réservation absente de cet utilisateur : " + reservationId);
            }
            utilisateur.removeReservation(reservation); // orphanRemoval supprime la ligne.
            return null;
        });
    }

    public void supprimerUtilisateur(Long utilisateurId) {
        enTransaction(manager -> {
            Utilisateur utilisateur = manager.find(Utilisateur.class, utilisateurId);
            if (utilisateur == null) {
                throw new EntityNotFoundException("Utilisateur introuvable : " + utilisateurId);
            }
            manager.remove(utilisateur); // CascadeType.REMOVE supprime ses réservations.
            return null;
        });
    }

    public void retirerEquipementSalle(Long salleId, Long equipementId) {
        enTransaction(manager -> {
            Salle salle = manager.find(Salle.class, salleId);
            Equipement equipement = manager.find(Equipement.class, equipementId);
            if (salle == null || equipement == null) {
                throw new EntityNotFoundException("Salle ou équipement introuvable");
            }
            salle.removeEquipement(equipement);
            return null;
        });
    }

    public void supprimerSalle(Long salleId) {
        enTransaction(manager -> {
            Salle salle = manager.find(Salle.class, salleId);
            if (salle == null) {
                throw new EntityNotFoundException("Salle introuvable : " + salleId);
            }
            manager.remove(salle); // Les équipements partagés ne sont pas supprimés.
            return null;
        });
    }

    @Override
    public void close() {
        try {
            if (em.isOpen()) {
                em.close();
            }
        } finally {
            if (factory.isOpen()) {
                factory.close();
            }
        }
    }
}
