$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$signingDir = Join-Path $repoRoot "signing"
$keystorePath = Join-Path $signingDir "granit-release.jks"
$secretsPath = Join-Path $repoRoot "ANDROID_RELEASE_SECRETS.txt"

if (Test-Path $keystorePath) {
    throw "Keystore already exists at $keystorePath. Do not overwrite it: the same key must sign every future update."
}

New-Item -ItemType Directory -Force -Path $signingDir | Out-Null

function New-RandomPassword {
    param([int]$ByteCount = 32)
    $bytes = New-Object byte[] $ByteCount
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$keytool = Get-Command keytool.exe -ErrorAction SilentlyContinue
if (-not $keytool) { $keytool = Get-Command keytool -ErrorAction SilentlyContinue }
if (-not $keytool) { throw "Java JDK 17 is required. Install a JDK and ensure keytool is in PATH." }

$storePassword = New-RandomPassword
$keyPassword = New-RandomPassword
$keyAlias = "granit-release"

& $keytool.Source -genkeypair -noprompt -v `
    -keystore $keystorePath `
    -storetype JKS `
    -storepass $storePassword `
    -keypass $keyPassword `
    -alias $keyAlias `
    -keyalg RSA `
    -keysize 3072 `
    -validity 10000 `
    -dname "CN=Granit Survival, OU=Android Release, O=Granit"
if ($LASTEXITCODE -ne 0) { throw "keytool failed to create the release keystore." }

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
$secretLines = @(
    "ANDROID_KEYSTORE_BASE64=$keystoreBase64"
    "ANDROID_KEYSTORE_PASSWORD=$storePassword"
    "ANDROID_KEY_ALIAS=$keyAlias"
    "ANDROID_KEY_PASSWORD=$keyPassword"
)
[IO.File]::WriteAllLines($secretsPath, $secretLines, [Text.Encoding]::ASCII)

Write-Host ""
Write-Host "Release keystore created. Keep BOTH files private:"
Write-Host "  $keystorePath"
Write-Host "  $secretsPath"
Write-Host ""
Write-Host "Open $secretsPath and copy each value into the matching GitHub Actions repository secret."
Write-Host "Never commit, upload publicly, or share these files. Keep an encrypted backup of the .jks file."
