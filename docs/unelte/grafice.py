"""Graficele articolului, din CSV-urile măsurătorilor (faza 6, Task 10). Doar biblioteca standard.

Din rădăcina proiectului:
  python -I docs\\unelte\\grafice.py <csv> [<csv> ...] <iesire.svg> --tip tunel-gaura --limba en --durate 15,10,4
  python -I docs\\unelte\\grafice.py <csv> <iesire.svg> --tip latenta --limba ro --durate 60

Desenul are 252 de unități pe lățime, adică lățimea unei coloane IEEE (8,9 cm) în puncte tipografice,
deci un font de 8 unități iese de 8 pt în articol.
"""
import argparse
import csv
import math
import os
import re
import sys
from datetime import datetime, timedelta
from xml.sax.saxutils import escape

LATIME = 252
FONT = "Times New Roman"
CULOARE_LIVE = '#1f4e79'
CULOARE_BUFFER = '#c55a11'
CULOARE_BANDA = '#e3e3e3'

TEXTE = {
    'en': {
        'sosire': 'Arrival time (s)', 'seq': 'Message no.', 'ora': 'Car time (s)', 'latenta': 'Latency (ms)',
        'tunel': 'Tunnel', 'gaura': 'Black hole', 'live': 'live', 'buffer': 'from buffer',
        'pierdute': 'lost', 'recuperate': 'recovered', 'medie': 'mean of 10',
        'perturbare': 'command active', 'intarziere': 'DELAY 500 ± 200 ms',
    },
    'ro': {
        'sosire': 'Ora sosirii (s)', 'seq': 'Nr. mesaj', 'ora': 'Ora mașinii (s)', 'latenta': 'Latența (ms)',
        'tunel': 'Tunel', 'gaura': 'Gaură neagră', 'live': 'live', 'buffer': 'din buffer',
        'pierdute': 'pierdute', 'recuperate': 'recuperate', 'medie': 'media pe 10',
        'perturbare': 'comanda activă', 'intarziere': 'ÎNTÂRZIERE 500 ± 200 ms',
    },
}


class Scara:
    """Duce o valoare din [dmin, dmax] în pixeli, din [pmin, pmax]. pmin > pmax pentru axa care crește în sus."""

    def __init__(self, dmin, dmax, pmin, pmax):
        self.dmin, self.dmax, self.pmin, self.pmax = dmin, dmax, pmin, pmax

    def __call__(self, v):
        if self.dmax == self.dmin:
            return (self.pmin + self.pmax) / 2
        return self.pmin + (v - self.dmin) * (self.pmax - self.pmin) / (self.dmax - self.dmin)


def _ora(text):
    return datetime.fromisoformat(text.replace('Z', '+00:00'))


def citeste(cale):
    with open(cale, encoding='utf-8') as f:
        return [{
            'seq': int(r['seq']),
            'ora_masinii': _ora(r['ora_masinii']),
            'ora_sosirii': _ora(r['ora_sosirii']),
            'din_buffer': r['din_buffer'].strip() == '1',
            'latenta_ms': float(r['latenta_ms']),
        } for r in csv.DictReader(f)]


def ora_comenzii(randuri, prag_ms):
    """Ora comenzii nu e în CSV. O aproximăm cu ora primului mesaj atins de perturbare: din buffer,
    cu latența peste prag, sau primul pierdut (după o gaură în seq, la o secundă după mesajul de dinainte)."""
    for i, r in enumerate(randuri):
        if i > 0 and r['seq'] != randuri[i - 1]['seq'] + 1:
            return randuri[i - 1]['ora_masinii'] + timedelta(seconds=1)
        if r['din_buffer'] or r['latenta_ms'] > prag_ms:
            return r['ora_masinii']
    return randuri[0]['ora_masinii']


def gradatii(dmin, dmax, cate=5):
    pas_brut = (dmax - dmin) / max(cate, 1)
    putere = 10 ** math.floor(math.log10(pas_brut)) if pas_brut > 0 else 1
    pas = next(m * putere for m in (1, 2, 5, 10) if m * putere >= pas_brut)
    v = math.ceil(dmin / pas) * pas
    rezultat = []
    while v <= dmax + 1e-9:
        rezultat.append(round(v, 6))
        v += pas
    return rezultat


def _numar(v):
    return str(int(v)) if float(v).is_integer() else f'{v:g}'


