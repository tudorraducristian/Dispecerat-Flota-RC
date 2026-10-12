package ro.flota.simulator.masina;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;

/** O conexiune de test: ține minte ce s-a publicat și poate eșua la comandă. O pot folosi mai multe fire. */
final class ConexiuneFalsa implements Conexiune {

    final List<Mesaj> publicate = new CopyOnWriteArrayList<>();
    final List<String> evenimente = new CopyOnWriteArrayList<>();
    int conectari;

    private boolean conectata;
    private int esecuriLaConectare;
    private int esecuriLaPublicare;

    synchronized void esueazaLaConectare(int ori) {
        esecuriLaConectare = ori;
    }

    synchronized void esueazaLaPublicare(int ori) {
        esecuriLaPublicare = ori;
    }

    synchronized void pierdeConexiunea() {
        conectata = false;
        evenimente.add("pierdută");
    }

    synchronized void conecteazaDirect() {
        conectata = true;
    }

    @Override
    public synchronized void conecteaza() throws EroareConexiune {
        conectari++;
        if (esecuriLaConectare > 0) {
            esecuriLaConectare--;
            evenimente.add("conectare refuzată");
            throw new EroareConexiune("refuzat", null);
        }
        conectata = true;
        evenimente.add("conectată");
    }

    @Override
    public synchronized void publica(Mesaj mesaj) throws EroareConexiune {
        if (!conectata) {
            throw new EroareConexiune("nu e conectată", null);
        }
        if (esecuriLaPublicare > 0) {
            esecuriLaPublicare--;
            throw new EroareConexiune("publicare eșuată", null);
        }
        publicate.add(mesaj);
        evenimente.add("publică " + mesaj.topic() + (mesaj.tip() == TipMesaj.STARE ? " " + mesaj.text() : ""));
    }

    @Override
    public synchronized boolean conectata() {
        return conectata;
    }

    @Override
    public synchronized void deconecteaza() {
        conectata = false;
        evenimente.add("deconectată");
    }
}
