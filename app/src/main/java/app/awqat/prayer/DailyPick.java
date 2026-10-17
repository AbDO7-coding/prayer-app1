package app.awqat.prayer;

/** Picks the "Hadith of the day": consecutive days (and consecutive sessions) never repeat while n > 1. */
public final class DailyPick {
    private DailyPick() {}

    private static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }

    /** Smallest stride >= 7 that is coprime with n, so every record is shown once before any repeats. */
    static int stride(int n) {
        if (n <= 2) return 1;
        int s = 7;
        while (gcd(s, n) != 1) s++;
        return s % n == 0 ? 1 : s;
    }

    /** sessionOffset increases by one for each new app session within the same day. */
    public static int index(long epochDay, int n, int sessionOffset) {
        if (n <= 0) return 0;
        long base = Math.floorMod(epochDay * (long) stride(n), (long) n);
        return (int) Math.floorMod(base + sessionOffset, (long) n);
    }
}
