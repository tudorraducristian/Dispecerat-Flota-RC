# Măsurătorile pentru articol (spec §10 și §16.5): tunelul, cele două găuri și întârzierea.
# Cu tot sistemul pornit (A, P, S, C), din rădăcina proiectului:
#   & docs\articol\masuratori\masoara.ps1
# Scrie docs\articol\date\e*-r*.csv și rezumat.csv. Durează în jur de 12 minute.
# Fișierul n-are BOM, deci PowerShell 5.1 îl citește ca ANSI: mesajele afișate sînt fără diacritice.
param([string]$Radacina = (Resolve-Path "$PSScriptRoot\..\..\..").Path)

$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$ordine = Join-Path $Radacina 'unelte\ordine.ps1'
$interogare = Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot 'interogari.sql')
$dosarDate = Join-Path $Radacina 'docs\articol\date'
New-Item -ItemType Directory -Force $dosarDate | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Sql([string]$q) { & $mysql -h 127.0.0.1 -u flota -pflota flota -B -N -e $q 2>$null }
function OraUtc { (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd HH:mm:ss.fff') }
function Stare([string]$masina) { Sql "SELECT stare FROM masini WHERE id = '$masina'" }

# O secundă de așteptare, cu starea mașinii notată: din ea ies secunde_offline.
function Bifa([string]$masina, [ref]$offline) {
    Start-Sleep -Seconds 1
    if ((Stare $masina) -eq 'OFFLINE') { $offline.Value++ }
}

function Exporta([string]$fisier, [string]$masina, [string]$de, [string]$pana) {
    $rows = Sql "SET @masina = '$masina', @de = '$de', @pana = '$pana'; $interogare"
    $linii = @('seq,ora_masinii,ora_sosirii,din_buffer,latenta_ms') + @($rows | ForEach-Object { $_ -replace "`t", ',' })
    [System.IO.File]::WriteAllLines((Join-Path $dosarDate $fisier), $linii, $utf8)
    return @($rows | ForEach-Object {
        $c = $_ -split "`t"
        [pscustomobject]@{ ora = [DateTimeOffset]::Parse($c[1]).UtcDateTime; dinBuffer = $c[3] -eq '1'; latenta = [double]$c[4] }
    })
}

function Percentila95($valori) {
    $v = @($valori | Sort-Object)
    if ($v.Count -eq 0) { return '' }
    return $v[[Math]::Min($v.Count - 1, [Math]::Ceiling(0.95 * $v.Count) - 1)]
}

$rezumat = @('experiment,repetitie,pierdute,recuperate,secunde_offline,latenta_medie_ms,latenta_p95_ms')

# Un experiment: contoarele la 0, 20 s liniștite, comanda, durata ei, revenirea, încă 30 s.
function Ruleaza([string]$nume, [int]$rep, [string]$masina, [string]$comanda, [int]$secunde, [string]$dupa) {
    Sql "UPDATE masini SET pierdute = 0, recuperate = 0 WHERE id = '$masina'" | Out-Null
    $offline = 0
    $de = (Get-Date).ToUniversalTime().AddSeconds(-20).ToString('yyyy-MM-dd HH:mm:ss.fff')
    for ($i = 0; $i -lt 20; $i++) { Bifa $masina ([ref]$offline) }
    $offline = 0
    $start = [DateTime]::UtcNow
    $raspuns = & $ordine -Ordine $comanda
    for ($i = 0; $i -lt $secunde; $i++) { Bifa $masina ([ref]$offline) }
    $sfarsit = [DateTime]::UtcNow
    if ($dupa) { & $ordine -Ordine $dupa | Out-Null }
    $limita = 120
    while ((Stare $masina) -ne 'ONLINE' -and $limita -gt 0) { Bifa $masina ([ref]$offline); $limita-- }
    for ($i = 0; $i -lt 30; $i++) { Bifa $masina ([ref]$offline) }
    $pana = OraUtc
    $randuri = Exporta "$nume-r$rep.csv" $masina $de $pana
    $c = Sql "SELECT pierdute, recuperate FROM masini WHERE id = '$masina'"
    $pierdute, $recuperate = $c -split "`t"
    # Latența din rezumat: doar mesajele live plecate cât a ținut perturbarea, de la comandă până la capătul ei.
    # Fereastra CSV-ului e mai largă (20 s înainte, 30 s după revenire), pentru grafice.
    $live = @($randuri | Where-Object { -not $_.dinBuffer -and $_.ora -ge $start -and $_.ora -le $sfarsit } |
        ForEach-Object { $_.latenta })
    $medie = if ($live.Count) { [Math]::Round(($live | Measure-Object -Average).Average) } else { '' }
    $p95 = Percentila95 $live
    $script:rezumat += "$nume,$rep,$pierdute,$recuperate,$offline,$medie,$p95"
    $buffer = @($randuri | Where-Object { $_.dinBuffer }).Count
    Write-Host ("{0} r{1}: {2} -> pierdute {3}, recuperate {4}, offline {5} s, latenta live {6} ms (p95 {7}), din buffer {8}, randuri {9}" -f $nume, $rep, $raspuns, $pierdute, $recuperate, $offline, $medie, $p95, $buffer, $randuri.Count)
}

if ((Stare 'masina-10') -ne 'ONLINE') { throw 'Flota nu e online: porneste A, P, S si C.' }
& $ordine -Ordine 'NORMAL TOATE' | Out-Null
for ($r = 1; $r -le 3; $r++) { Ruleaza 'e1-tunel' $r 'masina-3' 'TUNEL masina-3 15' 15 $null }
for ($r = 1; $r -le 3; $r++) { Ruleaza 'e2-gaura10' $r 'masina-5' 'GAURA masina-5 10' 10 $null }
for ($r = 1; $r -le 3; $r++) { Ruleaza 'e3-gaura4' $r 'masina-5' 'GAURA masina-5 4' 4 $null }
Ruleaza 'e4-intarziere' 1 'masina-3' 'INTARZIERE TOATE 500 200' 60 'NORMAL TOATE'
Move-Item (Join-Path $dosarDate 'e4-intarziere-r1.csv') (Join-Path $dosarDate 'e4-intarziere.csv') -Force

[System.IO.File]::WriteAllLines((Join-Path $dosarDate 'rezumat.csv'), $rezumat, $utf8)
Write-Host "Gata: $dosarDate"
