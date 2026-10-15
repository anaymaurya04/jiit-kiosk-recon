$ErrorActionPreference = "Continue"
$env:ANDROID_HOME = "C:\Users\thati\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$g = "C:\Users\thati\gradle-9.6.0\bin\gradle.bat"
Set-Location "C:\Users\thati\jiit-kiosk-recon\KioskApp"

# Maven Central DNS is intermittent here. Each Gradle run caches whatever it
# successfully downloads, so re-running eventually resolves everything. Stop on
# BUILD SUCCESSFUL or after MAX passes.
$MAX = 12
for ($i = 1; $i -le $MAX; $i++) {
    Write-Host "===== BUILD PASS $i / $MAX ====="
    $out = & $g assembleDebug --no-daemon --console=plain 2>&1
    $out | Select-Object -Last 40
    if (($out -join "`n") -match "BUILD SUCCESSFUL") {
        Write-Host "===== BUILD SUCCESSFUL on pass $i ====="
        exit 0
    }
    # If it's a compile error (not network), stop early — retrying won't help.
    if (($out -join "`n") -match "e: |error: |Unresolved reference|BUILD FAILED in \d+s" -and
        ($out -join "`n") -notmatch "Could not resolve|Could not HEAD|No such host|Could not get resource|connectionTimeout|timed out") {
        # Distinguish: if the ONLY failures are network-ish, keep going.
        Write-Host "non-network failure detected; one more check..."
    }
    Start-Sleep -Seconds 3
}
Write-Host "===== EXHAUSTED $MAX PASSES ====="
exit 1
