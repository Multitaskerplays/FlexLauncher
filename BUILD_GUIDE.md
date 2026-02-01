# FlexLauncher Build Guide

## Complete Setup and Build Instructions

### Prerequisites

#### 1. Install Required Software

**Android Studio** (Latest Version)
- Download from: https://developer.android.com/studio
- Install with default settings
- Include Android SDK, Platform Tools, and Build Tools

**Java Development Kit (JDK) 17**
- Included with Android Studio
- Or download from: https://www.oracle.com/java/technologies/downloads/

**Shizuku App** (For Testing)
- Download from Play Store: https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api
- Or GitHub: https://github.com/RikkaApps/Shizuku/releases

#### 2. Setup Android Studio

1. Open Android Studio
2. Configure SDK:
   - Go to Tools → SDK Manager
   - Install Android 8.0 (API 26) through Android 14 (API 34)
   - Install latest SDK Platform-Tools
   - Install latest Build Tools

3. Install Plugins:
   - Kotlin (should be included)
   - Android Gradle Plugin

### Building the Project

#### Method 1: Using Android Studio (Recommended for Beginners)

1. **Open Project**
   ```
   File → Open → Select FlexLauncher folder
   ```

2. **Wait for Gradle Sync**
   - Android Studio will automatically sync
   - This may take 5-10 minutes first time
   - Downloads all dependencies

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Find APK**
   - Location: `app/build/outputs/apk/debug/app-debug.apk`
   - Click "locate" in the build notification

5. **Install to Device**
   - Connect Android device via USB
   - Enable USB Debugging on device
   - Click Run button (green arrow) in Android Studio
   - Or: `adb install app-debug.apk`

#### Method 2: Using Command Line (Advanced)

**On Windows:**
```bash
cd FlexLauncher
gradlew.bat assembleDebug
```

**On Mac/Linux:**
```bash
cd FlexLauncher
chmod +x gradlew
./gradlew assembleDebug
```

**Output:**
```
app/build/outputs/apk/debug/app-debug.apk
```

### Building for Release

#### 1. Create Keystore

```bash
keytool -genkey -v -keystore flexlauncher.keystore -alias flexlauncher -keyalg RSA -keysize 2048 -validity 10000
```

Save the password securely!

#### 2. Configure Signing

Create `app/keystore.properties`:
```properties
storeFile=../flexlauncher.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=flexlauncher
keyPassword=YOUR_KEY_PASSWORD
```

#### 3. Update app/build.gradle

Add before `android` block:
```gradle
def keystorePropertiesFile = rootProject.file("app/keystore.properties")
def keystoreProperties = new Properties()
keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
```

Add inside `android` block:
```gradle
signingConfigs {
    release {
        keyAlias keystoreProperties['keyAlias']
        keyPassword keystoreProperties['keyPassword']
        storeFile file(keystoreProperties['storeFile'])
        storePassword keystoreProperties['storePassword']
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

#### 4. Build Release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Installing Shizuku for Testing

#### Option 1: ADB Wireless (No Root)

1. **Enable ADB on Device**
   - Settings → About Phone
   - Tap Build Number 7 times
   - Go back → Developer Options
   - Enable USB Debugging
   - Enable Wireless Debugging (Android 11+)

2. **Connect via ADB**
   ```bash
   # For Android 11+
   adb pair <IP>:<PORT>  # Use pairing code from device
   adb connect <IP>:<PORT>
   
   # For Android 10 and below
   adb tcpip 5555
   adb connect <DEVICE_IP>:5555
   ```

3. **Start Shizuku**
   - Open Shizuku app
   - Tap "Start via Wireless Debugging" or "Start via ADB"
   - Should show "Running"

#### Option 2: Root (If Available)

1. Open Shizuku app
2. Tap "Start via Root"
3. Grant root permission

### Testing the App

1. **Install FlexLauncher APK**
   ```bash
   adb install app-debug.apk
   ```

2. **Set as Default Launcher**
   - Press Home button
   - Select FlexLauncher
   - Choose "Always"

3. **Grant Shizuku Permission**
   - FlexLauncher will request Shizuku access
   - Approve in Shizuku manager

4. **Complete Setup**
   - Choose Single or Multi-User mode
   - Create user profiles if multi-user
   - Assign apps to categories

### Troubleshooting Build Issues

#### Gradle Sync Failed
```bash
# Clear cache
./gradlew clean
rm -rf .gradle/
rm -rf app/build/

# Sync again
./gradlew --refresh-dependencies
```

#### Dependency Resolution Error
- Check internet connection
- Verify repositories in `settings.gradle`
- Try VPN if in restricted region

#### Build Tools Not Found
- Open SDK Manager
- Install latest Build Tools
- Update `compileSdk` in `app/build.gradle`

#### Out of Memory
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

#### Kotlin Version Mismatch
Update in root `build.gradle`:
```gradle
ext.kotlin_version = '1.9.20'  // Latest stable
```

### Development Tips

#### Enable Hot Reload
Android Studio → Run → Apply Changes (Ctrl+F10)

#### View Logs
```bash
adb logcat | grep FlexLauncher
```

#### Debug on Device
- Run → Debug 'app'
- Set breakpoints in code
- Use Debug panel

#### Code Style
- Follow Kotlin conventions
- Use Android Studio formatter (Ctrl+Alt+L)
- Run lint: `./gradlew lint`

### Testing Features

#### Test Multi-User System
1. Setup mode → Multi-User
2. Add 2-3 test users
3. Lock device
4. Verify user selection screen appears
5. Test password authentication

#### Test App Cloning
1. Ensure Shizuku is running
2. Long-press any app
3. Select "Clone App"
4. Choose target user
5. Verify clone appears in user's app list

#### Test Gestures
1. Settings → Gestures
2. Configure swipe actions
3. Test each gesture
4. Verify actions execute

### Building for Different Android Versions

#### For Older Devices (Android 8-9)
Update in `app/build.gradle`:
```gradle
minSdk 26
targetSdk 28
```

#### For Latest Devices (Android 14)
```gradle
minSdk 26
targetSdk 34
```

### Performance Optimization

#### Reduce APK Size
```gradle
buildTypes {
    release {
        shrinkResources true
        minifyEnabled true
    }
}
```

#### Enable R8 Optimizer
In `gradle.properties`:
```properties
android.enableR8.fullMode=true
```

### Continuous Integration (CI/CD)

#### GitHub Actions Example

Create `.github/workflows/build.yml`:
```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

### Distribution

#### Google Play Store
1. Create Developer Account ($25 one-time)
2. Create app listing
3. Upload AAB (not APK):
   ```bash
   ./gradlew bundleRelease
   ```
4. Complete content rating, privacy policy
5. Submit for review

#### F-Droid (Open Source)
1. Fork F-Droid data repository
2. Add app metadata
3. Submit pull request
4. Wait for review and approval

#### Direct Distribution
1. Build signed APK
2. Host on website
3. Users must enable "Install from Unknown Sources"
4. Provide installation instructions

### Need Help?

- **Build Errors**: Check Android Studio Build panel
- **Runtime Errors**: Check Logcat
- **Gradle Issues**: Run with `--stacktrace` flag
- **Community**: Android development forums, Stack Overflow

### Next Steps

After successful build:
1. Test all features thoroughly
2. Fix any bugs found
3. Optimize performance
4. Add more features
5. Prepare for release

---

**Happy Building! 🚀**
