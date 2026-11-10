# Face un .docx dintr-un Markdown, pe un document de bază, prin Word (COM) (faza 6, Task 11 și 13).
#   & docs\unelte\docx-din-md.ps1 -Baza <docx> -Text <md> -Iesire <docx> -Stiluri <json> [-Pdf]
# Deschide o copie a bazei, păstrează stilurile și secțiunile ei, golește corpul și adaugă blocurile din
# Markdown, fiecare cu stilul din harta JSON. Cu -Pdf salvează și PDF-ul, alături.
#
# Markdown-ul acceptat (planul fazei 6, Task 11):
#   antetul --- cu titlu:, autor: (rânduri despărțite de " | "), abstract:, cuvinte-cheie:
#   #, ##, ###; paragrafe; "- " și "1. "; ![Legenda](cale.png); "Table: Legenda" urmat de un tabel cu |;
#   # References, apoi [n] ...; **gros**, *cursiv*, `cod`.
# Fișierul n-are BOM, deci PowerShell 5.1 îl citește ca ANSI: textele cu diacritice vin doar din JSON și MD.
param(
    [Parameter(Mandatory)][string]$Baza,
    [Parameter(Mandatory)][string]$Text,
    [Parameter(Mandatory)][string]$Iesire,
    [Parameter(Mandatory)][string]$Stiluri,
    [switch]$Pdf
)
$ErrorActionPreference = 'Stop'

function CaleIntreaga([string]$c) {
    if ([System.IO.Path]::IsPathRooted($c)) { return [System.IO.Path]::GetFullPath($c) }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $c))
}

$bazaCale = CaleIntreaga $Baza
$textCale = CaleIntreaga $Text
$iesireCale = CaleIntreaga $Iesire
$harta = Get-Content -Raw -Encoding UTF8 (CaleIntreaga $Stiluri) | ConvertFrom-Json
$dosarText = Split-Path $textCale
$linii = @(Get-Content -Encoding UTF8 $textCale)

# ---------- Markdown -> blocuri ----------
$antet = @{}
$i = 0
if ($linii.Count -gt 0 -and $linii[0].Trim() -eq '---') {
    $i = 1
    while ($i -lt $linii.Count -and $linii[$i].Trim() -ne '---') {
        if ($linii[$i] -match '^([a-z-]+):\s*(.*)$') { $antet[$Matches[1]] = $Matches[2].Trim() }
        $i++
    }
    $i++
}

$blocuri = New-Object System.Collections.ArrayList
$paragraf = New-Object System.Collections.ArrayList
$inReferinte = $false
function InchideParagraf {
    if ($script:paragraf.Count -gt 0) {
        [void]$script:blocuri.Add(@{ fel = 'corp'; text = ($script:paragraf -join ' ') })
        $script:paragraf.Clear()
    }
}
while ($i -lt $linii.Count) {
    $l = $linii[$i]
    if ($l.Trim() -eq '') { InchideParagraf; $i++; continue }
    if ($l -match '^(#{1,3})\s+(.*)$') {
        InchideParagraf
        $nivel = $Matches[1].Length; $titlu = $Matches[2].Trim()
        $faraNumar = $nivel -eq 1 -and $harta.titluriFaraNumar -contains $titlu
        $inReferinte = $faraNumar -and $titlu -eq 'References'
        [void]$blocuri.Add(@{ fel = $(if ($faraNumar) { 'faraNumar' } else { "h$nivel" }); text = $titlu })
        $i++; continue
    }
    if ($inReferinte -and $l -match '^\[\d+\]\s+(.*)$') {
        InchideParagraf
        [void]$blocuri.Add(@{ fel = 'referinta'; text = $Matches[1] }); $i++; continue
    }
    if ($l -match '^!\[(.*)\]\((.*)\)\s*$') {
        InchideParagraf
        [void]$blocuri.Add(@{ fel = 'imagine'; text = $Matches[1]; cale = (Join-Path $dosarText $Matches[2]) }); $i++; continue
    }
    if ($l -match '^Table:\s*(.*)$') {
        InchideParagraf
        $legenda = $Matches[1]; $i++
        while ($i -lt $linii.Count -and $linii[$i].Trim() -eq '') { $i++ }
        $randuri = New-Object System.Collections.ArrayList
        while ($i -lt $linii.Count -and $linii[$i].Trim().StartsWith('|')) {
            $celule = @($linii[$i].Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() })
            if (-not ($celule | Where-Object { $_ -notmatch '^:?-{3,}:?$' })) { $i++; continue } # rândul |---|
            [void]$randuri.Add($celule); $i++
        }
        [void]$blocuri.Add(@{ fel = 'tabel'; text = $legenda; randuri = $randuri }); continue
    }
    if ($l -match '^- (.*)$') { InchideParagraf; [void]$blocuri.Add(@{ fel = 'lista'; text = $Matches[1] }); $i++; continue }
    if ($l -match '^\d+\. (.*)$') { InchideParagraf; [void]$blocuri.Add(@{ fel = 'listaNumerotata'; text = $Matches[1] }); $i++; continue }
    [void]$paragraf.Add($l.Trim()); $i++
}
InchideParagraf

