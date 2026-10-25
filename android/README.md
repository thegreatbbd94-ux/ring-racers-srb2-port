# Ring Racers Android

Android port scaffold for the official Ring Racers `v2.4` source. The app targets
64-bit Android devices (API 28 or newer) and uses SDL2 with the engine's existing
GLES2 renderer.

## Fresh data

The APK contains only core runtime assets from the neighboring Ring Racers 2.4
installation. It does not package `ringdata.dat`, `ringprofiles.prf`,
`ringconfig.cfg`, backups, replays, logs, addons, passwords, or server data.

On first launch, core assets are extracted to the app-private `game` directory.

## Saves and addons

On first launch the app asks for storage access (on Android 11+ this opens the
"All files access" settings page; turn it on and go back to the game). With
access granted, profiles, settings, progress, and replays live in
`Internal storage/RingRacers`, and mods go in `Internal storage/RingRacers/addons`.
These folders are visible in any file manager and survive uninstalling the app.

If you choose "Continue without", the game uses the app-specific folder
`Android/data/org.kartkrew.ringracers/files/RingRacers` instead, which many file
managers on Android 11+ cannot open. You can grant access later from the app's
settings and your data is copied over on the next launch.

Saves from older builds (`files/user/.ringracers`) are copied over automatically.

## Touch controls

- D-pad: steer; up/down also aim items and choose trick directions
- `GO` (`A`): accelerate and menu accept
- `LOOK` (`B`): look backward
- `SPIN` (`C`): spindash, fastfall, and menu extra action
- `BRAKE` (`X`): brake and menu back
- `BAIL` (`Y`): Ring Bail
- `VOTE` (`Z`): multiplayer vote action
- `ITEM` (`L`): use items and Rings
- `DRIFT` (`R`): drift
- Pause icon: Start/pause

The overlay supports simultaneous fingers. SDL's normal USB and Bluetooth
controller handling remains enabled.

## Build

Required local tools:

- JDK 17
- Android SDK 34, Build Tools 34.0.0
- Android NDK 26.3.11579264 and CMake 3.22.1
- vcpkg at `C:/vcpkg` with the `arm64-android` dependencies installed

From this directory:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Without vcpkg (for example on Linux), build static arm64 copies of zlib, libpng,
Opus, SDL2, OpenSSL and curl into one install prefix and pass it to Gradle:

```sh
java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
    assembleDebug -PringRacersDepsPrefix=/opt/rrdeps/arm64
```
