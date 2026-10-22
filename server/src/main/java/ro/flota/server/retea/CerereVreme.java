package ro.flota.server.retea;

import java.util.Map;

/** Ce trimite pagina Rețea: {"comanda": "TUNEL", "tinta": "masina-3", "parametri": {"secunde": 15}}. */
public record CerereVreme(String comanda, String tinta, Map<String, Integer> parametri) {
}
