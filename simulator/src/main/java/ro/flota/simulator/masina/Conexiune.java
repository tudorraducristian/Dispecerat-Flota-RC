package ro.flota.simulator.masina;

import ro.flota.simulator.mesaje.Mesaj;

/** Legătura mașinii cu broker-ul. Implementarea reală e ConexiuneHiveMq, iar testele folosesc una falsă. */
public interface Conexiune {

    void conecteaza() throws EroareConexiune;

    void publica(Mesaj mesaj) throws EroareConexiune;

    boolean conectata();

    void deconecteaza();
}
