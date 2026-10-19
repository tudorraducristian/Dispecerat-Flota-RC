package ro.flota.proxy.transport;

/** Octeții citiți dintr-o dată de pe o conexiune, cu ora la care au voie să plece. */
record Bucata(byte[] octeti, long plecareMs) {
}