# **gros**, *cursiv*, `cod` -> textul simplu și bucățile de formatat.
function Desparte([string]$md) {
    $text = New-Object System.Text.StringBuilder
    $formate = New-Object System.Collections.ArrayList
    $re = [regex]'\*\*(.+?)\*\*|`([^`]+)`|\*([^*]+)\*'
    $poz = 0
    foreach ($m in $re.Matches($md)) {
        [void]$text.Append($md.Substring($poz, $m.Index - $poz))
        if ($m.Groups[1].Success) { $fel = 'gros'; $continut = $m.Groups[1].Value }
        elseif ($m.Groups[2].Success) { $fel = 'cod'; $continut = $m.Groups[2].Value }
        else { $fel = 'cursiv'; $continut = $m.Groups[3].Value }
        [void]$formate.Add(@{ de = $text.Length; lungime = $continut.Length; fel = $fel })
        [void]$text.Append($continut)
        $poz = $m.Index + $m.Length
    }
    [void]$text.Append($md.Substring($poz))
    return @{ text = $text.ToString(); formate = $formate }
}

# ---------- Word ----------
Copy-Item -LiteralPath $bazaCale -Destination $iesireCale -Force
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$doc = $null
try {
    $doc = $word.Documents.Open($iesireCale, $false, $false)
    $doc.TrackRevisions = $false

    # Paragraful scris la poziția $script:poz, cu stilul lui; textul de pe ecran rămâne doar cel din MD.
    function Adauga([string]$stil, [string]$md, [string]$prefix = '') {
        $d = Desparte $md
        $text = $prefix + $d.text
        $start = $script:poz
        $doc.Range($start, $start).InsertAfter($text + "`r")
        $par = $doc.Range($start, $start + $text.Length + 1)
        $par.Style = $doc.Styles.Item($stil)
        $par.Font.Reset()
        foreach ($f in $d.formate) {
            $r = $doc.Range($start + $prefix.Length + $f.de, $start + $prefix.Length + $f.de + $f.lungime)
            switch ($f.fel) {
                'gros' { $r.Font.Bold = 1 }
                'cursiv' { $r.Font.Italic = 1 }
                'cod' { $r.Font.Name = $harta.cod }
            }
        }
        $script:poz = $start + $text.Length + 1
    }

    function LatimeColoana([int]$poz) {
        $ps = $doc.Range($poz, $poz).Sections.Item(1).PageSetup
        if ($ps.TextColumns.Count -gt 1) { return $ps.TextColumns.Width }
        return $ps.PageWidth - $ps.LeftMargin - $ps.RightMargin
    }

    if ($harta.antet -eq 'ieee') {
        # Template-ul IEEE: S1 titlul, S2 autorii (3 coloane), S3 goală, S4 corpul (2 coloane), S5 caseta finală.
        if ($doc.Sections.Count -lt 4) { throw 'baza nu are cele 4 secțiuni ale template-ului IEEE' }
        while ($doc.Shapes.Count -gt 0) { $doc.Shapes.Item(1).Delete() }
        # Subsolul cu „XXX-X-XXXX... ©20XX IEEE” e tot text de îndrumare.
        foreach ($s in $doc.Sections) { foreach ($k in 1, 2, 3) { $s.Footers.Item($k).Range.Text = ''; $s.Headers.Item($k).Range.Text = '' } }
        $corp = $doc.Sections.Item(4).Range
        $doc.Range($corp.Start, $corp.End - 1).Delete() | Out-Null
        $autori = $doc.Sections.Item(2).Range
        $r = $doc.Range($autori.Start, $autori.End - 1)
        $r.Text = (($antet['autor'] -split '\s*\|\s*') -join [char]11)
        $r.Style = $doc.Styles.Item($harta.autor)
        $doc.Sections.Item(2).PageSetup.TextColumns.SetCount(1)
        $titlu = $doc.Sections.Item(1).Range
        $r = $doc.Range($titlu.Start, $titlu.End - 1)
        $r.Text = $antet['titlu']
        $r.Style = $doc.Styles.Item($harta.titlu)
        $script:poz = $doc.Sections.Item(4).Range.Start
        Adauga $harta.abstract $antet['abstract'] $harta.prefixAbstract
        Adauga $harta.cuvinteCheie $antet['cuvinte-cheie'] $harta.prefixCuvinteCheie
    } else {
        # Un document simplu: totul se golește; titlul, apoi cuprinsul, dacă e cerut.
        $doc.Content.Delete() | Out-Null
        $script:poz = 0
        Adauga $harta.titlu $antet['titlu']
        if ($antet['subtitlu']) { Adauga $harta.subtitlu $antet['subtitlu'] }
        if ($harta.cuprins) {
            Adauga $harta.titluCuprins $harta.textCuprins
            $start = $script:poz
            $doc.Range($start, $start).InsertAfter("`r")
            $toc = $doc.TablesOfContents.Add($doc.Range($start, $start), $true, 1, 3)
            $script:poz = $toc.Range.End + 1
            $doc.Range($script:poz - 1, $script:poz - 1).InsertBreak(7) # pagină nouă după cuprins
            $script:poz = $doc.Content.End - 1
        }
    }

    foreach ($b in $blocuri) {
        switch ($b.fel) {
            'h1' { Adauga $harta.h1 $b.text }
            'h2' { Adauga $harta.h2 $b.text }
            'h3' { Adauga $harta.h3 $b.text }
            'faraNumar' { Adauga $harta.titluFaraNumar $b.text }
            'corp' { Adauga $harta.corp $b.text }
            'lista' { Adauga $harta.lista $b.text }
            'listaNumerotata' { Adauga $harta.listaNumerotata $b.text }
            'referinta' { Adauga $harta.referinte $b.text }
            'imagine' {
                if (-not (Test-Path -LiteralPath $b.cale)) { throw "lipsește imaginea $($b.cale)" }
                $start = $script:poz
                $doc.Range($start, $start).InsertAfter("`r")
                $par = $doc.Range($start, $start + 1)
                $par.Style = $doc.Styles.Item($harta.imagine)
                $par.ParagraphFormat.Alignment = 1
                $par.ParagraphFormat.KeepWithNext = -1
                $img = $doc.InlineShapes.AddPicture((Resolve-Path -LiteralPath $b.cale).Path, $false, $true, $doc.Range($start, $start))
                $img.LockAspectRatio = -1
                $latime = LatimeColoana $start
                if ($harta.latimeImagine) { $latime = [Math]::Min($latime, [double]$harta.latimeImagine) }
                if ($img.Width -gt $latime -or $harta.antet -eq 'ieee') { $img.Width = $latime }
                $script:poz = $start + 2
                Adauga $harta.figura $b.text
            }
            'tabel' {
                Adauga $harta.tabel $b.text
                $doc.Range($script:poz - 1, $script:poz - 1).ParagraphFormat.KeepWithNext = -1
                $randuri = $b.randuri
                $start = $script:poz
                $doc.Range($start, $start).InsertAfter("`r")
                $t = $doc.Tables.Add($doc.Range($start, $start), $randuri.Count, $randuri[0].Count)
                $t.Borders.Enable = 1
                for ($ri = 0; $ri -lt $randuri.Count; $ri++) {
                    for ($ci = 0; $ci -lt $randuri[0].Count; $ci++) {
                        $celula = $t.Cell($ri + 1, $ci + 1).Range
                        $d = Desparte ([string]$randuri[$ri][$ci])
                        $celula.Text = $d.text
                        $celula.Style = $doc.Styles.Item($(if ($ri -eq 0) { $harta.celulaCap } else { $harta.celula }))
                        if ($ri -eq 0) { $celula.Font.Bold = 1 }
                    }
                }
                $t.AutoFitBehavior(1) # după conținut
                $t.AutoFitBehavior(2) # apoi pe lățimea coloanei
                $t.Rows.Alignment = 1
                # Paragraful gol pus înaintea tabelului a rămas după el; următorul bloc vine în locul lui.
                $dupa = $doc.Range($t.Range.End, $t.Range.End + 1)
                if ($dupa.Text -eq "`r" -and $dupa.Paragraphs.Item(1).Range.Text -eq "`r") { $dupa.Delete() | Out-Null }
                $script:poz = $t.Range.End
            }
        }
    }

    # Paragraful de după ultimul bloc (cel cu sfârșitul de secțiune) rămâne gol, cu stilul de bază.
    $coada = $doc.Range($script:poz, $script:poz).Paragraphs.Item(1)
    if ($coada.Range.Text.Trim([char]13, [char]12) -eq '') { $coada.Style = $doc.Styles.Item($harta.imagine) }
    if ($harta.cuprins -and $doc.TablesOfContents.Count -gt 0) { $doc.TablesOfContents.Item(1).Update() }
    $doc.Save()
    if ($Pdf) {
        $pdfCale = [System.IO.Path]::ChangeExtension($iesireCale, '.pdf')
        $doc.SaveAs2($pdfCale, 17)
        Write-Host "scris: $pdfCale"
    }
    Write-Host "scris: $iesireCale"
} finally {
    if ($doc) { $doc.Close($false) }
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
