# Vorbește cu proxy-ul pe portul 1885 (spec §5.4).
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
#       mod interactiv; o linie goală închide
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1 -Ordine "TUNEL masina-3 15"
#       o singură ordine, apoi iese
param([string]$Ordine)

$client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 1885)
$flux = $client.GetStream()
$ascii = [System.Text.Encoding]::ASCII
$scriitor = [System.IO.StreamWriter]::new($flux, $ascii)
$scriitor.NewLine = "`n"
$scriitor.AutoFlush = $true
$cititor = [System.IO.StreamReader]::new($flux, $ascii)

# Trimite o linie și afișează răspunsul. La STARE citește până la rândul cu un singur punct.
function Trimite([string]$linie) {
    $scriitor.WriteLine($linie)
    $raspuns = $cititor.ReadLine()
    Write-Output $raspuns
    if ($linie.Trim() -ieq 'STARE') {
        while ($null -ne $raspuns -and $raspuns -ne '.') {
            $raspuns = $cititor.ReadLine()
            Write-Output $raspuns
        }
    }
}

try {
    if ($Ordine) {
        Trimite $Ordine
        return
    }
    while ($true) {
        $linie = Read-Host 'ordine'
        if ([string]::IsNullOrWhiteSpace($linie)) { break }
        Trimite $linie
    }
} finally {
    $client.Close()
}
