package ro.flota.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** BOM-ul e invizibil în editor. În surse se scrie doar ca secvență escape, ca să se vadă și să nu se piardă. */
class SurseleTest {

    private static final char BOM = (char) 0xFEFF;

    @Test
    void niciunFisierJavaNuAreBomInvizibil() throws IOException {
        try (Stream<Path> fisiere = Files.walk(Path.of("src"))) {
            List<String> cuBom = fisiere
                    .filter(f -> f.toString().endsWith(".java"))
                    .filter(SurseleTest::continueBom)
                    .map(Path::toString)
                    .toList();
            assertEquals(List.of(), cuBom);
        }
    }

    private static boolean continueBom(Path fisier) {
        try {
            return Files.readString(fisier).indexOf(BOM) >= 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
