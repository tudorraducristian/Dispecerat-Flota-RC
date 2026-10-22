package ro.flota.server.statistici;

/** Un rând din statistici (endpoint-ul 12 și STATISTICI). latentaMedieMs e null până vine un mesaj live. */
public record StatisticaDto(String masinaId, double mesajePeSecunda, Double latentaMedieMs, long recuperate, long pierdute) {
}
