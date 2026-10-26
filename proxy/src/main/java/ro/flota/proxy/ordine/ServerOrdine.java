package ro.flota.proxy.ordine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Portul de ordine, 1885 (spec §5.4): management out-of-band, pe alt drum decât cel pe care îl strică.
 * Fiecare client are firul lui, iar conexiunea rămâne deschisă pentru oricâte comenzi.
 */
public final class ServerOrdine implements Runnable {

    private final ServerSocket server;
    private final Ordine ordine;

    public ServerOrdine(ServerSocket server, Ordine ordine) {
        this.server = server;
        this.ordine = ordine;
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                Thread fir = new Thread(() -> deserveste(client), "ordine-" + client.getPort());
                fir.setDaemon(true);
                fir.start();
            } catch (IOException e) {
                if (!server.isClosed()) {
                    System.out.println("Ordine: nu pot accepta o conexiune: " + e.getMessage());
                }
            }
        }
    }

    private void deserveste(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
             Writer out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII))) {
            CititorLinii linii = new CititorLinii(in);
            Linie linie;
            while ((linie = linii.urmatoarea()) != null) {
                if (!linie.preaLunga() && linie.text().isBlank()) {
                    continue; // un Enter apăsat din greșeală (D24)
                }
                List<String> raspuns = linie.preaLunga() ? List.of(Ordine.ERR_LINIE) : ordine.executa(linie.text());
                for (String rand : raspuns) {
                    out.write(rand);
                    out.write('\n');
                }
                out.flush();
                String jurnal = randJurnal(linie, raspuns);
                if (jurnal != null) {
                    System.out.println(jurnal);
                }
            }
        } catch (IOException e) {
            // Clientul a închis conexiunea.
        }
    }

    /**
     * Rândul din consolă pentru o ordine, sau null dacă nu merită scris. Un STARE reușit doar citește, iar
     * serverul îl cere la 3 secunde din fiecare tab deschis (faza 5): scris, ar îneca restul jurnalului.
     */
    static String randJurnal(Linie linie, List<String> raspuns) {
        boolean stareReusita = !linie.preaLunga() && linie.text().trim().equalsIgnoreCase("STARE")
                && !raspuns.get(0).startsWith("ERR");
        if (stareReusita) {
            return null;
        }
        return String.format("Ordine: %s → %s", linie.preaLunga() ? "(linie prea lungă)" : linie.text(), raspuns.get(0));
    }
}
