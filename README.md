# Card Vault

PIN-protected Android app for storing your own card details on-device.

## Security model

- Vault data never leaves the phone unless you export a backup.
- Card records are encrypted with AES-256-GCM.
- The data key is wrapped with a key derived from your PIN (PBKDF2-HMAC-SHA256, 210,000 iterations).
- Android backup/auto-transfer of app data is disabled. Move phones with an encrypted `.cvault` export.
- Screenshots of the app window are blocked (`FLAG_SECURE`).
- Copied card numbers/CVVs are cleared from the clipboard after 30 seconds.
- The vault auto-locks about 45 seconds after the app goes to the background.

## Search

On the home list, use **Search by nickname**. Matching is case-insensitive and matches part of the name (for example `hdfc` finds `HDFC Millennia`).

## Credit vs debit dates

When you add a card, choose **Credit card** or **Debit card**.

- Credit cards can store a **bill generation date** and a **due date**.
- Debit cards leave those dates blank; switching to debit clears them.

## Phone change / sync

1. Open **Settings**.
2. Choose a backup password (8+ characters, separate from your PIN).
3. Export the encrypted backup and save the `.cvault` file to Drive, USB, or another device you control.
4. Install Card Vault on the new phone, tap **Restore from backup**, pick the file, enter the backup password, and set a PIN for the new device.

There is no cloud account. The backup file is useless without the backup password.

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew test assembleRelease
```

The signed APK is `app/build/outputs/apk/release/app-release.apk`.

Install: enable **Install unknown apps** for your file manager, then open the APK. This is a personal sideload build, not a Play Store listing.

Use this only for cards you own. Do not store other people's payment data.
