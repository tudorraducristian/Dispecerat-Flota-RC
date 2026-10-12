package ro.flota.server.live;

/** Forma oricărui mesaj live (spec §6.6): {"tip": "...", "date": {...}}. */
public record MesajLive(String tip, Object date) {
}
