package ro.flota.server.comenzi;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Un rând din tabelul comenzi. stare e TRIMISA, CONFIRMATA sau RESPINSA. */
public record ComandaDto(long id, String masinaId, String tip, JsonNode parametri, Instant oraTrimiterii,
                         Instant oraConfirmarii, String stare, String motiv) {
}
