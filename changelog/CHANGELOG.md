2.5.4
- Adapted for Android 17
- Added app info export in Settings. Exported files can now be opened in WebUI¹ and viewed visually
- Added a detail page for Overlay apps
- Added support for detecting Modern Xposed API module information
- Moved DEX optimization information from the app properties dialog to the install source dialog. More accurate results are now shown for users with Shizuku granted
- For native libraries that already support 16 KB page alignment but are not ZIP-aligned to 16 KB, the specific ZIP alignment value is now shown
- Added new detection labels to the native library list
- Added detection for stripped symbol tables on the native library detail page
- Improved the speed and accuracy of app signing scheme detection
- Fixed an issue where alternative icons or themed icons could not be loaded correctly in some cases
- Fixed incomplete Split APKs list detection for some apps
- Fixed an issue where the update date disappeared on the library detail page after switching languages
- Fixed an issue where the app list on the home page might not refresh in some cases
- Fixed false positives in Jetpack Compose detection for some apps
- Fixed an issue where the bottom sheet might fail to show on wide-screen devices while staying in landscape orientation
- Optimized the UI layout of advanced settings for the app list
- Updated several UI designs
- Optimized accessibility
- Updated rules database to version 44

¹ You can now visit [WebUI](https://lc.absinthe.life/) on any device to analyze and compare Android app packages. All analysis is performed locally.

## 2.5.3
- Now supports sharing APK download links to LibChecker, displaying most basic information by downloading a small amount of metadata, especially suitable for oversized apps or games
- Supported comparing APKS files
- Supported displaying the permission provider on the permission details page
- Supported detecting app themed and alternative launch icons
- Supported detecting "Live Update Notifications"
- Supported sharing apps to other apps or the "Files" app
- Supported extracting specific native libraries to the Download directory
- Supported direct navigation to more file managers in "Further options" on the App Details page
- The status indicator color on the end of snapshot items is now displayed according to the specific ratio
- Added size change ratio for native library types in the snapshot list and snapshot details page
- Mirror links are now selected for some in-app links based on the locale
- Action types in Library References no longer filter the android namespace
- Non-standard ELF files are no longer included in 16 KB detection
- The filter in Library Reference Statistics now enables Action type statistics by default
- Fixed an issue where snapshots for some apps could not be saved
- Fixed an issue where the Library Reference Statistics page failed to navigate to the correct app details page in specific cases
- Fixed an issue where snapshot backups failed in edge cases
- Removed some telemetry events related to app launch statistics in the non-FOSS version
- Optimized UI display and fixed some issues
- Updated Rules to V43

## 2.5.2
- Now supports identifying and marking libraries via the `action` tag in a component’s IntentFilter, with lower display priority than class name matching
- Added an `Action` type in the advanced menu of the library reference statistics page
- Snapshots saved from this version will additionally record the system’s Build ID and security patch level, and any changes will be displayed on the snapshot dashboard
- Now the `Tint ABI Label` option also applies to the app details page.
- Added ability to long-press on the snapshot details page to copy the title bar information
- Added display of signing schemes in the app signature page
- Added recent app logs export in the Settings page
- In `Chart` - `Distribution`, now displays the device’s Android version (including upcoming minor versions)
- Fixed the detection method for native library 16 KB alignment
- Fixed issues where APK analysis failed under certain conditions
- Fixed missing library reference statistics data
- Fixed missing native libraries in some Split APKs
- Fixed crashes on wide-screen devices when switching to landscape mode in certain cases
- Fixed an issue where the `Tint ABI Label` option did not take effect in real time under specific conditions
- Fixed an issue where APK analysis could not handle multiple different apps consecutively on some devices
- Optimized an issue where search bar content was lost after page switching
- Other bug fixes and dependency updates

## 2.5.1
- Adapted for Android 16
- Supported analysis of APKs packages
- Supported detection of whether the app uses Compose Multiplatform technology
- Now supports viewing native library information for all architectures of an app
- Supports displaying 16 KB aligned apps in charts on Android 8+
- Native libraries with 16 KB alignment enabled, or uncompressed libraries that are not aligned to 16 KB in the ZIP package, will now be independently marked in the list on Android 8+. (When adapting an app to 16 KB, ensure all native libraries are labeled with ‘16 KB’ and that no native libraries are labeled with ‘NON 16 KB STORED’).
- Added an option for “Use IEC Units” for displaying file sizes in the app
- Added “Snapshot Auto-Remove” feature, now old snapshots can be automatically deleted
- Fixed the detection method for 16 KB alignment
- Fixed the display logic of the application component process indicator on the details page
- Updated to more modern Material Design page styles
- Supported recording basic information of “archived apps” on Android 15+
- Added reference links to feature detail popups
- Added installation time information to the installation source page
- Added supported store jump items to the “Further Options” section in the app details page
- Now no longer counts components starting with the Application ID in library reference statistics
- Fixed issues where snapshot backup and restoration could fail
- Native library information in the assets directory is no longer recorded in the snapshot
- Removed the “Dex” page from the app details page due to long-term lack of maintenance of Dex rules
- Migrated telemetry platform to Firebase
- Other fixes and optimizations
