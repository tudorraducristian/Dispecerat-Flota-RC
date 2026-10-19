package ro.flota.proxy.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.testutil.CeasManual;
import ro.flota.proxy.vreme.Registru;

class OrdineTest {

    private final CeasManual ceas = new CeasManual("2026-12-10T10:00:00Z");
    private final Registru registru = new Registru(ceas);
    private final Ordine ordine = new Ordine(registru);

    @BeforeEach
    void masiniVazute() {
        registru.vreme("masina-3");
        registru.vreme("masina-5");
    }

    /** Răspunsul la o comandă care trebuie să dea un singur rând. */
    private String unu(String linie) {
        List<String> raspuns = ordine.executa(linie);
        assertEquals(1, raspuns.size(), raspuns.toString());
        return raspuns.get(0);
    }

    private String descriere(String masina) {
        return registru.vreme(masina).descriere();
    }

    @Test
    void intarziereaPeOMasina() {
        assertEquals("OK", unu("INTARZIERE masina-3 500 200"));
        assertEquals("INTARZIERE 500 200", descriere("masina-3"));
        assertEquals("NORMAL", descriere("masina-5"));
    }

    @Test
    void variatiaEsteOptionala() {
        assertEquals("OK", unu("INTARZIERE masina-3 500"));
        assertEquals("INTARZIERE 500 0", descriere("masina-3"));
    }

    @Test
    void toateInseamnaToateMasinileVazute() {
        assertEquals("OK", unu("INTARZIERE TOATE 300"));
        assertEquals("INTARZIERE 300 0", descriere("masina-3"));
        assertEquals("INTARZIERE 300 0", descriere("masina-5"));
    }

    @Test
    void literelemariSauMiciNuConteaza() {
        assertEquals("OK", unu("intarziere Masina-3 100"));
        assertEquals("OK", unu("Intarziere toate 100"));
    }

    @Test
    void spatiileInPlusSintIgnorate() {
        assertEquals("OK", unu("  INTARZIERE   masina-3  500  "));
    }

    @Test
    void comandaNecunoscuta() {
        assertEquals(Ordine.ERR_COMANDA, unu("PLOAIE masina-3"));
    }

    @Test
    void masinaNecunoscuta() {
        assertEquals(Ordine.ERR_MASINA, unu("INTARZIERE masina-99 500"));
    }

    @Test
    void parametriGresiti() {
        for (String linie : List.of(
                "INTARZIERE",
                "INTARZIERE masina-3",
                "INTARZIERE masina-3 abc",
                "INTARZIERE masina-3 -1",
                "INTARZIERE masina-3 10001",
                "INTARZIERE masina-3 500 10001",
                "INTARZIERE masina-3 500 200 7")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void limiteleSintAcceptate() {
        assertEquals("OK", unu("INTARZIERE masina-3 0"));
        assertEquals("OK", unu("INTARZIERE masina-3 10000 10000"));
    }

    @Test
    void stareaSeTerminaCuPunct() {
        unu("INTARZIERE masina-5 500 200");
        assertEquals(List.of("masina-3 NORMAL", "masina-5 INTARZIERE 500 200", "."), ordine.executa("STARE"));
    }

    @Test
    void stareaNuPrimesteParametri() {
        assertEquals(Ordine.ERR_PARAMETRI, unu("STARE masina-3"));
    }

    @Test
    void stareaFaraMasiniEsteDoarPunctul() {
        assertEquals(List.of("."), new Ordine(new Registru(ceas)).executa("STARE"));
    }

    @Test
    void toateFaraMasiniEsteOk() {
        assertEquals(List.of("OK"), new Ordine(new Registru(ceas)).executa("INTARZIERE TOATE 100"));
    }

    @Test
    void banda() {
        assertEquals("OK", unu("BANDA masina-3 64"));
        assertEquals("BANDA 64", descriere("masina-3"));
    }

    @Test
    void bandaInAfaraIntervalului() {
        for (String linie : List.of("BANDA masina-3 0", "BANDA masina-3 100001", "BANDA masina-3", "BANDA masina-3 64 1")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void tunelulTineSiInchideLegaturile() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable legatura = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", legatura);
        assertEquals("OK", unu("TUNEL masina-3 15"));
        assertEquals(1, inchise.get());
        assertEquals("TUNEL 15", descriere("masina-3"));
        ceas.avanseaza(15_000);
        assertEquals("NORMAL", descriere("masina-3"));
    }

    @Test
    void tunelPeToateInchideToateMasinile() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable a = inchise::incrementAndGet;
        Closeable b = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.adaugaLegatura("masina-5", b);
        assertEquals("OK", unu("TUNEL TOATE 10"));
        assertEquals(2, inchise.get());
    }

    @Test
    void gaura() {
        assertEquals("OK", unu("GAURA masina-5 10"));
        assertEquals("GAURA 10", descriere("masina-5"));
    }

    @Test
    void durataInAfaraIntervalului() {
        for (String linie : List.of("TUNEL masina-3 0", "TUNEL masina-3 601", "TUNEL masina-3 -5", "TUNEL masina-3",
                "GAURA masina-3 0", "GAURA masina-3 601", "GAURA masina-3 1.5")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void normalStergeTot() {
        unu("INTARZIERE masina-3 500 200");
        unu("BANDA masina-3 64");
        unu("GAURA masina-3 30");
        assertEquals("OK", unu("NORMAL masina-3"));
        assertEquals("NORMAL", descriere("masina-3"));
    }

    @Test
    void normalNuPrimesteDurata() {
        assertEquals(Ordine.ERR_PARAMETRI, unu("NORMAL masina-3 5"));
    }

    @Test
    void comenzileNoiCerMasiniCunoscute() {
        for (String linie : List.of("BANDA masina-99 64", "TUNEL masina-99 10", "GAURA masina-99 10", "NORMAL masina-99")) {
            assertEquals(Ordine.ERR_MASINA, unu(linie), linie);
        }
    }
}
