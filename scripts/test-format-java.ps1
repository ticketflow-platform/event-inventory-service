$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Invoke-Formatter([string] $Path, [string] $Source) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = 'powershell.exe'
    $startInfo.Arguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "' + (Join-Path $PSScriptRoot 'format-java.ps1') + '" "' + $Path + '"'
    $startInfo.WorkingDirectory = $root
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
        $bytes = $utf8.GetBytes($Source)
        $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
        $process.StandardInput.Close()
        $process.WaitForExit()
        return @{
            ExitCode = $process.ExitCode
            Output = $output.GetAwaiter().GetResult()
            Errors = $errors.GetAwaiter().GetResult()
        }
    } finally {
        $process.Dispose()
    }
}

# Run gradle spotlessApply first: the on-disk file is the reference Gradle result.
$path = Join-Path $root 'src/test/java/scenario/cases/java/controller/GlobalExceptionHandlerTestCases.java'
$source = [IO.File]::ReadAllText($path, $utf8)
$hash = (Get-FileHash $path).Hash
$clean = Invoke-Formatter $path $source
if ($clean.ExitCode -ne 0 -or $clean.Output -cne $source) {
    throw "Already formatted input changed or failed: $($clean.Errors)"
}

$dirtySource = $source.Replace('public final class', 'public   final   class')
if ($dirtySource -ceq $source) {
    throw 'The test fixture no longer contains the expected class declaration.'
}
$dirty = Invoke-Formatter $path $dirtySource
if ($dirty.ExitCode -ne 0 -or $dirty.Output -cne $source) {
    throw "Unsaved input did not produce the Gradle result (including UTF-8 text): $($dirty.Errors)"
}
if ((Get-FileHash $path).Hash -cne $hash) {
    throw 'Formatting the editor buffer changed the on-disk file.'
}

$unmatched = Invoke-Formatter (Join-Path $root 'build/OutsideSourceSet.java') $source
if ($unmatched.ExitCode -eq 0 -or $unmatched.Output.Length -ne 0) {
    throw 'An unsupported buffer must fail without emitting replacement text.'
}
Write-Output 'PASS: unchanged input, unsaved input, UTF-8, disk preservation, unsupported buffer.'
