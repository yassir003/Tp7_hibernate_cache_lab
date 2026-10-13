package com.example;

import com.example.model.Auteur;
import com.example.service.DataInitService;
import com.example.service.PerformanceTestService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();

        try {
            DataInitService dataInitService = new DataInitService();
            PerformanceTestService performanceTestService = new PerformanceTestService();

            dataInitService.em = em;
            performanceTestService.em = em;

            // 1. Initialisation des données
            System.out.println("==================================================");
            System.out.println("=== 1. INITIALISATION DES DONNEES DE TEST      ===");
            System.out.println("==================================================");
            em.getTransaction().begin();
            dataInitService.initData();
            em.getTransaction().commit();
            em.clear(); // Vider le cache L1 pour tester le chargement réel

            Long auteurCount = em.createQuery("SELECT COUNT(a) FROM Auteur a", Long.class).getSingleResult();
            Long livreCount = em.createQuery("SELECT COUNT(l) FROM Livre l", Long.class).getSingleResult();
            System.out.println("Donnees inserees : " + auteurCount + " auteurs, " + livreCount + " livres.\n");
            em.clear();

            // 2. Test sans cache (Problème N+1)
            System.out.println("==================================================");
            System.out.println("=== 2. TEST SANS CACHE (PROBLEME N+1)          ===");
            System.out.println("==================================================");
            performanceTestService.testPerformanceSansCache();
            em.clear();

            // 3. Test avec JOIN FETCH
            System.out.println("\n==================================================");
            System.out.println("=== 3. TEST AVEC JOIN FETCH                    ===");
            System.out.println("==================================================");
            performanceTestService.testPerformanceAvecJoinFetch();
            em.clear();

            // 4. Test avec Entity Graph
            System.out.println("\n==================================================");
            System.out.println("=== 4. TEST AVEC ENTITY GRAPH                  ===");
            System.out.println("==================================================");
            performanceTestService.testPerformanceAvecEntityGraph();
            em.clear();

            // 5. Test avec Cache de second niveau (lectures successives)
            System.out.println("\n==================================================");
            System.out.println("=== 5. TEST AVEC CACHE DE SECOND NIVEAU        ===");
            System.out.println("==================================================");
            performanceTestService.testPerformanceAvecCache();

            // 6. Validation du Cache L2 cross-session avec em.find()
            System.out.println("\n==================================================");
            System.out.println("=== 6. VALIDATION CACHE L2 (NOUVELLE SESSION)  ===");
            System.out.println("==================================================");
            EntityManager em2 = emf.createEntityManager();
            try {
                System.out.println("Recherche de l'auteur 1 dans une NOUVELLE session :");
                long startFind = System.currentTimeMillis();
                Auteur auteur = em2.find(Auteur.class, 1L);
                long endFind = System.currentTimeMillis();
                System.out.println("Auteur recupere : " + auteur.getNom() + " en " + (endFind - startFind) + " ms (sans requete SQL, direct depuis Ehcache !)");
            } finally {
                em2.close();
            }

            System.out.println("\n==================================================");
            System.out.println("=== TOUS LES TESTS SONT TERMINES AVEC SUCCES   ===");
            System.out.println("==================================================");

        } finally {
            em.close();
            emf.close();
        }
    }
}
