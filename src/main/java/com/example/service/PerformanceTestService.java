package com.example.service;

import com.example.model.Auteur;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class PerformanceTestService {

    @PersistenceContext
    public EntityManager em;

    public PerformanceTestService() {
    }

    public PerformanceTestService(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public void testPerformanceSansCache() {
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        // Accès aux livres pour provoquer le chargement
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée sans cache : " + (end - start) + " ms");
    }

    public void testPerformanceAvecJoinFetch() {
        long start = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        // L'accès aux livres ne déclenche plus de requêtes supplémentaires
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec JOIN FETCH : " + (end - start) + " ms");
    }

    public void testPerformanceAvecEntityGraph() {
        long start = System.currentTimeMillis();

        EntityGraph<?> graph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        query.setHint("javax.persistence.fetchgraph", graph);
        List<Auteur> auteurs = query.getResultList();

        // L'accès aux livres ne déclenche plus de requêtes supplémentaires
        for (Auteur auteur : auteurs) {
            auteur.getLivres().size();
        }

        long end = System.currentTimeMillis();
        System.out.println("Durée avec Entity Graph : " + (end - start) + " ms");
    }

    public void testPerformanceAvecCache() {
        // Première lecture - chargement depuis la base
        testPerformanceAvecJoinFetch();

        // Deuxième lecture - chargement depuis le cache
        long start = System.currentTimeMillis();
        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();
        long end = System.currentTimeMillis();
        System.out.println("Durée avec cache (lecture 2) : " + (end - start) + " ms");
    }
}