class Desen:
    def __init__(self, inaltime):
        self.inaltime = inaltime
        self.parti = []

    def text(self, x, y, s, marime=8, ancora='start', extra=''):
        self.parti.append(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{marime}" text-anchor="{ancora}"{extra}>'
                          f'{escape(s)}</text>')

    def linie(self, x1, y1, x2, y2, culoare='#000', grosime=0.6, extra=''):
        self.parti.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" '
                          f'stroke="{culoare}" stroke-width="{grosime}"{extra}/>')

    def drept(self, x, y, l, h, umplere, extra=''):
        self.parti.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{l:.1f}" height="{h:.1f}" fill="{umplere}"{extra}/>')

    def cerc(self, x, y, r, culoare, plin, clasa):
        umplere = culoare if plin else 'none'
        self.parti.append(f'<circle class="{clasa}" cx="{x:.2f}" cy="{y:.2f}" r="{r}" fill="{umplere}" '
                          f'stroke="{culoare}" stroke-width="0.5"/>')

    def svg(self):
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{LATIME}" height="{self.inaltime}" '
                f'viewBox="0 0 {LATIME} {self.inaltime}" font-family="{FONT}">\n'
                f'<rect width="100%" height="100%" fill="#fff"/>\n' + '\n'.join(self.parti) + '\n</svg>\n')


def _axe(d, sx, sy, xs, ys, stanga, dreapta, sus, jos, eticheta_y, eticheta_x=None):
    for v in xs:
        x = sx(v)
        d.linie(x, jos, x, jos + 2.5)
        if eticheta_x is not False:
            d.text(x, jos + 10, _numar(v), ancora='middle')
    for v in ys:
        y = sy(v)
        d.linie(stanga - 2.5, y, stanga, y)
        d.linie(stanga, y, dreapta, y, culoare='#d0d0d0', grosime=0.3)
        d.text(stanga - 4, y + 2.8, _numar(v), ancora='end')
    d.linie(stanga, sus, stanga, jos)
    d.linie(stanga, jos, dreapta, jos)
    xm, ym = stanga - 24, (sus + jos) / 2
    d.text(xm, ym, eticheta_y, ancora='middle', extra=f' transform="rotate(-90 {xm:.1f} {ym:.1f})"')


def _titlu_panou(cale, t):
    nume = os.path.basename(cale).lower()
    return t['tunel'] if 'tunel' in nume else t['gaura']


def tunel_gaura(cai, durate, limba):
    """Câte un panou pe CSV: pe orizontală ora sosirii, în secunde de la comandă; pe verticală seq.
    Punctele live sînt pline, cele din buffer goale; un mesaj pierdut se vede ca loc gol pe verticală."""
    t = TEXTE[limba]
    panouri = []
    for cale, durata in zip(cai, durate):
        randuri = citeste(cale)
        t0 = ora_comenzii(randuri, prag_ms=1000)
        vizibile = [r for r in randuri if (r['ora_sosirii'] - t0).total_seconds() >= -10]
        seq0 = vizibile[0]['seq']
        pierdute = sum(b['seq'] - a['seq'] - 1 for a, b in zip(vizibile, vizibile[1:]) if b['seq'] > a['seq'] + 1)
        recuperate = sum(1 for r in vizibile if r['din_buffer'])
        panouri.append((cale, durata, t0, vizibile, seq0, pierdute, recuperate))

    xmax = max((r['ora_sosirii'] - p[2]).total_seconds() for p in panouri for r in p[3])
    xmin = -10
    xs = gradatii(xmin, xmax, 6)
    stanga, dreapta = 34, LATIME - 6
    h_panou, sus0 = 70, 22
    inaltime = sus0 + len(panouri) * (h_panou + 22) + 6
    d = Desen(inaltime)
    sx = Scara(xmin, xs[-1] if xs[-1] >= xmax else xmax, stanga, dreapta)

    # Legenda, sus.
    d.cerc(stanga + 4, 9, 2, CULOARE_LIVE, True, 'legenda')
    d.text(stanga + 9, 11.8, t['live'])
    d.cerc(stanga + 36, 9, 2, CULOARE_BUFFER, False, 'legenda')
    d.text(stanga + 41, 11.8, t['buffer'])
    d.drept(stanga + 92, 5.5, 9, 7, CULOARE_BANDA)
    d.text(stanga + 104, 11.8, t['perturbare'])

    for i, (cale, durata, t0, vizibile, seq0, pierdute, recuperate) in enumerate(panouri):
        sus = sus0 + i * (h_panou + 22)
        jos = sus + h_panou
        ymax = vizibile[-1]['seq'] - seq0
        ys = gradatii(0, ymax, 3)
        sy = Scara(0, max(ymax, ys[-1]), jos, sus)
        d.drept(sx(0), sus, sx(durata) - sx(0), h_panou, CULOARE_BANDA)
        _axe(d, sx, sy, xs, ys, stanga, dreapta, sus, jos, t['seq'])
        litera = chr(ord('a') + i)
        d.text(stanga + 3, sus + 8, f'({litera}) {_titlu_panou(cale, t)} {durata} s: '
                                    f'{pierdute} {t["pierdute"]}, {recuperate} {t["recuperate"]}')
        for r in vizibile:
            x = sx((r['ora_sosirii'] - t0).total_seconds())
            y = sy(r['seq'] - seq0)
            culoare = CULOARE_BUFFER if r['din_buffer'] else CULOARE_LIVE
            d.cerc(x, y, 1.3, culoare, not r['din_buffer'], 'mesaj buffer' if r['din_buffer'] else 'mesaj live')
    d.text((stanga + dreapta) / 2, inaltime - 4, t['sosire'], ancora='middle')
    return d.svg()


