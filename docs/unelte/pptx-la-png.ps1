# Exportă fiecare slide ca PNG, prin PowerPoint (COM), și, cu -Pdf, prezentarea ca PDF (faza 6, Task 12).
#   & docs\unelte\pptx-la-png.ps1 -Pptx docs\prezentare\flota-rc.pptx -Dosar docs\prezentare\slide-uri -Pdf
# PNG-urile sînt doar pentru verificare (nu intră în git); PDF-ul stă lângă .pptx.
param(
    [Parameter(Mandatory)][string]$Pptx,
    [Parameter(Mandatory)][string]$Dosar,
    [switch]$Pdf
)
$ErrorActionPreference = 'Stop'
function CaleIntreaga([string]$c) {
    if ([System.IO.Path]::IsPathRooted($c)) { return [System.IO.Path]::GetFullPath($c) }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location).Path $c))
}
$pptxCale = CaleIntreaga $Pptx
$dosarCale = CaleIntreaga $Dosar
New-Item -ItemType Directory -Force $dosarCale | Out-Null
Get-ChildItem $dosarCale -Filter 'slide-*.png' | Remove-Item

$pp = New-Object -ComObject PowerPoint.Application
$prez = $null
try {
    # ReadOnly, fără titlu, fără fereastră. Un fișier pe care PowerPoint vrea să-l repare dă eroare aici.
    $prez = $pp.Presentations.Open($pptxCale, -1, 0, 0)
    $n = $prez.Slides.Count
    for ($i = 1; $i -le $n; $i++) {
        $prez.Slides.Item($i).Export((Join-Path $dosarCale ('slide-{0:D2}.png' -f $i)), 'PNG', 1600, 900)
    }
    Write-Host "slide-uri: $n, in $dosarCale"
    if ($Pdf) {
        $pdfCale = [System.IO.Path]::ChangeExtension($pptxCale, '.pdf')
        $prez.SaveAs($pdfCale, 32) # ppSaveAsPDF
        Write-Host "scris: $pdfCale"
    }
} finally {
    if ($prez) { $prez.Close() }
    $pp.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($pp)
}
