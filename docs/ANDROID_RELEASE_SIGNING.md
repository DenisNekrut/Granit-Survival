# Android release signing

Every APK intended to update an installed Granit Survival release must use the same application ID and the same private signing key. The release key is permanent: losing it prevents future in-place updates for installations signed with it.

## 1. Create the release key on a trusted Windows PC

Install a Java JDK 17 (the Android build already uses JDK 17), open PowerShell in the repository folder, and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-android-signing-key.ps1
```

The script creates:

- `signing/granit-release.jks` — private signing keystore
- `ANDROID_RELEASE_SECRETS.txt` — four values to enter in GitHub

Both paths are ignored by Git. Keep an encrypted backup of the JKS file in a separate secure location. Never commit or publish either file.

## 2. Add the four GitHub Actions secrets

Open the repository on GitHub:

**Settings → Secrets and variables → Actions → New repository secret**

Create each secret using the exact name below and copy its value from `ANDROID_RELEASE_SECRETS.txt`:

| Secret name | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `granit-release` |
| `ANDROID_KEY_PASSWORD` | Private-key password |

Do not put these values in source files, issues, pull requests, or chat messages. After all four secrets are saved, securely delete the plaintext `ANDROID_RELEASE_SECRETS.txt` file after confirming the secrets are configured. Keep the JKS keystore and its encrypted backup.

## 3. Publish an update

The workflow `.github/workflows/android-release.yml` runs for a pushed tag beginning with `v`, builds a signed release APK, and attaches it to a GitHub Release. For example:

```bash
git tag v0.1.1
git push origin v0.1.1
```

The version code must increase with every release. The release workflow derives it from the GitHub Actions run number.

## Important: existing debug installations

A release-signed APK cannot update an installation signed with a different debug key. The first transition from a debug build to the permanent release key may require a one-time reinstall. Before doing that, export or back up local save data; do not uninstall the current app until a save migration/backup route has been verified.
