# Releasing to Google Play

The app ID is `com.stevenmcgrew.vehiclemaintenancetrackerapp`. It cannot change
after the first upload. Debug builds add `.debug`, so they install beside a
release build.

## 1. Create the upload key (once)

Play App Signing holds the real app signing key. You sign uploads with an
**upload key** that you create and keep. Make it outside this repository:

```sh
/opt/android-studio/jbr/bin/keytool -genkeypair -v \
  -keystore ~/keys/vehicle-maintenance-upload.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

keytool asks for the passwords and your name. Back up the `.jks` file and both
passwords somewhere safe, such as a password manager. If you lose the upload key,
Play support can reset it, but that takes time.

## 2. Point the build at it

Create `keystore.properties` in the project root. Git ignores it, so it is never
committed:

```properties
storeFile=/home/you/keys/vehicle-maintenance-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

`storeFile` can be absolute or relative to the project root. Without this file,
release builds still succeed, but they are unsigned and Play rejects them.

## 3. Build the bundle

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew bundleRelease
```

The bundle is `app/build/outputs/bundle/release/app-release.aab`. To check that it
is signed with your upload key:

```sh
/opt/android-studio/jbr/bin/jarsigner -verify -certs app/build/outputs/bundle/release/app-release.aab
```

Release builds are optimized with R8. Before each upload, install the matching APK
(`./gradlew assembleRelease`, then `adb install app/build/outputs/apk/release/app-release.apk`)
and try the main flows once.

The emulator may already have a release build signed with a throwaway test key
from feature 10's testing. It holds only sample data. Uninstall it
(`adb uninstall com.stevenmcgrew.vehiclemaintenancetrackerapp`) before installing a
build signed with your real key, or the install fails with a signature mismatch.

## 4. Before the first submission

- [ ] Replace `[CONTACT EMAIL]` in `play-store/privacy-policy.html` (two places on
      one line)
- [ ] Host that page at a public URL that will not move, such as GitHub Pages or
      your own site, and open it in a browser to check it
- [ ] Bump `versionCode` in `app/build.gradle.kts` for every later upload.
      `versionName` is what users see.

## 5. Play Console

1. Create the app: name `Vehicle Maintenance Tracker`, App, Free.
2. Accept Play App Signing when you upload the first bundle. Google manages the app
   signing key.
3. **App content**:
   - Privacy policy: the hosted URL
   - Ads: No
   - App access: all functionality is available without special access
   - Content rating: complete the questionnaire. It has no user interaction,
     sharing, location, or purchases.
   - Target audience: adults (18+), or 13+ if you prefer. It is not designed for
     children.
   - Data safety: follow `play-store/data-safety.md`
   - Government app, financial features, health: No
4. **Main store listing**: copy from `play-store/listing.md` and upload the files in
   `play-store/graphics/`.
5. **Testing**: upload `app-release.aab` to Internal testing first and install it
   from the Play link. New personal developer accounts must also run a closed test
   with a minimum number of testers for a set period before production access.
   The Console shows the current rule under Dashboard.
6. Promote the release to Production when testing and the Console checklist are
   complete.
