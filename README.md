# أوقات الصلاة: native Android app (Java)

A real Android app (no web view). Package `app.awqat.prayer`, minimum Android 8.0 (API 26).
It keeps the identity of the earlier web version (emerald and gold, prayer times for Morocco and Egypt, the sun element,
Qibla compass) and adds themes, Adhkar, Tasbeeh and a hadith library. The interface is Arabic, right to left, fully offline.

## How to get the APK
**GitHub (no installs):**
1. Create a new repository and upload everything in this folder, including the hidden `.github` folder.
2. Open the Actions tab, run "Build APK", wait about 5 to 10 minutes.
3. Download the artifact `prayer-times-apk`, unzip it, and install `app-debug.apk` on the phone.

**Android Studio:** open this folder, wait for the Gradle sync, then Build > Build APK(s). The file is at
`app/build/outputs/apk/debug/app-debug.apk`.

On the phone, allow installs from the app you opened the file with. The debug APK is fine for personal use; for Google Play,
build a release with your own signing key.

## What is implemented
- **Prayer times** for 26 Moroccan and 22 Egyptian cities or your location, with live countdown, date navigation, offsets,
  Ramadan option (Morocco only), Morocco's Ramadan clock and Egypt's summer time (from the system time zone data).
- **The sun element** (`SkyView`): a themed sky drawn with gradients that moves from sunrise to sunset, glow, drifting clouds,
  dunes, a moon and stars at night. About 8 frames per second, only while visible, static when system animations are off.
  It sits in the lower part of the hero card so it never covers text or buttons.
- **Themes**: 10 themes (الزمرد الملكي، ذهب منتصف الليل، أزرق المحيط، أبيض نقي، رمال الصحراء، البنفسج الملكي، أسود أموليد،
  الأخضر الإسلامي، شروق الشمس، غروب الشمس), each with a light and a dark palette; appearance can follow the system.
  Previews, saved choice, applied before the first frame, cross-fade on change. Add a theme by adding one line in `Themes.java`.
- **Hadith of the day** card on the home screen (full Arabic text, source, grade, copy, share, save, tap for details, another
  hadith; a new session shows a different hadith; can be switched off in settings).
- **Hadith and Adhkar library**: offline search (Arabic spelling and diacritics normalised), 12 categories, favourites, recents.
- **Morning and evening Adhkar**: per-item counters with prescribed counts, progress, previous/next, list, reset with confirmation,
  progress saved per day, resume from the first unfinished item, source and grade on every item.
- **Digital Tasbeeh**: the 20 phrases, large counter (compact size option), progress ring, targets 33 / 99 / 100 / custom,
  remaining count, target-reached indication, haptics (on/off), click sound (off by default), saved counts per phrase,
  session timer, history with dates, focus mode, reset confirmation. One touch counts exactly once; duplicate events are dropped.
- **Qibla compass** with the rotation sensor (or accelerometer + magnetometer), declination correction, haptic buzz when aligned.
- **Permissions**: location is requested on first launch and from the "موقعي" button. Android has no permission dialog for the
  compass, so the Qibla screen asks for your consent the first time.
- **Icon**: adaptive launcher icon (including the themed monochrome version) and 1024 / 512 px masters in `branding/`.

## Tests executed (`./run_tests.sh`, JDK 21, in the build environment)
891 checks passed, 0 failed:
- Java prayer maths equals the Python reference to the second on 10 city/date cases; prayer order is correct for every city in five months.
- Time zones: Morocco is UTC+0 in Ramadan 2026 and UTC+1 in October; Egypt is UTC+3 in mid-October and UTC+2 in January.
- Ramadan option changes Isha by 15 minutes in Morocco and has no effect in Egypt; Qibla bearings; next-prayer wrap after Isha.
- All 20 palettes: text contrast (ink 7:1, muted and accent 4.5:1, white on the hero gradient 4.5:1).
- Content validation: unique ids and texts, required fields, grade classes, categories; 20 distinct Tasbeeh phrases.
- Search normalisation, the daily-pick cycle (no consecutive repeats), counter/target/progress/history helpers, tap guard
  (100 rapid taps 80 ms apart all counted).

## Not tested (please read)
- **The Android screens have not been compiled against the Android SDK or run on a device.** The environment has no SDK or
  internet. All sources were checked with the JDK compiler for syntax (no syntax errors); the only compiler errors left are
  the missing Android classes. If the first build shows an error, send the log and it will be fixed.
- Not verified on real hardware: touch behaviour, compass accuracy, haptics, animations on mid-range phones, small screens,
  the launcher icon on different launchers, TalkBack.
- Reduced motion follows the system "remove animations" setting. No automated UI tests exist yet.

## Limitations and unfinished work
- **The hadith database has 51 records (39 Sahih, 12 Hasan), not the 250 required.** Each record has its source reference
  and the grading authority, but verifying 250 texts and gradings against printed editions was not possible here and nothing was
  invented to reach the number. Add records in `tools/gen_content.py` (it validates them), then run `./run_tests.sh`.
- **The texts are written without diacritics and have not been reviewed against printed editions.** Please have a qualified
  person review every hadith and remembrance (wording, numbers, grading) before public release. The Settings screen says so.
- Quran verses are shown without Uthmani diacritics; replace them with the standard mushaf text before release.
- Hadith numbers use the numbering of the standard editions only where named in the record; other editions may differ.
- A SQLite database was not used: about 70 records fit comfortably in generated Java arrays, which keeps the app small and fast.
  Move to SQLite if the library grows to hundreds of records.
- Data saved by the earlier web or Python versions is not migrated; this is a new app.

## Files
`app/src/main/java/app/awqat/prayer/`: `MainActivity` (shell and navigation), `HomeScreen`, `AdhkarScreen`, `TasbeehScreen`,
`HadithScreen`, `QiblaScreen`, `SettingsScreen`; `SkyView`, `CompassView`, `RingView`, `NavIconView` (custom drawing);
`PrayerCalc`, `Themes`, `ArabicText`, `ContentSearch`, `DailyPick`, `TasbeehLogic` (pure logic, unit tested);
`HadithData`, `AdhkarData` (generated by `tools/gen_content.py`); `AppState`, `Library`, `LocationHelper`, `Ui`, `HadithUi`.
