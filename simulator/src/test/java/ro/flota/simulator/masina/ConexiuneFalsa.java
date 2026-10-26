package ro.flota.simulator.masina;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;

/** O conexiune de test: ține minte ce s-a publicat și poate eșua la comandă. O pot folosi mai multe fire. */
final class ConexiuneFalsa implements Conexiune {

    final List<Mesaj> publicate = new CopyOnWriteArrayList<>();
    final List<Mesaj> pastrateDeClient = new CopyOnWriteArrayList<>();
    final List<String> evenimente = new CopyOnWriteArrayList<>();
    int conectari;

    private boolean conectata;
    private int esecuriLaConectare;
    private int esecuriLaPublicare;
    private int neconfirmariLaPublicare;

    synchronized void esueazaLaConectare(int ori) {
        esecuriLaConectare = ori;
    }

    synchronized void esueazaLaPublicare(int ori) {
        esecuriLaPublicare = ori;
    }

    /** Ca HiveMQ când pică legătura cu mesajul pe drum: confirmarea nu vine, dar clientul îl ține în sesiune. */
    synchronized void neconfirmaLaPublicare(int ori) {
        neconfirmariLaPublicare = ori;
    }

    /** Ca HiveMQ când broker-ul a pierdut sesiunea: renunță la mesajele ținute și le dă înapoi. */
    void renuntaLaMesajelePastrate() {
        for (Mesaj mesaj : pastrateDeClient) {
            laEsecTarziu.accept(mesaj);
        }
        pastrateDeClient.clear();
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
        if (neconfirmariLaPublicare > 0) {
            neconfirmariLaPublicare--;
            pastrateDeClient.add(mesaj);
            throw EroareConexiune.neconfirmata(null);
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

    private volatile Consumer<byte[]> laComanda = payload -> { };

    @Override
    public void laComanda(Consumer<byte[]> ascultator) {
        laComanda = ascultator;
    }

    private volatile Consumer<Mesaj> laEsecTarziu = mesaj -> { };

    @Override
    public void laEsecTarziu(Consumer<Mesaj> ascultator) {
        laEsecTarziu = ascultator;
    }

    /** În teste: ca și cum broker-ul ar livra o comandă. */
    void livreazaComanda(String json) {
        laComanda.accept(json.getBytes(StandardCharsets.UTF_8));
    }
}
