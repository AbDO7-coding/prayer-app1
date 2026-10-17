package app.awqat.prayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Prayer times and Qibla maths for Morocco and Egypt (pure Java, no Android classes).
 * Morocco: Ministry of Habous angles (Fajr 19, Isha 17) with small safety offsets.
 * Egypt: Egyptian General Authority of Survey (Fajr 19.5, Isha 17.5). Asr: standard shadow.
 * Times are UTC epoch seconds, rounded to the minute.
 */
public final class PrayerCalc {
    private PrayerCalc() {}

    public static final String[] KEYS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    public static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    public static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    public static final int[] FIVE = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};
    public static final String[] WEEKDAYS = {"الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"};
    public static final String[] DIRS = {"شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب"};
    public static final double KAABA_LAT = 21.4225, KAABA_LNG = 39.8262;

    public static final class City {
        public final String name;
        public final double lat, lng;
        City(String name, double lat, double lng) { this.name = name; this.lat = lat; this.lng = lng; }
    }

    public static final class Country {
        public final String id, label, zone, note;
        public final double fajr, isha;
        public final int[] adj;
        public final boolean ramadanOption;
        public final City[] cities;
        public final String[] months;
        Country(String id, String label, String zone, double fajr, double isha, int[] adj, boolean ram,
                City[] cities, String[] months, String note) {
            this.id = id; this.label = label; this.zone = zone; this.fajr = fajr; this.isha = isha;
            this.adj = adj; this.ramadanOption = ram; this.cities = cities; this.months = months; this.note = note;
        }
    }

    public static final class Next {
        public final int index;
        public final long epoch;
        public final boolean tomorrow;
        public final long[] today;
        Next(int index, long epoch, boolean tomorrow, long[] today) {
            this.index = index; this.epoch = epoch; this.tomorrow = tomorrow; this.today = today;
        }
    }

    private static City c(String n, double a, double b) { return new City(n, a, b); }

    public static final Country MOROCCO = new Country("ma", "المغرب", "Africa/Casablanca", 19.0, 17.0,
        new int[]{0, -3, 5, 0, 5, 0}, true,
        new City[]{c("الدار البيضاء", 33.5731, -7.5898), c("الرباط", 34.0209, -6.8416), c("مراكش", 31.6295, -7.9811),
            c("فاس", 34.0331, -5.0003), c("طنجة", 35.7595, -5.834), c("أكادير", 30.4278, -9.5981),
            c("مكناس", 33.8935, -5.5473), c("وجدة", 34.6867, -1.9114), c("القنيطرة", 34.261, -6.5802),
            c("تطوان", 35.5785, -5.3684), c("آسفي", 32.2994, -9.2372), c("الجديدة", 33.2316, -8.5007),
            c("الناظور", 35.1681, -2.9335), c("بني ملال", 32.3373, -6.3498), c("الصويرة", 31.5085, -9.7595),
            c("العيون", 27.1253, -13.1625), c("الداخلة", 23.6848, -15.958), c("الرشيدية", 31.9314, -4.4244),
            c("ورزازات", 30.9189, -6.8934), c("الحسيمة", 35.2517, -3.9372), c("شفشاون", 35.1688, -5.2636),
            c("إفران", 33.5228, -5.1106), c("تازة", 34.21, -4.01), c("العرائش", 35.1932, -6.1557),
            c("تيزنيت", 29.6974, -9.7316), c("كلميم", 28.987, -10.0574)},
        new String[]{"يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو", "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"},
        "الفجر عند 19° والعشاء عند 17° تحت الأفق وفق وزارة الأوقاف، والعصر بظل مثل الشيء، مع فروق احتياطية صغيرة. "
        + "الأوقات بالتوقيت المحلي للمغرب بما فيه تغيير الساعة في رمضان. عدّل الفروق بالدقائق إن اختلفت عن جدول مسجدك.");

    public static final Country EGYPT = new Country("eg", "مصر", "Africa/Cairo", 19.5, 17.5,
        new int[]{0, 0, 0, 0, 0, 0}, false,
        new City[]{c("القاهرة", 30.0444, 31.2357), c("الإسكندرية", 31.2001, 29.9187), c("الجيزة", 30.0131, 31.2089),
            c("بورسعيد", 31.2653, 32.3019), c("السويس", 29.9668, 32.5498), c("الأقصر", 25.6872, 32.6396),
            c("أسوان", 24.0889, 32.8998), c("المنصورة", 31.0409, 31.3785), c("طنطا", 30.7865, 31.0004),
            c("أسيوط", 27.1809, 31.1837), c("الإسماعيلية", 30.5965, 32.2715), c("الفيوم", 29.3084, 30.8428),
            c("الزقازيق", 30.5877, 31.502), c("دمياط", 31.4175, 31.8144), c("المنيا", 28.1099, 30.7503),
            c("سوهاج", 26.5591, 31.6948), c("قنا", 26.1551, 32.716), c("الغردقة", 27.2579, 33.8116),
            c("شرم الشيخ", 27.9158, 34.33), c("مرسى مطروح", 31.3543, 27.2373), c("بني سويف", 29.0661, 31.0994),
            c("العريش", 31.1316, 33.7984)},
        new String[]{"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"},
        "الفجر عند 19.5° والعشاء عند 17.5° تحت الأفق وفق الهيئة المصرية العامة للمساحة، والعصر بظل مثل الشيء. "
        + "الأوقات بالتوقيت المحلي لمصر بما فيه التوقيت الصيفي. عدّل الفروق بالدقائق إن اختلفت عن جدول مسجدك.");

    public static final Country[] COUNTRIES = {MOROCCO, EGYPT};

    // ------------------------------------------------------------ astronomy
    private static final double R = Math.PI / 180.0;
    private static double sin(double a) { return Math.sin(a * R); }
    private static double cos(double a) { return Math.cos(a * R); }
    private static double tan(double a) { return Math.tan(a * R); }
    private static double asin(double x) { return Math.asin(Math.max(-1, Math.min(1, x))) / R; }
    private static double acos(double x) { return Math.acos(Math.max(-1, Math.min(1, x))) / R; }
    private static double atan2(double y, double x) { return Math.atan2(y, x) / R; }
    private static double acot(double x) { return Math.atan(1.0 / x) / R; }
    private static double mod(double a, double b) { return a - b * Math.floor(a / b); }

    /** Returns {declination (deg), equation of time (hours)}. */
    public static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = mod(357.529 + 0.98560028 * d, 360);
        double q = mod(280.459 + 0.98564736 * d, 360);
        double lon = mod(q + 1.915 * sin(g) + 0.02 * sin(2 * g), 360);
        double e = 23.439 - 3.6e-7 * d;
        double ra = mod(atan2(cos(e) * sin(lon), cos(lon)), 360) / 15.0;
        double eqt = q / 15.0 - ra;
        if (eqt > 12) eqt -= 24;
        if (eqt < -12) eqt += 24;
        return new double[]{asin(sin(e) * sin(lon)), eqt};
    }

    /** Six UTC epoch-second times (fajr, sunrise, dhuhr, asr, maghrib, isha) for a local date. */
    public static long[] compute(LocalDate d, double lat, double lng, Country c, int[] adj, boolean ramadan) {
        if (adj == null) adj = c.adj;
        long base = d.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        double jd = (base + 43200) / 86400.0 + 2440587.5;
        double[] sp = sunPosition(jd);
        double decl = sp[0];
        double noon = 12.0 - lng / 15.0 - sp[1];
        double den = cos(decl) * cos(lat);
        double asrAngle = acos((sin(acot(1 + tan(Math.abs(lat - decl)))) - sin(decl) * sin(lat)) / den) / 15.0;
        double[] h = {
            noon - ha(c.fajr, decl, lat, den), noon - ha(0.833, decl, lat, den), noon,
            noon + asrAngle, noon + ha(0.833, decl, lat, den), noon + ha(c.isha, decl, lat, den)};
        int extra = (ramadan && c.ramadanOption) ? 15 : 0;   // Isha +15 min, Morocco only
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) {
            double t = base + h[i] * 3600.0 + ((i < adj.length ? adj[i] : 0) + (i == ISHA ? extra : 0)) * 60.0;
            out[i] = (long) Math.floor(t / 60.0 + 0.5) * 60L;
        }
        return out;
    }

    private static double ha(double angle, double decl, double lat, double den) {
        return acos((-sin(angle) - sin(decl) * sin(lat)) / den) / 15.0;
    }

    // ------------------------------------------------------------ time zones
    public static ZoneId zone(Country c) { return ZoneId.of(c.zone); }

    public static LocalDate localDate(Country c, long epoch) {
        return Instant.ofEpochSecond(epoch).atZone(zone(c)).toLocalDate();
    }

    public static String fmtTime(Country c, long epoch) {
        return DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withZone(zone(c)).format(Instant.ofEpochSecond(epoch));
    }

    public static String fmtDate(Country c, LocalDate d) {
        return WEEKDAYS[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " "
            + c.months[d.getMonthValue() - 1] + " " + d.getYear();
    }

    // ------------------------------------------------------------ helpers
    /** The next of the five prayers (wraps to tomorrow's Fajr after Isha). */
    public static Next next(Country c, double lat, double lng, int[] adj, boolean ramadan, long now) {
        LocalDate today = localDate(c, now);
        long[] t = compute(today, lat, lng, c, adj, ramadan);
        for (int i : FIVE) if (t[i] > now) return new Next(i, t[i], false, t);
        long[] t2 = compute(today.plusDays(1), lat, lng, c, adj, ramadan);
        return new Next(FAJR, t2[FAJR], true, t);
    }

    /** {p (0..1 sunrise to sunset), 1 if daytime else 0, nightP (0..1 sunset to sunrise)}. */
    public static double[] sunState(long[] t, long now) {
        double span = Math.max(1, t[MAGHRIB] - t[SUNRISE]);
        double p = Math.max(0, Math.min(1, (now - t[SUNRISE]) / span));
        boolean day = now >= t[SUNRISE] && now <= t[MAGHRIB];
        double nightLen = 86400.0 - span;
        double np;
        if (now > t[MAGHRIB]) np = (now - t[MAGHRIB]) / nightLen;
        else if (now < t[SUNRISE]) np = (now + 86400.0 - t[MAGHRIB]) / nightLen;
        else np = 0;
        return new double[]{p, day ? 1 : 0, Math.max(0, Math.min(1, np))};
    }

    /** {bearing from true north in degrees, distance to the Kaaba in km}. */
    public static double[] qibla(double lat, double lng) {
        double p1 = lat * R, p2 = KAABA_LAT * R, dl = (KAABA_LNG - lng) * R;
        double b = mod(atan2(Math.sin(dl) * Math.cos(p2),
            Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl)) + 360, 360);
        double a = Math.pow(Math.sin((p2 - p1) / 2), 2) + Math.cos(p1) * Math.cos(p2) * Math.pow(Math.sin(dl / 2), 2);
        return new double[]{b, 6371.0 * 2 * Math.asin(Math.sqrt(a))};
    }
}
