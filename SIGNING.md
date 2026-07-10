# App Signing Info — LalaStories / داستان سیندرلا و لالایی کودکانه

## Active Signing Key (used for CafeBazaar & Myket uploads)

| Field        | Value                                      |
|--------------|--------------------------------------------|
| File         | `/home/matin/.android/debug.keystore`      |
| Alias        | `androiddebugkey`                          |
| Store pass   | `android`                                  |
| Key pass     | `android`                                  |
| SHA-1        | `E7:F7:47:0E:E2:85:BA:82:5B:1B:CD:F3:7C:33:EB:22:70:9A:AE:24` |
| SHA-256      | `26:DA:71:AE:D6:42:98:13:D3:AD:23:5E:20:A3:17:DE:F1:88:6C:38:80:DD:27:03:AB:9E:29:3E:D3:40:A4:7E` |
| Created      | Jun 11, 2026                               |

This is Android Studio's default debug keystore. It was used to sign and upload
the very first version of the app to CafeBazaar, so all future updates must be
signed with this same key.

## ⚠️ Critical: Back This File Up

If `/home/matin/.android/debug.keystore` is ever lost or deleted, you will be
permanently locked out of updating the app on CafeBazaar/Myket. Back it up:

- Copy to a USB drive
- Upload to Google Drive / Telegram Saved Messages
- Store the file alongside this project in a safe location

## Discarded Key (DO NOT USE)

A separate release keystore was created on Jun 19, 2026 but was never accepted
by the stores because it doesn't match the originally published key.

| Field      | Value                                                           |
|------------|-----------------------------------------------------------------|
| File       | `app/lalastories-release.jks`                                   |
| Alias      | `lalastories`                                                   |
| SHA-1      | `BC:C5:79:E8:E0:EA:A2:2D:9E:68:F1:62:21:E8:01:74:C5:F5:BF:41` |

## Build Release APK

```bash
cd /home/matin/projects/Kid-stories
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Version History

| versionCode | versionName | Notes                        |
|-------------|-------------|------------------------------|
| 1           | 1.0         | First upload to CafeBazaar   |
| 2           | 1.1         | Added Snow White, Puss in Boots, app title update |
| 5           | 1.4         | Remote content (CDN manifest for stories/poems/lullabies), app title update |
