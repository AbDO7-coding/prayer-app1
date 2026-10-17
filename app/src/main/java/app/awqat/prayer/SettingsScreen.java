package app.awqat.prayer;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Theme picker with previews, appearance (system / light / dark) and the app's preferences. */
final class SettingsScreen implements Screen {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Themes.Palette p;

    SettingsScreen(MainActivity act, AppState st, Ui ui) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.p = ui.p;
    }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));
        root.addView(ui.text("الإعدادات والمظهر", 28, p.accent, true));
        root.addView(ui.text("يُحفظ اختيارك ويُطبَّق على كل الشاشات", 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        root.addView(ui.text("الوضع", 15, p.ink, true), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));
        LinearLayout modes = ui.row();
        String[] names = {"تلقائي (حسب النظام)", "فاتح", "داكن"};
        for (int i = 0; i < 3; i++) {
            final int a = i;
            modes.addView(ui.chip(names[i], st.appearance == i, v -> {
                if (st.appearance != a) { st.setAppearance(a); act.onThemeChanged(); }
            }), ui.weight(i == 0 ? 1.6f : 1f, 0, i == 2 ? 0 : 8));
        }
        root.addView(modes, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));

        root.addView(ui.text("السمات", 15, p.ink, true), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));
        for (int i = 0; i < Themes.ALL.length; i++) root.addView(themeCard(i));

        root.addView(ui.text("التفضيلات", 15, p.ink, true), ui.m(Ui.MATCH, Ui.WRAP, 0, 8, 0, 6));
        LinearLayout prefs = ui.card();
        prefs.addView(toggle("بطاقة حديث اليوم عند فتح التطبيق", st.startupCard, "startup_card", on -> { st.startupCard = on; act.invalidateScreen(0); }));
        prefs.addView(toggle("الاهتزاز", st.haptics, "haptics", on -> st.haptics = on));
        prefs.addView(toggle("صوت النقر في المسبحة", st.sound, "sound", on -> st.sound = on));
        prefs.addView(toggle("متابعة الأذكار من آخر ذكر غير مكتمل", st.resumeAdhkar, "resume", on -> st.resumeAdhkar = on));
        prefs.addView(toggle("الانتقال تلقائيًا إلى الذكر التالي", st.autoAdvance, "auto_adv", on -> st.autoAdvance = on));
        root.addView(prefs);

        root.addView(ui.text("عن المحتوى", 15, p.ink, true), ui.m(Ui.MATCH, Ui.WRAP, 0, 8, 0, 6));
        LinearLayout about = ui.card();
        String note = "عدد الأحاديث في قاعدة البيانات: " + HadithData.ALL.length + " (الهدف النهائي أكثر من "
            + HadithData.TARGET_COUNT + " وهو غير مكتمل بعد). عدد الأذكار: " + AdhkarData.ALL.length + ".";
        about.addView(ui.text(note, 14, p.ink, false));
        about.addView(ui.text(HadithData.REVIEWED_AGAINST_PRINTED_EDITIONS
            ? "روجعت النصوص على الطبعات المطبوعة."
            : "النصوص مكتوبة دون تشكيل وبإحالة إلى مصادرها، ولم تُراجَع بعد على الطبعات المطبوعة. يُنصح بمراجعتها لدى مختص قبل النشر العام.",
            13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 0));
        root.addView(about);
        sv.addView(root);
        return sv;
    }

    private View themeCard(final int index) {
        Themes.Theme t = Themes.ALL[index];
        Themes.Palette pv = Themes.resolve(index, st.appearance, st.systemDark());
        boolean selected = st.themeIndex == index;
        LinearLayout card = ui.row();
        GradientDrawable bg = ui.box(p.card, selected ? p.accent : p.line, 14);
        if (selected) bg.setStroke(ui.dp(2), p.accent);
        card.setBackground(bg);
        card.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        card.setLayoutParams(ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        LinearLayout preview = ui.row();
        preview.setBackground(ui.box(pv.bg, pv.line, 10));
        preview.setPadding(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6));
        preview.addView(swatch(pv.card, pv.line), ui.m(ui.dp(22), ui.dp(34), 0, 0, 4, 0));
        GradientDrawable hero = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{pv.heroA, pv.heroB});
        hero.setCornerRadius(ui.dp(6));
        View heroView = new View(act);
        heroView.setBackground(hero);
        preview.addView(heroView, ui.m(ui.dp(30), ui.dp(34), 0, 0, 4, 0));
        preview.addView(swatch(pv.accent, 0), ui.m(ui.dp(14), ui.dp(34), 0, 0, 4, 0));
        preview.addView(swatch(pv.accent2, 0), ui.m(ui.dp(14), ui.dp(34), 0, 0, 0, 0));
        card.addView(preview);
        TextView name = ui.text(t.nameAr, 16, p.ink, true);
        card.addView(name, ui.weight(1f, 12, 0));
        if (selected) card.addView(ui.text("مُفعّلة", 13, p.ok, true));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            if (st.themeIndex != index) { st.setTheme(index); act.onThemeChanged(); }
        });
        return card;
    }

    private View swatch(int color, int stroke) {
        View v = new View(act);
        v.setBackground(ui.box(color, stroke, 5));
        return v;
    }

    private interface Flag { void set(boolean on); }

    private View toggle(String label, boolean initial, final String key, final Flag f) {
        LinearLayout r = ui.row();
        CheckBox c = new CheckBox(act);
        c.setChecked(initial);
        c.setButtonTintList(ColorStateList.valueOf(p.accent));
        c.setOnCheckedChangeListener((b, on) -> {
            f.set(on);
            st.putBool(key, on);
        });
        r.addView(c);
        r.addView(ui.text(label, 15, p.ink, false), ui.weight(1f, 0, 0));
        return r;
    }
}
