param(
    [Parameter(Mandatory = $true)]
    [string] $BufferPath
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$source = [Console]::In.ReadToEnd()

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = 'cmd.exe'
$startInfo.Arguments = '/d /c gradle spotlessApply --quiet --console=plain "-PspotlessIdeHook=' + $BufferPath + '" -PspotlessIdeHookUseStdIn -PspotlessIdeHookUseStdOut'
$startInfo.WorkingDirectory = Split-Path $PSScriptRoot -Parent
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.StandardOutputEncoding = $utf8
$startInfo.StandardErrorEncoding = $utf8

$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
try {
    [void] $process.Start()
    $output = $process.StandardOutput.ReadToEndAsync()
    $errors = $process.StandardError.ReadToEndAsync()
    $bytes = $utf8.GetBytes($source)
    $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $formatted = $output.GetAwaiter().GetResult()
    $diagnostics = $errors.GetAwaiter().GetResult()

    if ($process.ExitCode -ne 0) {
        [Console]::Error.Write($diagnostics)
        exit $process.ExitCode
    }

    # Spotless emits no stdout when the input is already formatted.
    if ($diagnostics -match '(?m)^IS CLEAN\r?$') {
        [Console]::Out.Write($source)
    } elseif ($diagnostics -match '(?m)^IS DIRTY\r?$') {
        [Console]::Out.Write($formatted)
    } else {
        [Console]::Error.WriteLine('Spotless could not format this buffer. Save it as a Java file under src first.')
        [Console]::Error.Write($diagnostics)
        exit 1
    }
} finally {
    $process.Dispose()
}
