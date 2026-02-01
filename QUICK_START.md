# FlexLauncher - Quick Start Guide

## 🚀 You're 5 Steps Away from a Working App!

### ⚡ Step 1: Open in Android Studio (2 minutes)

1. Download and extract the FlexLauncher folder
2. Open Android Studio
3. Click "Open an Existing Project"
4. Select the `FlexLauncher` folder
5. Wait for Gradle sync to complete

### ⚡ Step 2: Add Required Icons (5 minutes)

**Option A: Use Android Studio's Built-in Tool (Easiest)**
1. Right-click `res/drawable` folder
2. New → Vector Asset
3. Choose "Clip Art"
4. Search for and create:
   - `ic_launcher` (app icon - use any icon)
   - `ic_user` (person icon)
   - `ic_search` (magnifying glass)
   - `ic_apps` (grid icon)
   - `ic_phone` (phone icon)
   - `ic_message` (message icon)
   - `ic_browser` (globe icon)
   - `ic_camera` (camera icon)

**Option B: Quick Placeholder Icons**
Create a simple placeholder drawable in `res/drawable/ic_placeholder.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,12m-10,0a10,10 0,1 1,20 0a10,10 0,1 1,-20 0"/>
</vector>
```

Then copy it for all missing icons.

### ⚡ Step 3: Add Missing Resource Files (3 minutes)

**Create `res/values/styles.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FlexLauncher" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryDark">@color/primary_dark</item>
        <item name="colorAccent">@color/accent</item>
    </style>

    <style name="Theme.FlexLauncher.Setup" parent="Theme.FlexLauncher" />

    <style name="Theme.FlexLauncher.Transparent" parent="Theme.FlexLauncher">
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
    </style>
</resources>
```

**Create `res/drawable/badge_background.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/error"/>
</shape>
```

**Create folder `res/xml/` and add two files:**

`backup_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="."/>
    <include domain="database" path="."/>
</full-backup-content>
```

`data_extraction_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="."/>
    </cloud-backup>
</data-extraction-rules>
```

### ⚡ Step 4: Build the APK (1 minute)

1. In Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Wait for build to complete
3. Click "locate" in the notification
4. Find `app-debug.apk`

**Or use command line:**
```bash
./gradlew assembleDebug
```

### ⚡ Step 5: Install & Test (5 minutes)

**Install Shizuku First:**
1. Download Shizuku from Play Store or [GitHub](https://github.com/RikkaApps/Shizuku/releases)
2. Follow Shizuku setup instructions
3. Start Shizuku service

**Install FlexLauncher:**
1. Transfer APK to your phone
2. Install the APK
3. Press Home button
4. Select FlexLauncher
5. Choose "Always"

**First Run:**
1. Choose "Multi-User Mode" (or "Single User")
2. Grant Shizuku permission when prompted
3. Add 2-3 users if multi-user
4. Explore your new launcher!

---

## 🎯 What Works Right Now

✅ **Working Features:**
- Multi-user mode selection
- User creation with passwords
- Shizuku integration
- App loading and display
- Basic launcher home screen
- Gesture detection
- User profile display

⚠️ **Needs Completion:**
- User selection screen after lock
- Full app drawer
- App cloning UI
- Settings screens
- Theme customization UI

---

## 🐛 Troubleshooting

### Build Fails
```bash
# Clear cache and rebuild
./gradlew clean
./gradlew assembleDebug
```

### Missing Resources Error
- Check you created all the icon files
- Verify styles.xml exists
- Ensure xml/ folder has backup files

### Shizuku Not Working
- Make sure Shizuku app is installed
- Check Shizuku is running
- Verify ADB connection or root access
- Grant permission in Shizuku settings

### App Crashes on Launch
- Check Logcat for errors
- Verify all required permissions
- Ensure Room database initializes
- Check icons are present

---

## 📝 Development Checklist

### Immediate Todos:
- [ ] Add all icon drawables
- [ ] Create styles.xml
- [ ] Add XML resource files
- [ ] Build debug APK
- [ ] Test on device

### Next Phase:
- [ ] Create UserSelectionActivity
- [ ] Build app drawer
- [ ] Add app options menu
- [ ] Implement settings
- [ ] Add themes

### Advanced:
- [ ] Widget support
- [ ] Notification badges
- [ ] Backup/restore
- [ ] Icon packs
- [ ] Performance optimization

---

## 💡 Pro Tips

1. **Use Real Device**: Launcher features work best on real hardware
2. **Enable USB Debugging**: Essential for ADB and Shizuku
3. **Test Gestures**: Make sure touch detection works on your device
4. **Check Logcat**: Use `adb logcat` to see errors
5. **Incremental Development**: Complete one feature at a time

---

## 📚 Documentation

- **README.md**: Complete feature documentation
- **BUILD_GUIDE.md**: Detailed build instructions
- **FEATURES.md**: All 115 features explained
- **PROJECT_SUMMARY.md**: Project overview

---

## 🆘 Need Help?

### Common Issues:

**"Cannot resolve symbol R"**
- Solution: Build → Clean Project → Rebuild

**"Gradle sync failed"**
- Solution: Check internet connection, sync again

**"App not installing"**
- Solution: Uninstall old version first

**"Shizuku permission denied"**
- Solution: Open Shizuku app, grant permission manually

---

## 🎉 Success!

Once you complete these 5 steps, you'll have a working Android launcher with:
- Multi-user support ✅
- Password protection ✅
- Shizuku integration ✅
- App management ✅
- Custom UI ✅

**Now go build something amazing!** 🚀

---

## 📞 Support

For questions or issues:
- Check the documentation files
- Review code comments
- Search Android developer docs
- Join Android development communities

**Happy coding!** 💻
