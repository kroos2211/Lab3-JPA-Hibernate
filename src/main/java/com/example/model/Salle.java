package com.example.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Salle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "salle_equipement",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    private Set<Equipement> equipements = new HashSet<>();

    public Salle() {
    }

    public Salle(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom de la salle est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom de la salle est obligatoire");
    }

    public Set<Reservation> getReservations() {
        return Collections.unmodifiableSet(reservations);
    }

    public void setReservations(Set<Reservation> nouvellesReservations) {
        Objects.requireNonNull(nouvellesReservations, "Les réservations sont obligatoires");
        Set<Reservation> copie = new HashSet<>(nouvellesReservations);
        for (Reservation reservation : new HashSet<>(reservations)) {
            removeReservation(reservation);
        }
        for (Reservation reservation : copie) {
            addReservation(reservation);
        }
    }

    public void addReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation est obligatoire").setSalle(this);
    }

    public void removeReservation(Reservation reservation) {
        if (reservation != null && reservation.getSalle() == this) {
            reservation.setSalle(null);
        }
    }

    void linkReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    void unlinkReservation(Reservation reservation) {
        reservations.remove(reservation);
    }

    public Set<Equipement> getEquipements() {
        return Collections.unmodifiableSet(equipements);
    }

    public void setEquipements(Set<Equipement> nouveauxEquipements) {
        Objects.requireNonNull(nouveauxEquipements, "Les équipements sont obligatoires");
        Set<Equipement> copie = new HashSet<>(nouveauxEquipements);
        for (Equipement equipement : new HashSet<>(equipements)) {
            removeEquipement(equipement);
        }
        for (Equipement equipement : copie) {
            addEquipement(equipement);
        }
    }

    public void addEquipement(Equipement equipement) {
        Objects.requireNonNull(equipement, "L'équipement est obligatoire");
        if (equipements.add(equipement)) {
            equipement.linkSalle(this);
        }
    }

    public void removeEquipement(Equipement equipement) {
        if (equipement != null && equipements.remove(equipement)) {
            equipement.unlinkSalle(this);
        }
    }

    @Override
    public String toString() {
        return "Salle{id=" + id + ", nom='" + nom + "', equipements=" + equipements.size()
                + ", reservations=" + reservations.size() + "}";
    }
}
