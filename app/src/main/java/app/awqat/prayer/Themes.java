package app.awqat.prayer;

/** Ten themes, each with a light and a dark palette. Add a theme by adding one line to ALL. */
public final class Themes {
    private Themes() {}

    public static final int FOLLOW_SYSTEM = 0, LIGHT = 1, DARK = 2;

    public static final class Palette {
        public final boolean dark;
        public final int bg, card, ink, muted, accent, accent2, line, hi, heroA, heroB, sun, glow, ok, danger;
        Palette(boolean dark, int bg, int card, int ink, int muted, int accent, int accent2, int line, int hi,
                int heroA, int heroB, int sun, int glow) {
            this.dark = dark; this.bg = bg; this.card = card; this.ink = ink; this.muted = muted;
            this.accent = accent; this.accent2 = accent2; this.line = line; this.hi = hi;
            this.heroA = heroA; this.heroB = heroB; this.sun = sun; this.glow = glow;
            this.ok = dark ? 0xFF5FD6A0 : 0xFF1E7A4F;
            this.danger = dark ? 0xFFFF8A65 : 0xFFB93A0C;
        }
    }

    public static final class Theme {
        public final String id, nameAr;
        public final Palette light, dark;
        Theme(String id, String nameAr, Palette light, Palette dark) {
            this.id = id; this.nameAr = nameAr; this.light = light; this.dark = dark;
        }
    }

    private static Palette L(int bg, int card, int ink, int muted, int accent, int accent2, int line, int hi,
                             int heroA, int heroB, int sun, int glow) {
        return new Palette(false, bg, card, ink, muted, accent, accent2, line, hi, heroA, heroB, sun, glow);
    }

    private static Palette D(int bg, int card, int ink, int muted, int accent, int accent2, int line, int hi,
                             int heroA, int heroB, int sun, int glow) {
        return new Palette(true, bg, card, ink, muted, accent, accent2, line, hi, heroA, heroB, sun, glow);
    }

