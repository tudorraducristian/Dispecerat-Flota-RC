package ro.flota.simulator.masina;

/** Conectarea sau publicarea n-a reușit. E verificată de compilator, ca radioul să fie obligat s-o trateze. */
public final class EroareConexiune extends Exception {

    public EroareConexiune(String mesaj, Throwable cauza) {
        super(mesaj, cauza);
    }
}
