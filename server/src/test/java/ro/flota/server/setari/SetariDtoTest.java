package ro.flota.server.setari;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;

class SetariDtoTest {

    @Test
    void implicitele() {
        assertEquals(new SetariDto(60, 5), SetariDto.IMPLICITE);
    }

    @Test
    void limiteleSintIncluse() {
        for (SetariDto s : List.of(new SetariDto(10, 1), new SetariDto(3600, 360))) {
            assertDoesNotThrow(s::valideaza);
        }
    }

    @Test
    void offlineInAfaraLimitelorDa400() {
        for (int v : new int[] {9, 3601}) {
            assertEquals("offlinePrelungitSecunde e între 10 și 3600",
                    assertThrows(CerereGresita.class, () -> new SetariDto(v, 5).valideaza()).getMessage());
        }
    }

    @Test
    void traseuInAfaraLimitelorDa400() {
        for (int v : new int[] {0, 361}) {
            assertEquals("traseuMinute e între 1 și 360",
                    assertThrows(CerereGresita.class, () -> new SetariDto(60, v).valideaza()).getMessage());
        }
    }
}
