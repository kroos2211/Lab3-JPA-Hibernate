package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    public Reservation() {
    }

    public Reservation(Utilisateur utilisateur, Salle salle, LocalDateTime dateDebut, LocalDateTime dateFin) {
        setPeriode(dateDebut, dateFin);
        setUtilisateur(Objects.requireNonNull(utilisateur, "L'utilisateur est obligatoire"));
        setSalle(Objects.requireNonNull(salle, "La salle est obligatoire"));
    }

    public static void validerPeriode(LocalDateTime debut, LocalDateTime fin) {
        Objects.requireNonNull(debut, "La date de début est obligatoire");
        Objects.requireNonNull(fin, "La date de fin est obligatoire");
        if (!fin.isAfter(debut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
    }

    @PrePersist
    @PreUpdate
    private void validerAvantEnregistrement() {
        validerPeriode(dateDebut, dateFin);
        Objects.requireNonNull(utilisateur, "L'utilisateur est obligatoire");
        Objects.requireNonNull(salle, "La salle est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur nouvelUtilisateur) {
        if (utilisateur == nouvelUtilisateur) {
            return;
        }
        Utilisateur ancien = utilisateur;
        utilisateur = nouvelUtilisateur;
        if (ancien != null) {
            ancien.unlinkReservation(this);
        }
        if (nouvelUtilisateur != null) {
            nouvelUtilisateur.linkReservation(this);
        }
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle nouvelleSalle) {
        if (salle == nouvelleSalle) {
            return;
        }
        Salle ancienne = salle;
        salle = nouvelleSalle;
        if (ancienne != null) {
            ancienne.unlinkReservation(this);
        }
        if (nouvelleSalle != null) {
            nouvelleSalle.linkReservation(this);
        }
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        Objects.requireNonNull(dateDebut, "La date de début est obligatoire");
        if (dateFin != null) {
            validerPeriode(dateDebut, dateFin);
        }
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        Objects.requireNonNull(dateFin, "La date de fin est obligatoire");
        if (dateDebut != null) {
            validerPeriode(dateDebut, dateFin);
        }
        this.dateFin = dateFin;
    }

    public void setPeriode(LocalDateTime debut, LocalDateTime fin) {
        validerPeriode(debut, fin);
        dateDebut = debut;
        dateFin = fin;
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", utilisateur='"
                + (utilisateur == null ? "aucun" : utilisateur.getNom())
                + "', salle='" + (salle == null ? "aucune" : salle.getNom())
                + "', debut=" + dateDebut + ", fin=" + dateFin + "}";
    }
}
