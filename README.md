# Everfold Android

## Build

### Nix Develop Shell

```bash
# Nix Shell
nix develop

# OR Direnv
apt install -y direnv
echo 'use flake' > .envrc
direnv allow

# Update
nix flake update
```

### Program

```bash
# Build Debug
./gradlew :app:assembleDebug

# Build Release
./gradlew :app:assembleRelease

# Unit Test
./gradlew :app:testDebugUnitTest

# Lint
./gradlew :app:lintDebug

# Check
./gradlew check

# Format Check
./gradlew spotlessCheck

# Format
./gradlew spotlessApply

# Update
./gradlew dependencyUpdates
```

### Android

```bash
# Create Android Emulator
avdmanager create avd --name everfold --package 'system-images;android-37.2;google_apis;x86_64' --device pixel_10_pro

# Run in Android Emulator
emulator -avd everfold -gpu host

# Install Debug APK to Device
./gradlew :app:installDebug

# Test on Device
./gradlew :app:connectedDebugAndroidTest
```