def latenta(cale, durata, limba):
    """Latența fiecărui mesaj față de ora lui de pe mașină, plus media mobilă pe 10 mesaje."""
    t = TEXTE[limba]
    randuri = citeste(cale)
    t0 = ora_comenzii(randuri, prag_ms=250)
    vizibile = [r for r in randuri if (r['ora_masinii'] - t0).total_seconds() >= -10]
    puncte = [((r['ora_masinii'] - t0).total_seconds(), r['latenta_ms']) for r in vizibile]
    xmin, xmax = -10, max(p[0] for p in puncte)
    xs = gradatii(xmin, xmax, 6)
    ymax = max(p[1] for p in puncte)
    ys = gradatii(0, ymax, 4)
    stanga, dreapta, sus, jos = 38, LATIME - 6, 20, 130
    d = Desen(jos + 22)
    sx = Scara(xmin, max(xmax, xs[-1]), stanga, dreapta)
    sy = Scara(0, max(ymax, ys[-1]), jos, sus)
    d.drept(sx(0), sus, sx(durata) - sx(0), jos - sus, CULOARE_BANDA)
    _axe(d, sx, sy, xs, ys, stanga, dreapta, sus, jos, t['latenta'])
    for x, y in puncte:
        d.cerc(sx(x), sy(y), 1.4, CULOARE_LIVE, True, 'mesaj live')
    medie = []
    for i in range(len(puncte)):
        fereastra = [p[1] for p in puncte[max(0, i - 9):i + 1]]
        medie.append((puncte[i][0], sum(fereastra) / len(fereastra)))
    traseu = ' '.join(f'{sx(x):.1f},{sy(y):.1f}' for x, y in medie)
    d.parti.append(f'<polyline points="{traseu}" fill="none" stroke="{CULOARE_BUFFER}" stroke-width="1"/>')
    # Legenda.
    d.cerc(stanga + 4, 9, 1.6, CULOARE_LIVE, True, 'legenda')
    d.text(stanga + 9, 11.8, t['live'])
    d.linie(stanga + 32, 9, stanga + 44, 9, culoare=CULOARE_BUFFER, grosime=1)
    d.text(stanga + 47, 11.8, t['medie'])
    d.drept(stanga + 98, 5.5, 9, 7, CULOARE_BANDA)
    d.text(stanga + 110, 11.8, t['intarziere'])
    d.text((stanga + dreapta) / 2, jos + 20, t['ora'], ancora='middle')
    return d.svg()


def main(argv=None):
    p = argparse.ArgumentParser(description='Graficele articolului flota-rc.')
    p.add_argument('fisiere', nargs='+', help='unul sau mai multe CSV-uri, apoi SVG-ul de ieșire')
    p.add_argument('--tip', choices=['tunel-gaura', 'latenta'], required=True)
    p.add_argument('--limba', choices=['en', 'ro'], required=True)
    p.add_argument('--durate', required=True, help='durata comenzii, în secunde, câte una pe CSV: 15,10,4')
    a = p.parse_args(argv)
    if len(a.fisiere) < 2:
        p.error('lipsește SVG-ul de ieșire')
    cai, iesire = a.fisiere[:-1], a.fisiere[-1]
    durate = [int(x) for x in re.split(r'[,\s]+', a.durate.strip())]
    if len(durate) != len(cai):
        p.error('--durate are nevoie de câte o valoare pe CSV')
    if a.tip == 'tunel-gaura':
        svg = tunel_gaura(cai, durate, a.limba)
    else:
        if len(cai) != 1:
            p.error('latenta primește un singur CSV')
        svg = latenta(cai[0], durate[0], a.limba)
    with open(iesire, 'w', encoding='utf-8', newline='\n') as f:
        f.write(svg)
    print(f'scris: {iesire}')


if __name__ == '__main__':
    sys.exit(main())
