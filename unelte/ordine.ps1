# Vorbește cu proxy-ul pe portul 1885 (spec §5.4).
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
#       mod interactiv; o linie goală închide
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1 -Ordine "TUNEL masina-3 15"
#       o singură ordine, apoi iese
# Fișierul n-are BOM, deci PowerShell 5.1 îl citește ca ANSI: mesajele afișate sînt fără diacritice.
param([string]$Ordine)

# O linie goală nu primește răspuns de la proxy (D24), deci n-are rost trimisă.
if ($PSBoundParameters.ContainsKey('Ordine') -and [string]::IsNullOrWhiteSpace($Ordine)) {
    Write-Host 'Ordinea e goala; nu trimit nimic.'
    exit 1
}

try {
    $client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 1885)
} catch {
    Write-Host 'Proxy-ul nu raspunde pe 127.0.0.1:1885. Porneste-l intai: .\mvnw.cmd -q -pl proxy compile exec:java'
    exit 1
}
# Plasa de siguranta: un raspuns care nu mai vine nu agata scriptul la nesfarsit.
$client.ReceiveTimeout = 5000
$flux = $client.GetStream()
$ascii = [System.Text.Encoding]::ASCII
$scriitor = [System.IO.StreamWriter]::new($flux, $ascii)
$scriitor.NewLine = "`n"
$scriitor.AutoFlush = $true
$cititor = [System.IO.StreamReader]::new($flux, $ascii)

# Trimite o linie și afișează răspunsul. La un STARE reușit citește până la rândul cu un singur punct;
# la ERR (de exemplu „linie prea lunga”) răspunsul are un singur rând.
function Trimite([string]$linie) {
    $scriitor.WriteLine($linie)
    $raspuns = $cititor.ReadLine()
    Write-Output $raspuns
    if ($linie.Trim() -ieq 'STARE' -and $null -ne $raspuns -and -not $raspuns.StartsWith('ERR')) {
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
} catch {
    # PowerShell învelește excepția .NET (IOException la time-out) într-una a lui; mesajul util e înăuntru.
    $cauza = if ($_.Exception.InnerException) { $_.Exception.InnerException.Message } else { $_.Exception.Message }
    Write-Host "Proxy-ul n-a raspuns in 5 secunde sau a inchis conexiunea ($cauza)."
    exit 1
} finally {
    $client.Close()
}
