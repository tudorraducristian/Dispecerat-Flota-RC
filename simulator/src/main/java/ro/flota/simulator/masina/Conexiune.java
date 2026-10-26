package ro.flota.simulator.masina;

import java.util.function.Consumer;
import ro.flota.simulator.mesaje.Mesaj;

/** Legătura mașinii cu broker-ul. Implementarea reală e ConexiuneHiveMq, iar testele folosesc una falsă. */
public interface Conexiune {

    void conecteaza() throws EroareConexiune;

    void publica(Mesaj mesaj) throws EroareConexiune;

    boolean conectata();

    void deconecteaza();

    /** Cine primește comenzile venite pe flota/<id>/comenzi. Se setează o dată, înainte de prima conectare. */
    void laComanda(Consumer<byte[]> ascultator);

    /**
     * Cine primește înapoi un mesaj rămas la client (EroareConexiune.ramaneLaClient) la care clientul a renunțat
     * mai târziu, de exemplu pentru că broker-ul repornit a pierdut sesiunea. Se setează o dată, la început.
     */
    void laEsecTarziu(Consumer<Mesaj> ascultator);
}
