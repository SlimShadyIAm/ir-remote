# IR Remote

Native Android 8.0+ app and a reusable Java Android IR SDK. Create named buttons with a custom carrier frequency in **Hz** and a raw alternating ON/OFF pattern in **microseconds**, beginning with ON. Saved buttons can be sent, edited, or deleted and persist locally across app restarts. No Internet permission, account, or remote service is needed.

Touch and hold **Send** to repeat the saved signal, with a 100 ms gap after each completed transmission. Release, slide outside the button, or leave the app to stop. A signal already handed to Android finishes before stopping; transmissions never overlap or build up in a queue. A normal tap sends once. Accessibility/keyboard clicks also send once. Repeating resends the full saved pattern; some appliance protocols need a distinct repeat frame, which is not generated automatically.

## OnePlus compatibility

The app checks Android's `ConsumerIrManager` at runtime and displays the emitter status and reported carrier ranges. It works on the OnePlus Nord 6 **if its firmware exposes the IR blaster through this public API**. Hardware compatibility has not been verified on a physical Nord 6. If firmware hides the emitter, this app cannot enable it. No root or private OnePlus API is used.

A frequency alone is insufficient: enter the pulse timings for the appliance and command you want to send. This is a raw signal editor, not a built-in code database or IR-learning receiver. Obtain timings from documented remote protocols or an external IR receiver. All durations must be positive integers, with at most 10,000 durations and a total shorter than two seconds. Unsupported carrier frequencies are rejected when the phone reports ranges. A successful send means Android accepted the transmission; IR provides no acknowledgment from the appliance.

## Build and validate

Install JDK 17 (21 also works), Android SDK platform 35, and build-tools 35.0.0. Set `ANDROID_HOME` to your SDK directory, or set `sdk.dir` in ignored `local.properties`. The checked-in Gradle 8.13 wrapper verifies its distribution checksum.

```sh
./gradlew test lint assembleDebug assembleRelease \
  :ir-sdk:publishReleasePublicationToLocalReleaseRepository
```

- Installable development APK: `app/build/outputs/apk/debug/app-debug.apk`
- Unsigned production APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- SDK library: `ir-sdk/build/outputs/aar/ir-sdk-release.aar`
- Maven repository including sources and POM: `ir-sdk/build/maven/`

Install the debug APK using `adb install -r app/build/outputs/apk/debug/app-debug.apk`. On the phone, create a button with known timings, restart the app to verify persistence, point the blaster at the appliance, then tap Send. Check editing, deletion, out-of-range frequencies, and behavior on devices without IR. Physical transmission and UI behavior require device/emulator validation; host unit tests cover signal parsing, bounds, and defensive copies.

For a production APK, use Android SDK `zipalign`, then `apksigner sign` with your own stable signing key, and verify using `apksigner verify`. Never commit your signing key or passwords. Debug builds are development-signed and CI keys may differ between runs; replacing one may require uninstalling it, which deletes saved controls.

## SDK usage

The SDK is distributed through GitHub Release assets, including a zipped Maven repository; it is not published to Maven Central or GitHub Packages. Add the downloaded AAR as a local Android dependency, or extract the Maven ZIP and add that directory as a Maven repository. Coordinates: `com.slimshady:ir-sdk:1.0.0` (or the release tag's version).

```java
IrTransmitter ir = new IrTransmitter(context);
if (ir.isAvailable()) {
    IrSignal signal = IrSignal.parse("38000", "9000, 4500, 560, 560");
    // Example timings only, not a complete appliance command.
    ir.transmit(signal); // Run on a worker thread; transmission can block.
}
```

The AAR manifest declares `android.permission.TRANSMIT_IR`, a normal install-time permission. Consumers should check hardware availability and catch hardware/argument errors. The app marks IR hardware optional so configuration remains possible without an emitter.

## GitHub Actions and publishing

Pull requests and pushes to `main` run tests, Android lint, both APK builds, and SDK Maven staging, then upload downloadable artifacts and validation reports. Pushing a version tag publishes those files to a GitHub Release using the repository's built-in `GITHUB_TOKEN`:

```sh
git tag v1.0.0
git push origin v1.0.0
```

Tags must be `vMAJOR.MINOR.PATCH` with an optional prerelease suffix. Release assets include the development-signed APK, unsigned production APK, AAR, Maven ZIP, and SHA-256 checksums. Only the tag release job receives `contents: write`. This repository must first be pushed to GitHub with Actions enabled. Creating these files locally does not run Actions or publish a release.
