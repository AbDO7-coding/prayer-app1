package app.awqat.prayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/** Settings and selections shared by all screens (persisted in SharedPreferences). */
final class AppState {
    final Context ctx;
    final SharedPreferences prefs;
    Themes.Palette pal;
    int themeIndex, appearance;
    boolean startupCard, haptics, sound, resumeAdhkar, autoAdvance, compactCounter;
    PrayerCalc.Country country;
    int cityIdx;
    boolean useGps, hasGps;
    double gpsLat, gpsLng;
    int[] adj;
    boolean ramadan;

    AppState(Context c) {
        ctx = c;
        prefs = c.getSharedPreferences("prayer", Context.MODE_PRIVATE);
        themeIndex = Math.max(0, Math.min(Themes.ALL.length - 1, prefs.getInt("theme", 0)));
        appearance = prefs.getInt("appearance", Themes.FOLLOW_SYSTEM);
        startupCard = prefs.getBoolean("startup_card", true);
        haptics = prefs.getBoolean("haptics", true);
        sound = prefs.getBoolean("sound", false);          // click sounds are off by default
        resumeAdhkar = prefs.getBoolean("resume", true);
        autoAdvance = prefs.getBoolean("auto_adv", true);
        compactCounter = prefs.getBoolean("compact", false);
        reloadPalette();
        setCountry("eg".equals(prefs.getString("cc", "ma")) ? PrayerCalc.EGYPT : PrayerCalc.MOROCCO, false);
    }

    boolean systemDark() {
        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    void reloadPalette() { pal = Themes.resolve(themeIndex, appearance, systemDark()); }

    void setTheme(int i) {
        themeIndex = i;
        prefs.edit().putInt("theme", i).apply();
        reloadPalette();
    }

    void setAppearance(int a) {
        appearance = a;
        prefs.edit().putInt("appearance", a).apply();
        reloadPalette();
    }

    void putBool(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }

    void setCountry(PrayerCalc.Country c, boolean persist) {
        country = c;
        adj = c.adj.clone();
        ramadan = false;
        useGps = false;
        hasGps = false;
        cityIdx = prefs.getInt("city_" + c.id, 0);
        if (cityIdx < 0 || cityIdx >= c.cities.length) cityIdx = 0;
        if (persist) prefs.edit().putString("cc", c.id).apply();
    }

    void setCity(int i) {
        cityIdx = i;
        useGps = false;
        prefs.edit().putInt("city_" + country.id, i).apply();
    }

    void setGps(double lat, double lng) {
        gpsLat = lat;
        gpsLng = lng;
        hasGps = true;
        useGps = true;
    }

    double[] latLng() {
        if (useGps && hasGps) return new double[]{gpsLat, gpsLng};
        PrayerCalc.City c = country.cities[cityIdx];
        return new double[]{c.lat, c.lng};
    }
}
