package ro.flota.server.flota;

/** Ce spune un număr de ordine nou despre mesajele dintre el și cel de dinainte (spec §6.3, pasul 2). */
public record Secventa(long pierdute, boolean repornire) {

    public static Secventa dupa(Long ultimulSeq, long seq) {
        if (ultimulSeq == null) {
            return new Secventa(0, false); // primul mesaj văzut (D25)
        }
        if (seq < ultimulSeq) {
            return new Secventa(0, true); // simulatorul a repornit și numără iar de la 1
        }
        if (seq > ultimulSeq + 1) {
            return new Secventa(seq - ultimulSeq - 1, false);
        }
        return new Secventa(0, false); // următorul, sau același încă o dată
    }
}
