package ro.flota.simulator.geo;

import java.util.List;

/** O buclă: după ultimul punct, drumul continuă spre primul. */
public final class Traseu {

    private final String id;
    private final List<Punct> puncte;
    /** inceput[i] = câți metri sînt de la start până la începutul segmentului i. */
    private final double[] inceput;
    private final double lungime;

    public Traseu(String id, List<Punct> puncte) {
        if (puncte.size() < 2) {
            throw new IllegalArgumentException("Traseul " + id + " are mai puțin de 2 puncte");
        }
        this.id = id;
        this.puncte = List.copyOf(puncte);
        this.inceput = new double[this.puncte.size()];
        double total = 0;
        for (int i = 0; i < this.puncte.size(); i++) {
            inceput[i] = total;
            total += this.puncte.get(i).distantaPana(urmatorul(i));
        }
        if (total <= 0) {
            throw new IllegalArgumentException("Traseul " + id + " are lungimea 0");
        }
        this.lungime = total;
    }

    public String id() {
        return id;
    }

    public double lungime() {
        return lungime;
    }

    /** Aduce orice distanță în bucla [0, lungime). */
    public double normalizeaza(double metri) {
        double x = metri % lungime;
        return x < 0 ? x + lungime : x;
    }

    /** Punctul aflat la atâția metri de start, mergând pe buclă. */
    public Punct punctLa(double metri) {
        double x = normalizeaza(metri);
        int i = segmentul(x);
        double sfarsit = i + 1 < puncte.size() ? inceput[i + 1] : lungime;
        double lungimeSegment = sfarsit - inceput[i];
        if (lungimeSegment == 0) {
            return puncte.get(i);
        }
        return puncte.get(i).spre(urmatorul(i), (x - inceput[i]) / lungimeSegment);
    }

    private Punct urmatorul(int i) {
        return puncte.get((i + 1) % puncte.size());
    }

    /**
     * Ultimul segment care începe la cel mult x, găsit prin căutare binară.
     * Segmentele de lungime 0, adică punctele dublate, au același început ca următorul,
     * așa că sînt sărite.
     */
    private int segmentul(double x) {
        int st = 0;
        int dr = puncte.size() - 1;
        while (st < dr) {
            int mij = (st + dr + 1) / 2;
            if (inceput[mij] <= x) {
                st = mij;
            } else {
                dr = mij - 1;
            }
        }
        return st;
    }
}
