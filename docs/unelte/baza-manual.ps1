# Face o dată docs\manual\baza-manual.docx: documentul gol din Word, pe A4, cu stilurile lui și numărul paginii
# în subsol (faza 6, Task 13). Manualul se scrie apoi peste o copie a lui, cu docx-din-md.ps1.
#   & docs\unelte\baza-manual.ps1
$ErrorActionPreference = 'Stop'
$iesire = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\manual\baza-manual.docx'))
New-Item -ItemType Directory -Force (Split-Path $iesire) | Out-Null
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$doc = $null
try {
    $doc = $word.Documents.Add()
    $ps = $doc.PageSetup
    $ps.PaperSize = 7 # wdPaperA4
    $ps.TopMargin = 72; $ps.BottomMargin = 72; $ps.LeftMargin = 72; $ps.RightMargin = 72
    $doc.Styles.Item('Normal').Font.Name = 'Calibri'
    $doc.Styles.Item('Normal').Font.Size = 11
    # Limba documentului: română, ca Word să nu sublinieze tot textul.
    foreach ($s in 'Normal', 'Heading 1', 'Heading 2', 'Heading 3', 'List Bullet', 'Caption', 'Title') { $doc.Styles.Item($s).LanguageID = 1048 }
    [void]$doc.Sections.Item(1).Footers.Item(1).PageNumbers.Add(1, $true) # centrat, și pe prima pagină
    $doc.SaveAs2($iesire, 16)
    Write-Host "scris: $iesire"
} finally {
    if ($doc) { $doc.Close($false) }
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
