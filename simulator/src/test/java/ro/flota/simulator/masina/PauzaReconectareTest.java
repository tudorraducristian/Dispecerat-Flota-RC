package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.simulare.Aleator;
import ro.flota.simulator.testutil.AleatorScriptat;

class PauzaReconectareTest {

    private static final List<Long> SIR = List.of(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L);

    @Test
    void faraAbatereSirulEste1_2_4_8_16_30_30() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5)); // 0.5 → factorul 1
        assertEquals(SIR, IntStream.range(0, 7).mapToObj(i -> p.dupaEsec()).toList());
    }

    @Test
    void abatereaEsteCelMult20LaSuta() {
        assertEquals(800, new PauzaReconectare(new AleatorScriptat(0.0)).dupaEsec());
        assertEquals(1_200, new PauzaReconectare(new AleatorScriptat(0.99999)).dupaEsec());
    }

    @Test
    void cuIntamplareRealaFiecarePauzaStaInInterval() {
        for (int saminta = 0; saminta < 200; saminta++) {
            PauzaReconectare p = new PauzaReconectare(Aleator.din(new Random(saminta)));
            for (long baza : SIR) {
                long pauza = p.dupaEsec();
                assertTrue(pauza >= baza * 0.8 && pauza <= baza * 1.2, "pauza " + pauza + " pentru " + baza);
            }
        }
    }

    @Test
    void dupaConectareSeIaDeLaCapat() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5));
        p.dupaEsec();
        p.dupaEsec();
        p.dupaEsec();
        p.reseteaza();
        assertEquals(0, p.incercari());
        assertEquals(1_000, p.dupaEsec());
    }

    @Test
    void dupaMulteIncercariRamane30() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5));
        long ultima = 0;
        for (int i = 0; i < 100; i++) {
            ultima = p.dupaEsec();
        }
        assertEquals(30_000, ultima);
    }

    @Test
    void zeceMasiniNuBatDeodata() {
        Set<Long> primele = new HashSet<>();
        // Semințele mici și consecutive dau aproape același prim număr în java.util.Random,
        // deci se iau dintr-un alt Random, risipite ca la new Random() din Main.
        Random seminte = new Random(42);
        for (int masina = 1; masina <= 10; masina++) {
            primele.add(new PauzaReconectare(Aleator.din(new Random(seminte.nextLong()))).dupaEsec());
        }
        assertTrue(primele.size() > 5, "pauzele au ieșit prea asemănătoare: " + primele);
    }
}
