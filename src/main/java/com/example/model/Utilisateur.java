package com.example.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new HashSet<>();

    public Utilisateur() {
    }

    public Utilisateur(String nom, String email) {
        this.nom = Objects.requireNonNull(nom, "Le nom est obligatoire");
        this.email = Objects.requireNonNull(email, "L'email est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "L'email est obligatoire");
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom est obligatoire");
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
        Objects.requireNonNull(reservation, "La réservation est obligatoire").setUtilisateur(this);
    }

    public void removeReservation(Reservation reservation) {
        if (reservation != null && reservation.getUtilisateur() == this) {
            reservation.setUtilisateur(null);
        }
    }

    void linkReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    void unlinkReservation(Reservation reservation) {
        reservations.remove(reservation);
    }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", nom='" + nom + "', email='" + email
                + "', reservations=" + reservations.size() + "}";
    }
}
