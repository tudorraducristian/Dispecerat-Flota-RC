package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Alarma;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipAlarma;

class BufferTest {

    private static Mesaj telemetrie(long seq) {
        return Mesaje.telemetrie("masina-1",
                new Telemetrie(seq, "2026-12-10T10:15:03.120Z", 46, 23, 50, 90, 100, true));
    }

    private static Mesaj alarma() {
        return Mesaje.alarma("masina-1", new Alarma(TipAlarma.SUPRAINCALZIRE, "2026-12-10T10:15:03.120Z", 111));
    }

    private static long seq(Mesaj m) {
        return Json.MAPPER.readTree(m.payload()).get("seq").asLong();
    }

    @Test
    void scoateInOrdineaInCareAuIntrat() throws InterruptedException {
        Buffer b = new Buffer(10);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        b.adauga(t1);
        b.adauga(t2);
        assertSame(t1, b.ia(100));
        assertSame(t2, b.ia(100));
        assertEquals(0, b.marime());
    }

    @Test
    void plinAruncaTelemetriaCeaMaiVeche() {
        Buffer b = new Buffer(3);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        Mesaj t4 = telemetrie(4);
        b.adauga(t1);
        b.adauga(t2);
        b.adauga(t3);
        b.adauga(t4);
        assertEquals(List.of(t2, t3, t4), b.continut());
        assertEquals(1, b.aruncate());
    }

    @Test
    void alarmeleNuSeAruncaNiciodata() {
        Buffer b = new Buffer(3);
        Mesaj a = alarma();
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        b.adauga(a);
        b.adauga(t1);
        b.adauga(t2);
        b.adauga(t3);
        assertEquals(List.of(a, t2, t3), b.continut());
        assertEquals(1, b.aruncate());
    }

    @Test
    void plinDoarCuAlarmeCresteTotusi() {
        Buffer b = new Buffer(2);
        b.adauga(alarma());
        b.adauga(alarma());
        b.adauga(alarma());
        assertEquals(3, b.marime());
        assertEquals(0, b.aruncate());
    }

    @Test
    void puneInapoiEstePrimulSiNuAruncaNimic() throws InterruptedException {
        Buffer b = new Buffer(2);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        b.adauga(t1);
        b.adauga(t2);
        assertSame(t1, b.ia(100));
        b.adauga(t3);
        b.puneInapoi(t1);
        assertEquals(List.of(t1, t2, t3), b.continut());
        assertEquals(0, b.aruncate());
    }

    @Test
    void iaAsteaptaCelMultCatISeSpune() throws InterruptedException {
        Buffer b = new Buffer(10);
        long inceput = System.nanoTime();
        assertNull(b.ia(150));
        long ms = (System.nanoTime() - inceput) / 1_000_000;
        assertTrue(ms >= 140 && ms < 1_000, "a așteptat " + ms + " ms");
    }

    @Test
    void iaSeTreziesteCandVineUnMesaj() throws InterruptedException {
        Buffer b = new Buffer(10);
        Mesaj t1 = telemetrie(1);
        Thread producator = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
            b.adauga(t1);
        });
        producator.start();
        long inceput = System.nanoTime();
        assertSame(t1, b.ia(5_000));
        assertTrue((System.nanoTime() - inceput) / 1_000_000 < 2_000, "consumatorul n-a fost trezit");
        producator.join();
    }

    @Test
    void unProducatorSiUnConsumatorNuPierdNimic() throws InterruptedException {
        Buffer b = new Buffer(100_000);
        int n = 10_000;
        Thread producator = new Thread(() -> {
            for (int i = 1; i <= n; i++) {
                b.adauga(telemetrie(i));
            }
        });
        producator.start();
        List<Long> primite = new ArrayList<>();
        while (primite.size() < n) {
            Mesaj m = b.ia(2_000);
            if (m == null) {
                break;
            }
            primite.add(seq(m));
        }
        producator.join();
        assertEquals(n, primite.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i + 1, primite.get(i));
        }
    }
}
