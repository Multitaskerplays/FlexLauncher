# FlexLauncher - Advanced Multi-User Android Launcher

## 🚀 Overview

FlexLauncher is a feature-rich Android launcher with multi-user support, app cloning via Shizuku, and extensive customization options. Inspired by Smart Launcher's design philosophy but built from scratch with unique features.

## ✨ Core Features

### 1. Multi-User System
- **Single User Mode**: Traditional launcher experience
- **Multi-User Mode**: Password-protected profiles for shared devices
- **User Switching**: Lock screen prompts for user selection
- **Profile Photos**: Visual user identification
- **Biometric Authentication**: Optional fingerprint/face unlock per user

### 2. App Cloning (via Shizuku)
- Clone apps for different users
- Each user gets their own instance of apps
- Automatic Play Store duplication
- System apps shared by default (configurable)
- No root required - uses Shizuku for elevated permissions

### 3. App Management
- **Smart Categorization**: Auto-organize apps into categories
- **App Hiding**: Hide apps per user profile
- **Favorites**: Quick access to frequently used apps
- **Custom Labels**: Rename apps per user
- **Usage Statistics**: Track app usage, launch count, recent apps

### 4. Customization

#### Themes (10+ built-in themes)
1. Default Material
2. Dark Mode
3. AMOLED Black
4. Light Minimalist
5. Gradient Flow
6. Nature Green
7. Ocean Blue
8. Sunset Orange
9. Purple Haze
10. Retro Neon

#### Icon Packs
- Support for third-party icon packs
- Custom icon selection per app
- Adaptive icon support
- Icon size adjustment

#### Grid Layouts
- Customizable rows and columns (3x3 to 8x8)
- Variable icon sizes
- Adjustable spacing
- Portrait/landscape optimization

### 5. Gestures & Controls

#### Swipe Gestures
- **Swipe Up**: App drawer / Search / Recent apps
- **Swipe Down**: Notifications / Quick settings
- **Swipe Left**: Widgets / Custom action
- **Swipe Right**: Widgets / Custom action
- **Double Tap**: Lock screen / Custom action
- **Pinch**: Zoom home screen
- **Two-finger swipe**: Quick settings

#### Custom Actions
- Open specific apps
- Toggle system settings
- Launch activities
- Execute shortcuts

### 6. Search & Discovery
- Universal search (apps, contacts, web)
- Search providers: Google, DuckDuckGo, Bing, Custom
- App suggestions based on:
  - Time of day
  - Location (with permission)
  - Usage patterns
  - Installed app relationships

### 7. Widgets
- Home screen widgets
- Widget pages
- Resizable widgets
- Custom widget grids

### 8. Folders
- Auto-categorized folders
- Custom folders
- Folder colors and icons
- Nested folder support
- Swipe-to-expand folders

### 9. Dock
- Customizable dock (3-7 icons)
- Dock backgrounds
- Floating dock option
- Hidden dock (swipe to reveal)

### 10. Backup & Restore
- Cloud backup (Google Drive)
- Local backup
- Export/import settings
- Scheduled auto-backups
- Multi-device sync

## 🎨 Extended Features (100+ Additional Features)

### Visual Customization (20 features)
11. Wallpaper blur effects
12. Parallax wallpapers
13. Live wallpaper support
14. Dynamic colors (Material You)
15. Custom accent colors
16. Gradient backgrounds
17. Icon shadows
18. App label styles (always/never/on-touch)
19. Badge customization
20. Transition animations (15+ types)
21. Page indicators
22. Clock widget styles
23. Weather widget
24. Calendar integration
25. Custom fonts
26. Icon shape customization
27. Transparent backgrounds
28. Color filters
29. Night mode scheduling
30. Seasonal themes

### App Organization (15 features)
31. Smart folders auto-creation
32. App drawer styles (vertical/horizontal/list/grid)
33. Sort apps by: name, color, size, install date, usage
34. App groups
35. Hidden apps drawer
36. Work profile integration
37. App recommendations
38. Suggested apps widget
39. App shortcuts support
40. Quick actions in app drawer
41. App search filters
42. Recent apps timeline
43. App usage insights
44. App size display
45. Batch app operations

### Performance & Battery (10 features)
46. Low memory mode
47. Animation speed control
48. Battery saver integration
49. Background limit control
50. Cache management
51. Aggressive doze mode
52. Wake lock control
53. Performance monitoring
54. RAM usage display
55. Storage optimization

### Security & Privacy (15 features)
56. App lock per app
57. Private space
58. Guest mode
59. Incognito mode
60. Hidden folder
61. Secure folder
62. App permissions manager
63. Privacy dashboard
64. Tracker blocker
65. Usage tracking disable
66. Secure password storage
67. Encrypted backups
68. Auto-lock timeout
69. Failed login attempts limit
70. Panic mode (quick switch to default profile)

### Productivity (20 features)
71. Quick notes widget
72. To-do list integration
73. Calendar widget
74. Reminders
75. Timer and stopwatch
76. Unit converter
77. Calculator widget
78. Screen recorder shortcut
79. Screenshot editor
80. Clipboard manager
81. QR code scanner
82. Flashlight toggle
83. Do Not Disturb scheduler
84. Focus mode
85. App usage limits
86. Screen time tracking
87. Break reminders
88. Notification management
89. Smart reply
90. Voice commands