    public static final Theme[] ALL = {
        new Theme("royal_emerald", "الزمرد الملكي",
            L(0xFFEEF5F0, 0xFFFFFFFF, 0xFF10241B, 0xFF4F6258, 0xFF0B6B4B, 0xFF8F6B00, 0xFFD5E3DA, 0xFFDDEEE4, 0xFF0B6B4B, 0xFF064230, 0xFFF4C95D, 0xFFFFE9A8),
            D(0xFF07130E, 0xFF0E211A, 0xFFE7F3EC, 0xFF9DB5A8, 0xFF4FD1A0, 0xFFE0B94A, 0xFF1E3A2F, 0xFF12302A, 0xFF0B6B4B, 0xFF04271C, 0xFFF4C95D, 0xFFFFE9A8)),
        new Theme("midnight_gold", "ذهب منتصف الليل",
            L(0xFFF6F1E3, 0xFFFFFDF7, 0xFF1B1810, 0xFF625A43, 0xFF7A5F0A, 0xFF8F6B00, 0xFFE4DAB8, 0xFFF0E6C3, 0xFF3B3010, 0xFF15110A, 0xFFF2C94C, 0xFFFFE08A),
            D(0xFF0A0A0C, 0xFF15151A, 0xFFF3EFE4, 0xFFAFA994, 0xFFD4AF37, 0xFFF0D58A, 0xFF2A2A30, 0xFF1D1B16, 0xFF1A1608, 0xFF050505, 0xFFF2C94C, 0xFFFFE08A)),
        new Theme("ocean_blue", "أزرق المحيط",
            L(0xFFEAF3FB, 0xFFFFFFFF, 0xFF0F2438, 0xFF4B647D, 0xFF0B5FA5, 0xFF1D7FBF, 0xFFD0E1F0, 0xFFDCEBF8, 0xFF0B5FA5, 0xFF083B6B, 0xFFFFD66B, 0xFFFFF0B8),
            D(0xFF07141F, 0xFF0D2233, 0xFFE6F1FB, 0xFF9AB3C9, 0xFF6CC0F5, 0xFF9ADBFF, 0xFF1B3850, 0xFF12304A, 0xFF0B5FA5, 0xFF06263F, 0xFFFFD66B, 0xFFFFF0B8)),
        new Theme("pure_white", "أبيض نقي",
            L(0xFFF5F6F8, 0xFFFFFFFF, 0xFF1A1D21, 0xFF5B616B, 0xFF374151, 0xFF6B7280, 0xFFE5E7EB, 0xFFEEF0F3, 0xFF334155, 0xFF1F2937, 0xFFFDE68A, 0xFFFFFBEB),
            D(0xFF121316, 0xFF1B1D21, 0xFFECEDEF, 0xFFA3A9B1, 0xFFD1D5DB, 0xFF9CA3AF, 0xFF2B2E33, 0xFF24272C, 0xFF334155, 0xFF111827, 0xFFFDE68A, 0xFFFFFBEB)),
        new Theme("desert_sand", "رمال الصحراء",
            L(0xFFF4EBDD, 0xFFFFFAF1, 0xFF2B2117, 0xFF6B5B46, 0xFF8F5420, 0xFF9A6A2D, 0xFFE6D6BD, 0xFFEFE0C8, 0xFF9A5A28, 0xFF6B3C18, 0xFFFFD27A, 0xFFFFE9B8),
            D(0xFF17120D, 0xFF211A13, 0xFFF2E8D8, 0xFFBBAB94, 0xFFE0A265, 0xFFF0C28A, 0xFF3A2E21, 0xFF2C2217, 0xFF8F5420, 0xFF4A2A12, 0xFFFFD27A, 0xFFFFE9B8)),
        new Theme("royal_purple", "البنفسج الملكي",
            L(0xFFF1EDF7, 0xFFFFFFFF, 0xFF1E1530, 0xFF5E5375, 0xFF5B2C9A, 0xFF6B7280, 0xFFDDD3EC, 0xFFE8DFF5, 0xFF5B2C9A, 0xFF34175C, 0xFFFFD76A, 0xFFFFEFB5),
            D(0xFF100A1B, 0xFF1A1228, 0xFFEFE9F8, 0xFFB1A4C9, 0xFFB791F0, 0xFFC9CED6, 0xFF2F2347, 0xFF251A3A, 0xFF5B2C9A, 0xFF2A1248, 0xFFFFD76A, 0xFFFFEFB5)),
        new Theme("dark_amoled", "أسود أموليد",
            L(0xFFF4F4F4, 0xFFFFFFFF, 0xFF111111, 0xFF595959, 0xFF0E6B4A, 0xFF1F8F66, 0xFFE0E0E0, 0xFFECECEC, 0xFF0E5A41, 0xFF06281D, 0xFFF7D060, 0xFFFFE9A0),
            D(0xFF000000, 0xFF0B0B0B, 0xFFEDEDED, 0xFFA3A3A3, 0xFF3DDC97, 0xFF8BE9C0, 0xFF1C1C1C, 0xFF141414, 0xFF0E3B2C, 0xFF000000, 0xFFF7D060, 0xFFFFE9A0)),
        new Theme("islamic_green", "الأخضر الإسلامي",
            L(0xFFEDF4EA, 0xFFFBFEF9, 0xFF12261A, 0xFF52675A, 0xFF1F6B3A, 0xFF5E7F3F, 0xFFD3E2CC, 0xFFDFEBD9, 0xFF2E7D4F, 0xFF14472B, 0xFFF6D365, 0xFFFFF0B0),
            D(0xFF0A140D, 0xFF112016, 0xFFE4F0E6, 0xFF9CB2A1, 0xFF6FCF8E, 0xFFA5D6A7, 0xFF1F3A28, 0xFF17301F, 0xFF2E7D4F, 0xFF0D3320, 0xFFF6D365, 0xFFFFF0B0)),
        new Theme("sunrise", "شروق الشمس",
            L(0xFFFFF4EA, 0xFFFFFFFF, 0xFF3A2218, 0xFF7A5A4B, 0xFFB5481A, 0xFFC4506C, 0xFFF3DCCB, 0xFFFFE7D3, 0xFFBC5018, 0xFF9A3A4C, 0xFFFFE08A, 0xFFFFF3C4),
            D(0xFF1A0F0C, 0xFF26150F, 0xFFFBEADF, 0xFFC9A998, 0xFFFF9A57, 0xFFFF9DB5, 0xFF3F241A, 0xFF331C14, 0xFFB5501C, 0xFF7A2E48, 0xFFFFD27A, 0xFFFFE9B8)),
        new Theme("sunset", "غروب الشمس",
            L(0xFFFBEFEA, 0xFFFFFFFF, 0xFF2C1420, 0xFF6E4A5A, 0xFFB5321E, 0xFF7B3FA0, 0xFFF0D6D0, 0xFFF7E1DA, 0xFFC2441F, 0xFF4B1D6B, 0xFFFFB347, 0xFFFFD9A0),
            D(0xFF150B16, 0xFF1F1020, 0xFFF6E7EC, 0xFFC4A3B3, 0xFFFF7A59, 0xFFC58BF0, 0xFF38203A, 0xFF2B1730, 0xFFB8422A, 0xFF3A1259, 0xFFFFB347, 0xFFFFD9A0)),
    };

    /** appearance: FOLLOW_SYSTEM, LIGHT or DARK. */
    public static Palette resolve(int themeIndex, int appearance, boolean systemDark) {
        Theme t = ALL[Math.max(0, Math.min(ALL.length - 1, themeIndex))];
        boolean dark = appearance == DARK || (appearance == FOLLOW_SYSTEM && systemDark);
        return dark ? t.dark : t.light;
    }

    // ---- WCAG contrast helpers (used by the unit tests)
    private static double lin(int c) {
        double v = c / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    public static double luminance(int argb) {
        return 0.2126 * lin((argb >> 16) & 255) + 0.7152 * lin((argb >> 8) & 255) + 0.0722 * lin(argb & 255);
    }

    public static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }
}
