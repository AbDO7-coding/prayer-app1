#!/bin/sh
# Runs the unit tests for the pure-Java logic (calculation, themes, content, search, counter helpers).
# Needs a JDK 17+ (java and its built-in compiler). The Android screens are not covered by these tests.
set -e
cd "$(dirname "$0")"
python3 tools/gen_content.py          # regenerates and validates the hadith / adhkar data
SRC=app/src/main/java/app/awqat/prayer
java tools/Compile.java /tmp/prayer_test_out \
  $SRC/PrayerCalc.java $SRC/ArabicText.java $SRC/Themes.java $SRC/TasbeehData.java $SRC/TasbeehLogic.java \
  $SRC/Hadith.java $SRC/HadithData.java $SRC/Dhikr.java $SRC/AdhkarData.java $SRC/ContentSearch.java \
  $SRC/DailyPick.java tests/TestMain.java
java -cp /tmp/prayer_test_out TestMain tests/reference_times.txt
