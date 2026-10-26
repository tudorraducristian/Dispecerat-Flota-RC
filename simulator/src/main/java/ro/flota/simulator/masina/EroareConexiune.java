package ro.flota.simulator.masina;

/** Conectarea sau publicarea n-a reușit. E verificată de compilator, ca radioul să fie obligat s-o trateze. */
public final class EroareConexiune extends Exception {

    private final boolean ramaneLaClient;

    public EroareConexiune(String mesaj, Throwable cauza) {
        this(mesaj, cauza, false);
    }

    private EroareConexiune(String mesaj, Throwable cauza, boolean ramaneLaClient) {
        super(mesaj, cauza);
        this.ramaneLaClient = ramaneLaClient;
    }

    /**
     * Confirmarea n-a venit la timp, dar clientul MQTT ține mesajul în sesiune și îl retrimite singur după
     * reconectare (QoS 1 și 2). Dacă renunță totuși la el, îl dă înapoi prin Conexiune.laEsecTarziu.
     */
    public static EroareConexiune neconfirmata(Throwable cauza) {
        return new EroareConexiune("confirmarea n-a venit la timp", cauza, true);
    }

    public boolean ramaneLaClient() {
        return ramaneLaClient;
    }
}
