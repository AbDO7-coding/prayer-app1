package app.awqat.prayer;

/** Arabic search helpers. The displayed text is never changed; only the search key is normalised. */
public final class ArabicText {
    private ArabicText() {}

    public static String normalize(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= '\u064B' && ch <= '\u065F') || ch == '\u0670' || ch == '\u0640'
                || (ch >= '\u06D6' && ch <= '\u06ED') || ch == '\u200F' || ch == '\u200E') continue;
            switch (ch) {
                case '\u0623': case '\u0625': case '\u0622': case '\u0671': ch = '\u0627'; break; // alef forms
                case '\u0649': ch = '\u064A'; break;   // alef maqsura -> ya
                case '\u0629': ch = '\u0647'; break;   // ta marbuta -> ha
                case '\u0624': ch = '\u0648'; break;   // waw with hamza
                case '\u0626': ch = '\u064A'; break;   // ya with hamza
                default:
                    if (ch >= '\u0660' && ch <= '\u0669') ch = (char) ('0' + (ch - '\u0660')); // Arabic-Indic digits
                    else ch = Character.toLowerCase(ch);
            }
            b.append(ch);
        }
        return b.toString().replaceAll("\\s+", " ").trim();
    }

    /** True when every word of the query appears in the (normalised) haystack. */
    public static boolean matches(String normalizedHaystack, String query) {
        String q = normalize(query);
        if (q.isEmpty()) return true;
        for (String w : q.split(" ")) if (!normalizedHaystack.contains(w)) return false;
        return true;
    }
}
