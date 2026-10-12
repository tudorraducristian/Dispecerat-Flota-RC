package ro.flota.simulator.config;

import ro.flota.simulator.geo.Traseu;

/** O mașină din date\flota.json: start e partea din buclă deja parcursă la pornire, între 0 și 1. */
public record DefinitieMasina(String id, Traseu traseu, double start, String zona) {
}
