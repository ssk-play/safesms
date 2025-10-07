# App Signing Setup

This guide explains how to set up app signing for Kids SMS.

## 1. Generate Keystore (First Time Only)

Generate a new keystore file for signing your app:

```bash
keytool -genkey -v -keystore kidssms-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias kidssms-key
```

You will be prompted to enter:
- Keystore password (remember this!)
- Key password (remember this!)
- Your name, organization, city, state, country

**IMPORTANT**:
- Keep this keystore file safe - you cannot update your app without it
- Store the passwords securely (password manager recommended)
- Back up the keystore file to a secure location

## 2. Create key.properties File

Copy the template and fill in your values:

```bash
cp key.properties.template app/key.properties
```

Edit `app/key.properties` with your actual values:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=kidssms-key
storeFile=/absolute/path/to/kidssms-release-key.jks
```

**Notes**:
- `key.properties` file should be in `app/` directory
- `storeFile` should be an absolute path
- Example: `storeFile=/Users/yourname/keystores/kidssms-release-key.jks`

## 3. Build Signed APK/AAB

### Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Build Release AAB (for Play Store)

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## 4. Verify Signing

Check if APK/AAB is properly signed:

```bash
# For APK
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# For AAB (use bundletool)
java -jar bundletool.jar validate --bundle=app/build/outputs/bundle/release/app-release.aab
```

## 5. Security Best Practices

✅ **DO**:
- Keep keystore file in a secure location
- Use strong passwords (16+ characters)
- Store passwords in a password manager
- Back up keystore to multiple secure locations
- Add `key.properties` to `.gitignore` (already done)

❌ **DON'T**:
- Commit keystore or key.properties to git
- Share keystore file or passwords
- Use weak passwords
- Store passwords in plain text

## Troubleshooting

### "key.properties not found"

The build will still work but won't sign the APK. Create `key.properties` file as described above.

### "Could not read keystore"

Check that `storeFile` path in `key.properties` is correct.

### "Wrong password"

Verify `storePassword` and `keyPassword` in `key.properties` match the keystore.

### "KeyAlias not found"

Check that `keyAlias` in `key.properties` matches the alias used when creating keystore.

## Google Play App Signing

If using Google Play App Signing:
1. Upload your AAB to Play Console
2. Google will manage the signing key
3. You keep the upload key for future updates

Learn more: https://developer.android.com/studio/publish/app-signing

## Key Information

- **App ID**: ssk.kidssms
- **Recommended Key Alias**: kidssms-key
- **Keystore Type**: JKS
- **Key Algorithm**: RSA 2048-bit
- **Validity**: 10000 days (~27 years)
