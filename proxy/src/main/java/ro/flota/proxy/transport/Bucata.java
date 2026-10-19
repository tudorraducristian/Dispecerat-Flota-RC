package ro.flota.proxy.transport;

/**
 * Octeții citiți dintr-o dată de pe o conexiune, cu ora la care au voie să plece.
 * O bucată fără octeți e sfârșitul: celălalt capăt a închis curat, iar după ea nu mai vine nimic.
 */
record Bucata(byte[] octeti, long plecareMs) {

    static Bucata sfarsit(long plecareMs) {
        return new Bucata(null, plecareMs);
    }

    boolean esteSfarsit() {
        return octeti == null;
    }
}
