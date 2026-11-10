# Reface figurile articolului: graficele din CSV-uri, apoi câte un PNG din fiecare SVG (faza 6, Task 10).
# Din rădăcina proiectului:  & docs\articol\figuri\refa.ps1
# harta.png nu se reface de aici: e o captură din aplicația pornită (docs\unelte\capturi.mjs).
$ErrorActionPreference = 'Stop'
$radacina = (Resolve-Path "$PSScriptRoot\..\..\..").Path
Push-Location $radacina
try {
    $date = 'docs\articol\date'
    $figuri = 'docs\articol\figuri'
    foreach ($limba in 'en', 'ro') {
        python -I docs\unelte\grafice.py "$date\e1-tunel-r1.csv" "$date\e2-gaura10-r1.csv" "$date\e3-gaura4-r1.csv" `
            "$figuri\tunel-gaura-$limba.svg" --tip tunel-gaura --limba $limba --durate 15,10,4
        if ($LASTEXITCODE) { throw 'grafice.py a picat' }
        python -I docs\unelte\grafice.py "$date\e4-intarziere.csv" "$figuri\latenta-$limba.svg" --tip latenta --limba $limba --durate 60
        if ($LASTEXITCODE) { throw 'grafice.py a picat' }
    }
    # Lățimea unei coloane (252 de unități) devine 525 px CSS, deci 1050 px în PNG: ~300 dpi la 8,9 cm.
    foreach ($svg in Get-ChildItem "$figuri\*.svg") {
        $x = [xml](Get-Content -Raw -Encoding UTF8 $svg.FullName)
        $latime = [math]::Round([double]$x.svg.width * 525 / 252)
        $inaltime = [math]::Round([double]$x.svg.height * 525 / 252)
        & "$radacina\docs\unelte\svg-la-png.ps1" -Svg $svg.FullName -Png ($svg.FullName -replace '\.svg$', '.png') -Latime $latime -Inaltime $inaltime
    }
} finally {
    Pop-Location
}
