"""Scrie fiecare paragraf dintr-un .docx ca `[stil] text` (faza 6, Task 11). Doar biblioteca standard.

  python -I docs\\unelte\\stiluri_docx.py <fisier.docx>

Citește și varianta „strict” a formatului Word (alt namespace), în care e salvat template-ul IEEE.
Un tabel apare ca un rând `[tabel] R×C`, iar o imagine dintr-un paragraf ca `<imagine>` în text.
"""
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

NS_W = ('http://schemas.openxmlformats.org/wordprocessingml/2006/main',
        'http://purl.oclc.org/ooxml/wordprocessingml/main')


def _ns(radacina):
    for ns in NS_W:
        if radacina.tag.startswith('{' + ns + '}'):
            return ns
    raise ValueError('nu e un document Word: ' + radacina.tag)


def nume_stiluri(z):
    try:
        radacina = ET.fromstring(z.read('word/styles.xml'))
    except KeyError:
        return {}
    w = '{' + _ns(radacina) + '}'
    rezultat = {}
    for s in radacina.iter(w + 'style'):
        nume = s.find(w + 'name')
        if nume is not None:
            rezultat[s.get(w + 'styleId')] = nume.get(w + 'val')
    return rezultat


def _text(element, w):
    parti = []
    for e in element.iter():
        if e.tag == w + 't':
            parti.append(e.text or '')
        elif e.tag == w + 'br':
            # Doar rândul nou din paragraf; saltul de coloană sau de pagină nu e text.
            if e.get(w + 'type') in (None, 'textWrapping'):
                parti.append(' | ')
        elif e.tag == w + 'tab':
            parti.append('\t')
        elif e.tag in (w + 'drawing', w + 'pict') or e.tag.endswith('}drawing'):
            parti.append('<imagine>')
    return re.sub(r'\s+', ' ', ''.join(parti)).strip()


def paragrafe(cale):
    with zipfile.ZipFile(cale) as z:
        stiluri = nume_stiluri(z)
        radacina = ET.fromstring(z.read('word/document.xml'))
    w = '{' + _ns(radacina) + '}'
    corp = radacina.find(w + 'body')
    for e in corp:
        if e.tag == w + 'p':
            pstyle = e.find(f'{w}pPr/{w}pStyle')
            id_stil = pstyle.get(w + 'val') if pstyle is not None else 'Normal'
            stil = stiluri.get(id_stil, id_stil)
            yield f'[{stil}] {_text(e, w)}'.rstrip()
        elif e.tag == w + 'tbl':
            randuri = e.findall(w + 'tr')
            coloane = len(randuri[0].findall(w + 'tc')) if randuri else 0
            yield f'[tabel] {len(randuri)}×{coloane}'


def main(argv):
    if len(argv) != 2:
        print('folosire: python -I stiluri_docx.py <fisier.docx>', file=sys.stderr)
        return 2
    sys.stdout.reconfigure(encoding='utf-8')
    for linie in paragrafe(argv[1]):
        print(linie)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
