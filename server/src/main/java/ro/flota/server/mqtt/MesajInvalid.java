package ro.flota.server.mqtt;

/** Un mesaj care nu poate fi folosit. Se scrie în log și se ignoră (spec §6.1). */
public final class MesajInvalid extends Exception {

    public MesajInvalid(String motiv) {
        super(motiv);
    }
}
