# ER SK GAUR Presentation Remote

Android Bluetooth remote for Microsoft PowerPoint on Windows 10.

## What is included
- Android app source (Kotlin / Android Studio project)
- Windows companion receiver source (.NET 8, classic Bluetooth RFCOMM)
- GitHub Actions workflows to build an Android debug APK and a Windows self-contained ZIP
- Next/Previous, Start/End show, Black screen, volume controls, reconnect loop
- Experimental current-slide preview: receiver exports the active slide to PNG and sends it to Android

## Important
This is the first development build, not a tested release. Bluetooth compatibility depends on the laptop Bluetooth adapter/driver and Windows pairing. The Windows receiver expects classic Bluetooth RFCOMM and PowerPoint desktop to be installed. The slide preview is a best-effort feature and should be validated on your PowerPoint version.

## Create your separate GitHub repository
1. Open https://github.com/new while signed in.
2. Repository name: `er-sk-gaur-presentation-remote`
3. Choose Public or Private, and check **Add a README file** (or leave it unchecked; both are fine).
4. Create the repository.
5. On the repository page, choose **Add file → Upload files**.
6. Upload the contents of this ZIP (upload the files/folders inside the extracted folder, not the ZIP itself).
7. Commit to `main`. GitHub Actions will start building the Android APK and Windows receiver.

## Download the build outputs
1. Open the repository's **Actions** tab.
2. Open the latest successful workflow run.
3. Download `er-sk-gaur-android-apk` for the APK.
4. Download `er-sk-gaur-windows-receiver` for the Windows companion ZIP.

## Install and test
### Windows 10
1. Extract the Windows artifact ZIP.
2. Run `ERSKGaurRemoteReceiver.exe`.
3. Allow Bluetooth access if Windows asks. Pair your phone in **Settings → Devices → Bluetooth & other devices** first.
4. Keep the receiver running. It waits for an RFCOMM connection.

### Android
1. Install the generated debug APK (you may need to allow installing apps from your browser/files app).
2. Grant Nearby devices/Bluetooth permissions.
3. Tap **Connect / Reconnect** and choose the paired Windows laptop.
4. Open PowerPoint on the laptop. Use **Start** to begin the slide show, then test Next/Previous.
5. Test Black screen, End, and volume controls. Preview requests the active slide image when a show is running.

## Troubleshooting
- Ensure Bluetooth is enabled on both devices and pair them in Android/Windows settings first.
- If the app does not see the laptop, make sure the receiver is running and that Windows Bluetooth supports classic RFCOMM.
- If PowerPoint commands fail, open the desktop PowerPoint app and a presentation first.
- Do not use this first build for a critical presentation until you have tested it on your exact devices.