### Smart Features (15 features)
91. Contextual app suggestions
92. Driving mode
93. Sleep mode
94. Meeting mode
95. Workout mode
96. Gaming mode
97. Reading mode
98. Location-based profiles
99. Time-based automation
100. Bluetooth device triggers
101. WiFi network triggers
102. Battery level triggers
103. Headphone connection triggers
104. NFC triggers
105. Gesture macros

### Accessibility (10 features)
106. Large text mode
107. High contrast mode
108. Color blind modes
109. Voice feedback
110. Haptic feedback levels
111. One-handed mode
112. Screen reader optimization
113. Magnification gestures
114. Switch access support
115. Accessibility shortcuts

## 📋 Requirements

### Minimum Requirements
- Android 8.0 (API 26) or higher
- 100MB free storage
- 2GB RAM recommended

### For Full Features
- **Shizuku**: Required for app cloning and advanced features
- **Storage Permission**: For backups and wallpapers
- **Notification Access**: For notification badges
- **Accessibility Service**: For gesture shortcuts (optional)

## 🔧 Installation & Setup

### Step 1: Install Shizuku
1. Download Shizuku from Play Store or [GitHub](https://github.com/RikkaApps/Shizuku)
2. Follow Shizuku's setup instructions
3. Start Shizuku service

### Step 2: Install FlexLauncher
1. Install the APK
2. Set as default launcher when prompted
3. Choose Single User or Multi-User mode
4. Grant Shizuku permission

### Step 3: Setup Users (Multi-User Mode)
1. Add users with names and passwords
2. Add profile photos (optional)
3. Assign apps to each user
4. Configure user-specific settings

### Step 4: Configure Apps
1. Open app drawer
2. Long-press apps to assign categories
3. Clone apps for multiple users if needed
4. Set favorites and customize

## 🛠️ Building from Source

### Prerequisites
```bash
# Install Android Studio
# Install Android SDK 34
# Install Kotlin 1.9.0+
```

### Build Instructions
```bash
# Clone repository
git clone [repository-url]
cd FlexLauncher

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to device
./gradlew installDebug
```

### Build Variants
- **debug**: Development build with logging
- **release**: Production build (requires signing)

## 📖 Usage Guide

### Multi-User Flow
1. **Lock Screen**: Shows user selection
2. **Select User**: Tap user profile
3. **Enter Password**: Type password or use biometric
4. **Access Profile**: See personalized home screen
5. **Switch Users**: Lock device or swipe to user switcher

### App Cloning
1. Long-press app in drawer
2. Select "Clone App"
3. Choose target user
4. Wait for clone to complete
5. Cloned app appears in user's app list

### Customization
1. Long-press home screen
2. Select "Settings"
3. Customize themes, grid, gestures
4. Apply changes

### Backup & Restore
1. Settings → Backup & Restore
2. Choose backup location (local/cloud)
3. Create backup
4. Restore on new device

## 🔐 Security Features

### Password Protection
- SHA-256 hashed passwords
- Salt per user
- No plain text storage
- Biometric fallback

### App Cloning Security
- Sandboxed app instances
- Separate data storage
- Independent permissions
- Encrypted clone metadata

### Privacy
- No telemetry
- No ads
- Local-first storage
- Optional cloud sync only

## 🐛 Troubleshooting

### Shizuku Not Working
- Ensure Shizuku is running
- Check permission in Shizuku manager
- Restart both Shizuku and FlexLauncher
- Re-grant permissions

### App Cloning Failed
- Verify Shizuku permission
- Check if app supports cloning
- Ensure sufficient storage
- Check Android version (8.0+)

### Profile Not Loading
- Clear app cache
- Verify password
- Check database integrity
- Reinstall if necessary

## 📱 Compatibility

### Tested Devices
- Google Pixel (all models)
- Samsung Galaxy S series
- OnePlus devices
- Xiaomi devices
- Most Android 8.0+ devices

### Known Issues
- Some OEMs restrict launcher features
- MIUI may require additional permissions
- OneUI may have gesture conflicts

## 🤝 Contributing

Contributions welcome! Areas needing help:
- Icon pack development
- Theme creation
- Translation
- Bug fixes
- Feature requests

## 📄 License

This project is provided as-is for educational purposes.

## 🙏 Credits

- **Shizuku**: For elevated permission framework
- **Material Design**: For UI components
- **Android Community**: For inspiration and support

## 📞 Support

For issues or questions:
- Open GitHub issue
- Check FAQ section
- Community forums

## 🗺️ Roadmap

### Version 1.1
- More themes
- Cloud sync
- Widget improvements

### Version 1.2
- AI-powered suggestions
- Enhanced gestures
- Performance optimizations

### Version 2.0
- Desktop mode
- Tablet optimization
- Foldable support

## ⚠️ Disclaimer

FlexLauncher requires Shizuku for advanced features. Shizuku requires ADB or root setup. Always use from trusted sources. This launcher is independent and not affiliated with Smart Launcher or any other launcher application.

---

**Made with ❤️ for the Android community**
