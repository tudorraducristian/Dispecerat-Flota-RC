"""Testele pentru grafice.py (faza 6, Task 10). Rulare, din rădăcina proiectului:
python -I -m unittest discover -s docs\\unelte -p "test_*.py"
"""
import os
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import grafice  # noqa: E402

SVG = '{http://www.w3.org/2000/svg}'

# 5 mesaje: 2 live, o gaură în seq (12003 lipsește), 2 din buffer.
CSV_MIC = """seq,ora_masinii,ora_sosirii,din_buffer,latenta_ms
12000,2026-10-26T15:42:55.000Z,2026-10-26T15:42:55.100Z,0,100
12001,2026-10-26T15:42:56.000Z,2026-10-26T15:42:56.080Z,0,80
12002,2026-10-26T15:42:57.000Z,2026-10-26T15:43:03.000Z,1,6000
12004,2026-10-26T15:42:59.000Z,2026-10-26T15:43:03.010Z,1,4010
12005,2026-10-26T15:43:03.500Z,2026-10-26T15:43:03.600Z,0,100
"""


def scrie_csv(continut):
    f = tempfile.NamedTemporaryFile('w', suffix='-e1-tunel.csv', delete=False, encoding='utf-8')
    f.write(continut)
    f.close()
    return f.name


class TestGrafice(unittest.TestCase):

    def setUp(self):
        self.csv = scrie_csv(CSV_MIC)

    def tearDown(self):
        os.remove(self.csv)

    def test_scara_duce_capetele_la_margini(self):
        s = grafice.Scara(-10, 50, 40, 240)
        self.assertAlmostEqual(s(-10), 40)
        self.assertAlmostEqual(s(50), 240)
        self.assertAlmostEqual(s(20), 140)
        inversa = grafice.Scara(0, 100, 180, 20)  # axa verticală crește în sus
        self.assertAlmostEqual(inversa(0), 180)
        self.assertAlmostEqual(inversa(100), 20)

    def test_tunel_gaura_are_cate_un_punct_pe_mesaj(self):
        svg = grafice.tunel_gaura([self.csv], [5], 'en')
        radacina = ET.fromstring(svg)
        cercuri = [c for c in radacina.iter(SVG + 'circle') if 'mesaj' in c.get('class', '')]
        self.assertEqual(len(cercuri), 5)
        goale = [c for c in cercuri if c.get('fill') == 'none']
        self.assertEqual(len(goale), 2)

    def test_eticheta_axei_in_limba_ceruta(self):
        en = grafice.tunel_gaura([self.csv], [5], 'en')
        ro = grafice.tunel_gaura([self.csv], [5], 'ro')
        self.assertIn('Arrival time (s)', en)
        self.assertIn('Ora sosirii (s)', ro)
        self.assertNotIn('Ora sosirii (s)', en)

    def test_iesirea_e_svg_valid(self):
        for svg in (grafice.tunel_gaura([self.csv], [5], 'ro'), grafice.latenta(self.csv, 5, 'en')):
            radacina = ET.fromstring(svg)
            self.assertEqual(radacina.tag, SVG + 'svg')

    def test_ora_comenzii_vine_din_primul_mesaj_atins(self):
        # Primul mesaj atins e 12002, din buffer: comanda a venit cel târziu la ora lui.
        randuri = grafice.citeste(self.csv)
        t0 = grafice.ora_comenzii(randuri, prag_ms=1000)
        self.assertEqual(t0, randuri[2]['ora_masinii'])


if __name__ == '__main__':
    unittest.main()
