package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Equipement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new HashSet<>();

    public Equipement() {
    }

    public Equipement(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom de l'équipement est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom de l'équipement est obligatoire");
    }

    public Set<Salle> getSalles() {
        return Collections.unmodifiableSet(salles);
    }

    public void setSalles(Set<Salle> nouvellesSalles) {
        Objects.requireNonNull(nouvellesSalles, "Les salles sont obligatoires");
        Set<Salle> copie = new HashSet<>(nouvellesSalles);
        for (Salle salle : new HashSet<>(salles)) {
            salle.removeEquipement(this);
        }
        for (Salle salle : copie) {
            salle.addEquipement(this);
        }
    }

    void linkSalle(Salle salle) {
        salles.add(salle);
    }

    void unlinkSalle(Salle salle) {
        salles.remove(salle);
    }

    @Override
    public String toString() {
        return "Equipement{id=" + id + ", nom='" + nom + "', salles=" + salles.size() + "}";
    }
}
