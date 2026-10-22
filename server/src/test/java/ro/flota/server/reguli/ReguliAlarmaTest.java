package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;

class ReguliAlarmaTest {

    private static final List<Punct> PATRAT = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.01, 23.01),
            new Punct(46.01, 23.00), new Punct(46.00, 23.00));
    private static final Punct INAUNTRU = new Punct(46.005, 23.005);
    private static final Punct AFARA = new Punct(46.02, 23.005);

    private final ReguliAlarma reguli;

    ReguliAlarmaTest() {
        RegistruFlota registru = new RegistruFlota();
        registru.incarca(List.of(new ZonaDefinita("z1", "Centru", PATRAT, 50)),
                List.of(new MasinaDefinita("masina-3", "z1")));
        reguli = new ReguliAlarma(registru);
    }

    private List<String> tipuri(Punct p, double viteza) {
        return reguli.verifica("masina-3", p, viteza).stream().map(ReguliAlarma.AlarmaNoua::tip).toList();
    }

    @Test
    void iesireaDinZonaDaOAlarma() {
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void catStaAfaraNuMaiDaAlarme() {
        tipuri(INAUNTRU, 40);
        tipuri(AFARA, 40);
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(AFARA, 40));
    }

    @Test
    void seRearmeazaCandRevineInZona() {
        tipuri(INAUNTRU, 40);
        tipuri(AFARA, 40);
        tipuri(INAUNTRU, 40);
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void oMasinaPornitaAfaraNuDaAlarmaPanaNuIntraSiIese() {
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void detaliileSpunZona() {
        tipuri(INAUNTRU, 40);
        assertTrue(reguli.verifica("masina-3", AFARA, 40).get(0).detalii().contains("Centru"));
    }

    @Test
    void oMasinaFaraZonaNuDaAlarme() {
        assertEquals(List.of(), reguli.verifica("masina-99", AFARA, 200));
    }

    @Test
    void vitezaPesteLimitaDaOAlarma() {
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("VITEZA_PESTE_LIMITA"), tipuri(INAUNTRU, 52));
    }

    @Test
    void catRamanePesteLimitaNuMaiDaAlarme() {
        tipuri(INAUNTRU, 52);
        assertEquals(List.of(), tipuri(INAUNTRU, 60));
        assertEquals(List.of(), tipuri(INAUNTRU, 55));
    }

    @Test
    void vitezaSeRearmeazaSubLimita() {
        tipuri(INAUNTRU, 52);
        tipuri(INAUNTRU, 49);
        assertEquals(List.of("VITEZA_PESTE_LIMITA"), tipuri(INAUNTRU, 52));
    }

    @Test
    void exactLaLimitaNuEPeste() {
        assertEquals(List.of(), tipuri(INAUNTRU, 50));
    }

    @Test
    void detaliileSpunVitezaSiLimita() {
        String detalii = reguli.verifica("masina-3", INAUNTRU, 52).get(0).detalii();
        assertTrue(detalii.contains("52.0") && detalii.contains("50"), detalii);
    }
}
