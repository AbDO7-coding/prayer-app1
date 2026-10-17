package app.awqat.prayer;

import java.util.ArrayList;
import java.util.List;

/** Pure counter helpers (kept free of Android classes so they can be unit tested). */
public final class TasbeehLogic {
    private TasbeehLogic() {}

    public static boolean reached(int count, int target) { return target > 0 && count >= target; }

    public static float progress(int count, int target) {
        if (target <= 0) return 0f;
        return Math.min(1f, (float) count / (float) target);
    }

    public static int remaining(int count, int target) { return Math.max(0, target - count); }

    public static String duration(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(java.util.Locale.ENGLISH, "%02d:%02d", s / 60, s % 60);
    }

    /** Rejects a second tap delivered within minGapMs of the previous one (duplicate event protection). */
    public static final class TapGuard {
        private final long minGapMs;
        private long last = Long.MIN_VALUE / 2;
        public TapGuard(long minGapMs) { this.minGapMs = minGapMs; }
        public boolean accept(long nowMs) {
            if (nowMs - last < minGapMs) return false;
            last = nowMs;
            return true;
        }
    }

    /** History line format: epochSeconds|phraseIndex|total|durationSeconds ; newest first. */
    public static String encodeHistory(List<long[]> items) {
        StringBuilder b = new StringBuilder();
        for (long[] it : items) {
            if (b.length() > 0) b.append(';');
            b.append(it[0]).append('|').append(it[1]).append('|').append(it[2]).append('|').append(it[3]);
        }
        return b.toString();
    }

    public static List<long[]> decodeHistory(String s) {
        List<long[]> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(";")) {
            String[] f = part.split("\\|");
            if (f.length != 4) continue;
            try {
                out.add(new long[]{Long.parseLong(f[0]), Long.parseLong(f[1]), Long.parseLong(f[2]), Long.parseLong(f[3])});
            } catch (NumberFormatException ignored) { /* skip a corrupt entry */ }
        }
        return out;
    }

    public static List<long[]> addToHistory(List<long[]> items, long[] entry, int max) {
        List<long[]> out = new ArrayList<>();
        out.add(entry);
        out.addAll(items);
        while (out.size() > max) out.remove(out.size() - 1);
        return out;
    }
}
