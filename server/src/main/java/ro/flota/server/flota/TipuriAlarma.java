package ro.flota.server.flota;

import java.util.Set;

/**
 * Tipurile de alarmă pe care le scriu mașina și regulile serverului. Filtrul din pagina Alarme le primește
 * din URL, iar un tip scris greșit trebuie să dea 400, nu o listă goală care pare adevărată.
 */
public final class TipuriAlarma {

    public static final Set<String> TOATE = Set.of(
            "SUPRAINCALZIRE", "FRANARE_BRUSCA", "VITEZA_PESTE_LIMITA", "IESIRE_DIN_ZONA", "OFFLINE_PRELUNGIT");

    private TipuriAlarma() {
    }
}
