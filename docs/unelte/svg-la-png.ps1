# Face un PNG dintr-un SVG, cu Edge fără fereastră (faza 6, Task 10).
#   & docs\unelte\svg-la-png.ps1 -Svg docs\articol\figuri\arhitectura.svg -Png docs\articol\figuri\arhitectura.png -Latime 525 -Inaltime 400
# Latime și Inaltime sînt în pixeli CSS; imaginea iese de două ori mai mare (--force-device-scale-factor=2).
# SVG-ul se întinde pe toată pagina, deci raportul Latime/Inaltime trebuie să fie cel din viewBox.
param(
    [Parameter(Mandatory)][string]$Svg,
    [Parameter(Mandatory)][string]$Png,
    [Parameter(Mandatory)][int]$Latime,
    [Parameter(Mandatory)][int]$Inaltime
)
$ErrorActionPreference = 'Stop'
$edge = @("${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
          "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe") | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $edge) { throw 'Edge nu e instalat.' }

$svgCale = (Resolve-Path $Svg).Path
$pngCale = if ([System.IO.Path]::IsPathRooted($Png)) { $Png } else { [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Png)) }
$lucru = Join-Path ([System.IO.Path]::GetTempPath()) ("svg-la-png-" + [guid]::NewGuid())
New-Item -ItemType Directory $lucru | Out-Null
try {
    # O pagină cu SVG-ul pus ca imagine pe toată suprafața, ca să nu conteze width/height din fișier.
    $uri = ([System.Uri]$svgCale).AbsoluteUri
    $html = "<!doctype html><html><head><meta charset=`"utf-8`"><style>html,body{margin:0;background:#fff}" +
            "img{display:block;width:${Latime}px;height:${Inaltime}px}</style></head><body><img src=`"$uri`"></body></html>"
    $pagina = Join-Path $lucru 'pagina.html'
    [System.IO.File]::WriteAllText($pagina, $html, (New-Object System.Text.UTF8Encoding($false)))
    # Start-Process lipește argumentele cu spații, deci căile (cu „Semestrul 1”) se pun între ghilimele.
    $argumente = "--headless --disable-gpu --hide-scrollbars --allow-file-access-from-files " +
        "`"--user-data-dir=$lucru\profil`" --force-device-scale-factor=2 --window-size=$Latime,$Inaltime " +
        "`"--screenshot=$pngCale`" `"$(([System.Uri]$pagina).AbsoluteUri)`""
    $p = Start-Process -FilePath $edge -ArgumentList $argumente -Wait -PassThru -WindowStyle Hidden
    if (-not (Test-Path $pngCale)) { throw "Edge n-a scris $pngCale (cod $($p.ExitCode))." }
    Write-Host "scris: $Png"
} finally {
    Remove-Item -Recurse -Force $lucru -ErrorAction SilentlyContinue
}
