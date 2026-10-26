package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;

class ReguliContTest {

    @Test
    void numeBune() {
        for (String n : List.of("ana", "dispecer.1", "tudor_r-2", "a".repeat(20))) {
            assertDoesNotThrow(() -> ReguliCont.verificaNume(n));
        }
    }

    @Test
    void numeGresite() {
        for (String n : List.of("", "ab", "a".repeat(21), "Ana", "ion popescu", "ștefan")) {
            assertThrows(CerereGresita.class, () -> ReguliCont.verificaNume(n));
        }
    }

    @Test
    void mesajulPentruNume() {
        CerereGresita e = assertThrows(CerereGresita.class, () -> ReguliCont.verificaNume("ab"));
        assertEquals("numele are 3–20 caractere: litere mici, cifre, punct, _ sau -", e.getMessage());
    }

    @Test
    void normalizeazaTaieSpatiileSiMicsoreazaLiterele() {
        assertEquals("admin", ReguliCont.normalizeaza("  Admin "));
    }

    @Test
    void parolaSubOptCaractereDa400() {
        CerereGresita e = assertThrows(CerereGresita.class, () -> ReguliCont.verificaParola("1234567"));
        assertEquals("parola are cel puțin 8 caractere", e.getMessage());
    }

    /** 37 × „ș” = 74 de octeți în UTF-8. */
    @Test
    void parolaPeste72DeOctetiDa400() {
        CerereGresita e = assertThrows(CerereGresita.class, () -> ReguliCont.verificaParola("ș".repeat(37)));
        assertEquals("parola are cel mult 72 de octeți", e.getMessage());
    }

    @Test
    void parolaDeExact72DeOctetiTrece() {
        assertDoesNotThrow(() -> ReguliCont.verificaParola("a".repeat(72)));
    }
}
