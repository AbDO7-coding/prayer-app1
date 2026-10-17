package app.awqat.prayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Shared hadith / dhikr actions (copy, share, bookmark) and the detail dialogs. */
final class HadithUi {
    private HadithUi() {}

    static String shareText(Hadith h) {
        return h.text + "\n\n" + h.narrator + "\n" + h.reference() + "\nالدرجة: " + h.gradeText + " (" + h.grader + ")";
    }

    static String shareText(Dhikr d) {
        return d.text + "\n\n" + d.source + "\n" + d.countNote;
    }

    static void copy(Activity a, String text) {
        ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("text", text));
        Toast.makeText(a, "تم النسخ", Toast.LENGTH_SHORT).show();
    }

    static void share(Activity a, String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        a.startActivity(Intent.createChooser(i, "مشاركة"));
    }

    static String gradeLine(Hadith h) { return h.gradeText + " — " + h.grader; }

    static void showHadith(final Activity a, final AppState st, final Ui ui, final Library lib, final Hadith h,
                           final Runnable onChange) {
        lib.addRecent(h.id);
        Themes.Palette p = ui.p;
        ScrollView sv = new ScrollView(a);
        LinearLayout box = ui.col();
        box.setBackgroundColor(p.card);
        box.setPadding(ui.dp(18), ui.dp(14), ui.dp(18), ui.dp(6));
        box.addView(ui.text(h.gradeText + " — " + h.grader, 13, p.ok, true));
        TextView body = ui.text(h.text, 22, p.ink, false);
        body.setLineSpacing(0f, 1.5f);
        body.setTextIsSelectable(true);
        box.addView(body, ui.m(Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));
        box.addView(ui.text("الراوي: " + h.narrator, 14, p.ink, false));
        box.addView(ui.text("المصدر: " + h.reference(), 14, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 0));
        if (!h.edition.isEmpty()) box.addView(ui.text(h.edition, 12, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 2, 0, 0));
        StringBuilder cats = new StringBuilder();
        for (String c : h.categories) {
            for (int i = 0; i < HadithData.CATEGORY_CODES.length; i++) {
                if (HadithData.CATEGORY_CODES[i].equals(c)) {
                    if (cats.length() > 0) cats.append("، ");
                    cats.append(HadithData.CATEGORY_NAMES[i]);
                }
            }
        }
        box.addView(ui.text("التصنيف: " + cats, 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 10));
        LinearLayout actions = ui.row();
        actions.addView(ui.button("نسخ", false, v -> copy(a, shareText(h))), ui.weight(1, 0, 6));
        actions.addView(ui.button("مشاركة", false, v -> share(a, shareText(h))), ui.weight(1, 0, 6));
        final TextView fav = ui.button(lib.isFav(h.id) ? "إزالة من المفضلة" : "حفظ في المفضلة", false, null);
        fav.setOnClickListener(v -> {
            boolean now = lib.toggleFav(h.id);
            fav.setText(now ? "إزالة من المفضلة" : "حفظ في المفضلة");
            if (onChange != null) onChange.run();
        });
        actions.addView(fav, ui.weight(1.4f, 0, 0));
        box.addView(actions);
        sv.addView(box);
        Ui.show(ui.dialog().setView(sv).setPositiveButton("إغلاق", null));
    }

    static void showDhikr(final Activity a, final AppState st, final Ui ui, final Library lib, final Dhikr d,
                          final Runnable onChange) {
        lib.addRecent(d.id);
        Themes.Palette p = ui.p;
        ScrollView sv = new ScrollView(a);
        LinearLayout box = ui.col();
        box.setBackgroundColor(p.card);
        box.setPadding(ui.dp(18), ui.dp(14), ui.dp(18), ui.dp(6));
        box.addView(ui.text(d.typeLabel() + " — " + ("morning".equals(d.period) ? "أذكار الصباح" : "أذكار المساء"), 13, p.accent, true));
        TextView body = ui.text(d.text, 22, p.ink, false);
        body.setLineSpacing(0f, 1.5f);
        body.setTextIsSelectable(true);
        box.addView(body, ui.m(Ui.MATCH, Ui.WRAP, 0, 12, 0, 12));
        box.addView(ui.text("عدد التكرار: " + d.count + (d.countNote.isEmpty() ? "" : " (" + d.countNote + ")"), 14, p.ink, false));
        box.addView(ui.text("المصدر: " + d.source, 14, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 0));
        box.addView(ui.text("الدرجة: " + d.grade, 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 4, 0, 10));
        LinearLayout actions = ui.row();
        actions.addView(ui.button("نسخ", false, v -> copy(a, shareText(d))), ui.weight(1, 0, 6));
        actions.addView(ui.button("مشاركة", false, v -> share(a, shareText(d))), ui.weight(1, 0, 6));
        final TextView fav = ui.button(lib.isFav(d.id) ? "إزالة من المفضلة" : "حفظ في المفضلة", false, null);
        fav.setOnClickListener(v -> {
            boolean now = lib.toggleFav(d.id);
            fav.setText(now ? "إزالة من المفضلة" : "حفظ في المفضلة");
            if (onChange != null) onChange.run();
        });
        actions.addView(fav, ui.weight(1.4f, 0, 0));
        box.addView(actions);
        sv.addView(box);
        Ui.show(ui.dialog().setView(sv).setPositiveButton("إغلاق", null));
    }
}
