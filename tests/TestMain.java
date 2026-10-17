import app.awqat.prayer.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class TestMain {
    static int pass = 0, fail = 0;
    static void check(boolean ok, String name) {
        if (ok) pass++; else { fail++; System.out.println("FAIL: " + name); }
    }

    public static void main(String[] args) throws Exception {
        // 1. Java port vs the Python reference (epoch seconds must match exactly)
        int cases = 0;
        for (String line : Files.readAllLines(Paths.get(args[0]))) {
            String[] f = line.trim().split(" ");
            if (f.length < 12) continue;
            PrayerCalc.Country c = f[0].equals("ma") ? PrayerCalc.MOROCCO : PrayerCalc.EGYPT;
            long[] t = PrayerCalc.compute(LocalDate.of(Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5])),
                Double.parseDouble(f[1]), Double.parseDouble(f[2]), c, null, false);
            boolean same = true;
            for (int i = 0; i < 6; i++) same &= t[i] == Long.parseLong(f[6 + i]);
            check(same, "python reference case " + line.substring(0, Math.min(40, line.length())));
            cases++;
        }
        check(cases >= 8, "reference cases loaded: " + cases);

        // 2. every city, several months: times strictly increase
        for (PrayerCalc.Country c : PrayerCalc.COUNTRIES) {
            for (PrayerCalc.City city : c.cities) {
                for (int m : new int[]{1, 3, 6, 9, 12}) {
                    long[] t = PrayerCalc.compute(LocalDate.of(2026, m, 15), city.lat, city.lng, c, null, false);
                    boolean ordered = true;
                    for (int i = 1; i < 6; i++) ordered &= t[i] > t[i - 1];
                    check(ordered, "order " + c.id + " " + city.name + " month " + m);
                }
            }
        }

        // 3. time zones: Morocco switches to UTC+0 for Ramadan, Egypt has summer time
        ZoneId ma = PrayerCalc.zone(PrayerCalc.MOROCCO), eg = PrayerCalc.zone(PrayerCalc.EGYPT);
        check(ma.getRules().getOffset(Instant.parse("2026-03-01T12:00:00Z")).getTotalSeconds() == 0, "Morocco offset in Ramadan 2026");
        check(ma.getRules().getOffset(Instant.parse("2026-10-14T12:00:00Z")).getTotalSeconds() == 3600, "Morocco offset in October");
        check(eg.getRules().getOffset(Instant.parse("2026-10-14T12:00:00Z")).getTotalSeconds() == 3 * 3600, "Egypt summer time in mid-October");
        check(eg.getRules().getOffset(Instant.parse("2026-01-15T12:00:00Z")).getTotalSeconds() == 2 * 3600, "Egypt winter time");
        long[] cai = PrayerCalc.compute(LocalDate.of(2026, 10, 14), 30.0444, 31.2357, PrayerCalc.EGYPT, null, false);
        String dh = PrayerCalc.fmtTime(PrayerCalc.EGYPT, cai[PrayerCalc.DHUHR]);
        check(dh.compareTo("12:30") >= 0 && dh.compareTo("12:55") <= 0, "Cairo Dhuhr plausible: " + dh);
        long[] cas = PrayerCalc.compute(LocalDate.of(2026, 10, 14), 33.5731, -7.5898, PrayerCalc.MOROCCO, null, false);
        check(PrayerCalc.fmtTime(PrayerCalc.MOROCCO, cas[PrayerCalc.DHUHR]).startsWith("13:"), "Casablanca Dhuhr plausible");

        // 4. Ramadan option only changes Morocco
        long a1 = PrayerCalc.compute(LocalDate.of(2026, 3, 1), 33.57, -7.59, PrayerCalc.MOROCCO, null, false)[5];
        long a2 = PrayerCalc.compute(LocalDate.of(2026, 3, 1), 33.57, -7.59, PrayerCalc.MOROCCO, null, true)[5];
        check(a2 - a1 == 900, "Morocco Ramadan adds 15 minutes to Isha");
        long e1 = PrayerCalc.compute(LocalDate.of(2026, 3, 1), 30.04, 31.23, PrayerCalc.EGYPT, null, false)[5];
        long e2 = PrayerCalc.compute(LocalDate.of(2026, 3, 1), 30.04, 31.23, PrayerCalc.EGYPT, null, true)[5];
        check(e1 == e2, "Ramadan option ignored in Egypt");

        // 5. Qibla
        double[] q = PrayerCalc.qibla(33.5731, -7.5898);
        check(q[0] > 85 && q[0] < 100 && q[1] > 4000 && q[1] < 5000, "Qibla Casablanca " + q[0]);
        q = PrayerCalc.qibla(30.0444, 31.2357);
        check(q[0] > 130 && q[0] < 145, "Qibla Cairo " + q[0]);
        check(PrayerCalc.qibla(21.4225, 39.8262)[1] < 0.1, "Qibla at the Kaaba is zero km");

        // 6. next prayer wraps to tomorrow after Isha, and sun state is sane
        PrayerCalc.Next n = PrayerCalc.next(PrayerCalc.MOROCCO, 33.5731, -7.5898, null, false, cas[5] + 60);
        check(n.index == PrayerCalc.FAJR && n.tomorrow && n.epoch > cas[5], "next after Isha is tomorrow's Fajr");
        double[] s = PrayerCalc.sunState(cas, cas[PrayerCalc.DHUHR]);
        check(s[1] == 1 && s[0] > 0.4 && s[0] < 0.6, "sun is high at Dhuhr: " + s[0]);
        s = PrayerCalc.sunState(cas, cas[PrayerCalc.ISHA]);
        check(s[1] == 0 && s[2] > 0 && s[2] < 0.3, "night progress after Isha: " + s[2]);

        // 7. Arabic search normalisation
        check(ArabicText.normalize("الرَّحْمَٰنِ").equals(ArabicText.normalize("الرحمن")) , "tashkeel removed");
        check(ArabicText.matches(ArabicText.normalize("إنما الأعمال بالنيات"), "انما الاعمال"), "hamza forms match");
        check(ArabicText.matches(ArabicText.normalize("الصلاة"), "الصلاه"), "ta marbuta matches ha");
        check(!ArabicText.matches(ArabicText.normalize("الصلاة"), "الزكاة"), "non-matching word rejected");

        // 8. themes
        check(Themes.ALL.length == 10, "ten themes");
        Set<String> ids = new HashSet<>();
        for (Themes.Theme t : Themes.ALL) {
            check(ids.add(t.id), "unique theme id " + t.id);
            for (Themes.Palette p : new Themes.Palette[]{t.light, t.dark}) {
                String tag = t.id + (p.dark ? " dark" : " light");
                check(Themes.contrast(p.ink, p.bg) >= 7.0, tag + " ink/bg " + fmt(Themes.contrast(p.ink, p.bg)));
                check(Themes.contrast(p.ink, p.card) >= 7.0, tag + " ink/card " + fmt(Themes.contrast(p.ink, p.card)));
                check(Themes.contrast(p.ink, p.hi) >= 7.0, tag + " ink/hi " + fmt(Themes.contrast(p.ink, p.hi)));
                check(Themes.contrast(p.muted, p.bg) >= 4.5, tag + " muted/bg " + fmt(Themes.contrast(p.muted, p.bg)));
                check(Themes.contrast(p.muted, p.card) >= 4.5, tag + " muted/card " + fmt(Themes.contrast(p.muted, p.card)));
                check(Themes.contrast(p.accent, p.card) >= 4.5, tag + " accent/card " + fmt(Themes.contrast(p.accent, p.card)));
                check(Themes.contrast(p.accent, p.hi) >= 4.5, tag + " accent/hi " + fmt(Themes.contrast(p.accent, p.hi)));
                check(Themes.contrast(p.accent2, p.card) >= 3.0, tag + " accent2/card " + fmt(Themes.contrast(p.accent2, p.card)));
                check(Themes.contrast(0xFFFFFFFF, p.heroA) >= 4.5, tag + " white/heroA " + fmt(Themes.contrast(0xFFFFFFFF, p.heroA)));
                check(Themes.contrast(0xFFFFFFFF, p.heroB) >= 4.5, tag + " white/heroB " + fmt(Themes.contrast(0xFFFFFFFF, p.heroB)));
            }
        }
        check(Themes.resolve(0, Themes.DARK, false).dark && !Themes.resolve(0, Themes.LIGHT, true).dark
            && Themes.resolve(0, Themes.FOLLOW_SYSTEM, true).dark, "appearance resolution");

        // 9. hadith content
        Set<String> hid = new HashSet<>(), htext = new HashSet<>();
        int sahih = 0, hasan = 0;
        for (Hadith h : HadithData.ALL) {
            check(hid.add(h.id), "unique hadith id " + h.id);
            check(htext.add(ArabicText.normalize(h.text)), "unique hadith text " + h.id);
            check(!h.text.isEmpty() && !h.collection.isEmpty() && !h.number.isEmpty() && !h.narrator.isEmpty(), "hadith fields " + h.id);
            check(h.gradeClass.equals("sahih") || h.gradeClass.equals("hasan"), "hadith grade class " + h.id);
            check(!h.grader.isEmpty() && !h.gradeText.isEmpty(), "hadith grader " + h.id);
            check(h.categories.length > 0, "hadith category " + h.id);
            if (h.gradeClass.equals("sahih")) sahih++; else hasan++;
        }
        Set<String> kid = new HashSet<>();
        for (Dhikr d : AdhkarData.ALL) {
            check(kid.add(d.id), "unique dhikr id " + d.id);
            check(d.count >= 1 && !d.text.isEmpty() && !d.source.isEmpty() && !d.grade.isEmpty(), "dhikr fields " + d.id);
            check(d.type.equals("quran") || d.type.equals("prophetic"), "dhikr type " + d.id);
            check(d.period.equals("morning") || d.period.equals("evening"), "dhikr period " + d.id);
        }
        check(HadithData.CATEGORY_CODES.length == 12, "twelve hadith categories");

        // 10. tasbeeh
        check(TasbeehData.PHRASES.length == 20, "twenty tasbeeh phrases");
        check(new HashSet<>(Arrays.asList(TasbeehData.PHRASES)).size() == 20, "tasbeeh phrases are distinct");
        check(TasbeehLogic.reached(33, 33) && !TasbeehLogic.reached(32, 33) && !TasbeehLogic.reached(5, 0), "target reached logic");
        check(TasbeehLogic.progress(50, 100) == 0.5f && TasbeehLogic.progress(200, 100) == 1f, "progress");
        check(TasbeehLogic.remaining(30, 33) == 3 && TasbeehLogic.remaining(40, 33) == 0, "remaining");
        TasbeehLogic.TapGuard g = new TasbeehLogic.TapGuard(25);
        check(g.accept(1000) && !g.accept(1010) && g.accept(1030), "tap guard drops duplicate events only");
        int taps = 0; TasbeehLogic.TapGuard g2 = new TasbeehLogic.TapGuard(25);
        for (int i = 0; i < 100; i++) if (g2.accept(5000L + i * 80L)) taps++;
        check(taps == 100, "100 rapid taps 80 ms apart are all counted");
        List<long[]> hist = new ArrayList<>();
        for (int i = 0; i < 5; i++) hist = TasbeehLogic.addToHistory(hist, new long[]{1000 + i, i, 33, 20}, 3);
        check(hist.size() == 3 && hist.get(0)[0] == 1004, "history keeps newest first, capped");
        List<long[]> back = TasbeehLogic.decodeHistory(TasbeehLogic.encodeHistory(hist));
        check(back.size() == 3 && back.get(2)[2] == 33 && TasbeehLogic.decodeHistory("garbage;1|2").isEmpty(), "history round trip");
        check(TasbeehLogic.duration(125).equals("02:05"), "duration format");


        // 11. content search and daily pick
        check(ContentSearch.hadith("الغضب", null).size() >= 1, "search finds the anger hadith");
        check(ContentSearch.hadith("الصلاه", "prayer").size() >= 3, "search with ta marbuta variant inside a category");
        check(ContentSearch.hadith("zzzz غير موجود", null).isEmpty(), "empty result for unknown query");
        check(ContentSearch.dhikr("الكرسي").isEmpty() == false || ContentSearch.dhikr("القيوم").size() >= 2, "dhikr search finds Ayat al-Kursi");
        check(ContentSearch.dhikr("").isEmpty(), "no dhikr results for an empty query");
        check(ContentSearch.hadithById("h001") != null && ContentSearch.dhikrById("k001") != null, "lookup by id");
        int nH = HadithData.ALL.length;
        Set<Integer> seen = new HashSet<>();
        boolean noRepeat = true;
        for (int day = 20000; day < 20000 + nH; day++) {
            int i = DailyPick.index(day, nH, 0);
            noRepeat &= seen.add(i) && i != DailyPick.index(day + 1, nH, 0);
        }
        check(noRepeat && seen.size() == nH, "daily pick cycles through every hadith without consecutive repeats");
        check(DailyPick.index(20000, nH, 0) != DailyPick.index(20000, nH, 1), "a new session on the same day shows a different hadith");
        check(DailyPick.index(5, 1, 3) == 0 && DailyPick.index(5, 0, 0) == 0, "daily pick edge cases");

        System.out.println("hadith records: " + HadithData.ALL.length + " (" + sahih + " sahih, " + hasan + " hasan); target "
            + HadithData.TARGET_COUNT + (HadithData.ALL.length > HadithData.TARGET_COUNT ? " met" : " NOT MET"));
        System.out.println("adhkar records: " + AdhkarData.ALL.length + "; tasbeeh phrases: " + TasbeehData.PHRASES.length
            + "; themes: " + Themes.ALL.length);
        System.out.println("checks passed: " + pass + ", failed: " + fail);
        System.exit(fail == 0 ? 0 : 1);
    }

    static String fmt(double d) { return String.format(Locale.ENGLISH, "%.2f", d); }
}
