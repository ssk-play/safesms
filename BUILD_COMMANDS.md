# Build Commands

Quick reference for building Kids SMS.

## Debug Build (Development)

```bash
# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Release Build (Production)

### Build AAB (for Google Play Store)

```bash
# Build release bundle
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### Build APK (for direct distribution)

```bash
# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

## Clean Build

```bash
# Clean all build artifacts
./gradlew clean

# Clean + build
./gradlew clean bundleRelease
```

## Verify Signing

```bash
# Check AAB signing
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab

# Check APK signing
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk
```

## Install on Device

```bash
# Install debug APK
./gradlew installDebug

# Or use adb directly
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Run Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Version Management

Update version in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 1      // Increment for each release
    versionName = "1.0"  // User-facing version
}
```

## Common Issues

### "Task :app:signReleaseBundle FAILED"
- Check that `app/key.properties` exists and has correct values
- Verify keystore file path in `storeFile` property
- Confirm passwords are correct

### "Failed to read key from keystore"
- Wrong `keyAlias` or `keyPassword`
- Check with: `keytool -list -v -keystore /path/to/keystore.jks`

### "Unsigned APK"
- For release builds, ensure `key.properties` is configured
- For debug builds, this is normal (uses debug keystore)

## Build Output Sizes

Typical sizes for Kids SMS:
- Debug APK: ~4-6 MB
- Release APK: ~3-5 MB (smaller due to optimization)
- Release AAB: ~3-4 MB (Play Store uses this)

## Next Steps

After building:
1. **AAB**: Upload to Google Play Console
2. **APK**: Test on physical devices or distribute directly
3. **Verify**: Install and test all features before publishing
