$ErrorActionPreference = "Continue"
$env:ANDROID_HOME = "C:\Users\thati\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$g = "C:\Users\thati\gradle-9.6.0\bin\gradle.bat"
Set-Location "C:\Users\thati\jiit-kiosk-recon\KioskApp"

# Maven Central DNS is intermittent; each run caches more of junit/hamcrest.
$MAX = 10
for ($i = 1; $i -le $MAX; $i++) {
    Write-Host "===== TEST PASS $i / $MAX ====="
    $out = & $g testDebugUnitTest --no-daemon --console=plain 2>&1
    $out | Select-Object -Last 35
    $joined = $out -join "`n"
    if ($joined -match "BUILD SUCCESSFUL") {
        Write-Host "===== TESTS PASSED on pass $i ====="
        exit 0
    }
    if ($joined -match "FAILED|Unresolved|error:" -and $joined -notmatch "Could not GET|No such host|Could not download|Could not resolve|Could not transform") {
        Write-Host "===== REAL FAILURE (not network) on pass $i ====="
        exit 1
    }
    Start-Sleep -Seconds 3
}
Write-Host "===== EXHAUSTED $MAX PASSES ====="
exit 1
