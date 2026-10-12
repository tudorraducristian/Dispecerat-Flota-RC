package ro.flota.simulator.masina;

/** O pauză. În program e Thread::sleep; în teste, o listă care ține minte cât s-a „dormit". */
@FunctionalInterface
public interface Somn {

    void dormi(long ms) throws InterruptedException;
}
