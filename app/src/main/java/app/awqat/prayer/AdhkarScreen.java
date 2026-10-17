package app.awqat.prayer;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Morning and evening remembrances with per-item counters. Progress is saved per day and per period. */
final class AdhkarScreen implements Screen {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Library lib;
    private final Themes.Palette p;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int period;                 // 0 morning, 1 evening
    private List<Dhikr> list = new ArrayList<>();
    private int idx;

    private TextView tabM, tabE, lProgress, lType, lText, lCount, lSource, lNote, lIndex;
    private View barDone, barRest;
    private RingView ring;

    AdhkarScreen(MainActivity act, AppState st, Ui ui, Library lib) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.lib = lib;
        this.p = ui.p;
    }

    private String periodCode() { return period == 0 ? "morning" : "evening"; }

    private String key(Dhikr d) {
        return "ad_" + d.period + "_" + LocalDate.now().toString().replace("-", "") + "_" + d.id;
    }

    private int count(Dhikr d) { return st.prefs.getInt(key(d), 0); }

    private boolean done(Dhikr d) { return count(d) >= d.count; }

    private void loadList() {
        list = new ArrayList<>();
        for (Dhikr d : AdhkarData.ALL) if (d.period.equals(periodCode())) list.add(d);
    }

    private int firstIncomplete() {
        for (int i = 0; i < list.size(); i++) if (!done(list.get(i))) return i;
        return Math.max(0, list.size() - 1);
    }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));
        root.addView(ui.text("الأذكار", 28, p.accent, true));
        root.addView(ui.text("يُحفظ تقدمك تلقائيًا ويبدأ من جديد كل يوم", 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        LinearLayout tabs = ui.row();
        tabM = ui.chip("أذكار الصباح", true, v -> switchPeriod(0));
        tabE = ui.chip("أذكار المساء", false, v -> switchPeriod(1));
        tabs.addView(tabM, ui.weight(1, 0, 8));
        tabs.addView(tabE, ui.weight(1, 0, 0));
        root.addView(tabs, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        LinearLayout prog = ui.card();
        lProgress = ui.text("", 14, p.ink, true);
        prog.addView(lProgress);
        LinearLayout bar = new LinearLayout(act);
        bar.setBackground(ui.box(p.hi, 0, 5));
        barDone = new View(act);
        barDone.setBackground(ui.box(p.ok, 0, 5));
        barRest = new View(act);
        bar.addView(barDone, new LinearLayout.LayoutParams(0, ui.dp(8), 0f));
        bar.addView(barRest, new LinearLayout.LayoutParams(0, ui.dp(8), 1f));
        prog.addView(bar, ui.m(Ui.MATCH, ui.dp(8), 0, 8, 0, 0));
        root.addView(prog);

        LinearLayout card = ui.card();
        lIndex = ui.text("", 13, p.muted, false);
        card.addView(lIndex);
        lType = ui.text("", 13, p.accent, true);
        card.addView(lType, ui.m(Ui.MATCH, Ui.WRAP, 0, 2, 0, 8));
        lText = ui.text("", 22, p.ink, false);
        lText.setLineSpacing(0f, 1.55f);
        card.addView(lText);
        FrameLayout ringBox = new FrameLayout(act);
        ring = new RingView(act, p.hi, p.accent, p.ok, ui.dp(8));
        ringBox.addView(ring, new FrameLayout.LayoutParams(ui.dp(120), ui.dp(120), Gravity.CENTER));
        lCount = ui.text("", 26, p.ink, true);
        lCount.setTextDirection(View.TEXT_DIRECTION_LTR);
        ringBox.addView(lCount, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.CENTER));
        ringBox.setOnClickListener(v -> increment(v));
        card.addView(ringBox, ui.m(Ui.MATCH, ui.dp(130), 0, 14, 0, 6));
        card.addView(ui.text("اضغط على الدائرة للعدّ", 12, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        lNote = ui.text("", 13, p.muted, false);
        card.addView(lNote);
        lSource = ui.text("", 12, p.muted, false);
        card.addView(lSource, ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 0));
        root.addView(card);

        LinearLayout nav = ui.row();
        nav.addView(ui.button("السابق", false, v -> go(idx - 1)), ui.weight(1, 0, 6));
        nav.addView(ui.button("عدّ", true, v -> increment(v)), ui.weight(1.2f, 0, 6));
        nav.addView(ui.button("التالي", false, v -> go(idx + 1)), ui.weight(1, 0, 0));
        root.addView(nav, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        LinearLayout more = ui.row();
        more.addView(ui.button("القائمة", false, v -> showList()), ui.weight(1, 0, 6));
        more.addView(ui.button("إعادة التقدم", false, v -> confirmReset()), ui.weight(1, 0, 0));
        root.addView(more);
        sv.addView(root);
        period = 0;
        loadList();
        idx = st.resumeAdhkar ? firstIncomplete() : 0;
        render();
        return sv;
    }

    private void switchPeriod(int pr) {
        period = pr;
        loadList();
        idx = st.resumeAdhkar ? firstIncomplete() : 0;
        tabM.setBackground(ui.box(pr == 0 ? p.heroA : p.hi, 0, 18));
        tabM.setTextColor(pr == 0 ? Ui.WHITE : p.ink);
        tabE.setBackground(ui.box(pr == 1 ? p.heroA : p.hi, 0, 18));
        tabE.setTextColor(pr == 1 ? Ui.WHITE : p.ink);
        render();
    }

    private void go(int i) {
        if (i < 0 || i >= list.size()) return;
        idx = i;
        render();
    }

    private void increment(View src) {
        if (list.isEmpty()) return;
        Dhikr d = list.get(idx);
        int c = count(d);
        if (c >= d.count) return;                          // never count past the prescribed repetitions
        st.prefs.edit().putInt(key(d), c + 1).apply();
        if (st.haptics) src.performHapticFeedback(c + 1 >= d.count ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.KEYBOARD_TAP);
        render();
        if (c + 1 >= d.count) {
            if (allDone()) {
                Toast.makeText(act, period == 0 ? "أتممت أذكار الصباح" : "أتممت أذكار المساء", Toast.LENGTH_LONG).show();
            } else if (st.autoAdvance && idx < list.size() - 1) {
                final int from = idx;
                handler.postDelayed(() -> { if (idx == from) go(idx + 1); }, 450);
            }
        }
    }

    private boolean allDone() {
        for (Dhikr d : list) if (!done(d)) return false;
        return true;
    }

    private void render() {
        if (list.isEmpty()) return;
        int completed = 0;
        for (Dhikr d : list) if (done(d)) completed++;
        lProgress.setText("أكملت " + completed + " من " + list.size() + " ذكرًا");
        ((LinearLayout.LayoutParams) barDone.getLayoutParams()).weight = completed;
        ((LinearLayout.LayoutParams) barRest.getLayoutParams()).weight = list.size() - completed;
        barDone.requestLayout();
        Dhikr d = list.get(idx);
        int c = count(d);
        lIndex.setText("الذكر " + (idx + 1) + " من " + list.size());
        lType.setText(d.typeLabel() + (done(d) ? "  ✓ مكتمل" : ""));
        lText.setText(d.text);
        lCount.setText(String.format(Locale.ENGLISH, "%d / %d", Math.min(c, d.count), d.count));
        ring.set(TasbeehLogic.progress(c, d.count), done(d));
        lNote.setText(d.countNote);
        lNote.setVisibility(d.countNote.isEmpty() ? View.GONE : View.VISIBLE);
        lSource.setText("المصدر: " + d.source + "\nالدرجة: " + d.grade);
    }

    private void showList() {
        String[] names = new String[list.size()];
        for (int i = 0; i < names.length; i++) {
            Dhikr d = list.get(i);
            String t = d.text.length() > 44 ? d.text.substring(0, 44) + "…" : d.text;
            names[i] = (done(d) ? "✓ " : (i + 1) + ". ") + t;
        }
        Ui.show(ui.dialog().setTitle("قائمة الأذكار").setItems(names, (dlg, which) -> go(which)));
    }

    private void confirmReset() {
        Ui.show(ui.dialog().setTitle("إعادة التقدم")
            .setMessage("هل تريد مسح تقدم " + (period == 0 ? "أذكار الصباح" : "أذكار المساء") + " لليوم؟")
            .setPositiveButton("نعم، إعادة", (dlg, w) -> {
                android.content.SharedPreferences.Editor e = st.prefs.edit();
                for (Dhikr d : list) e.remove(key(d));
                e.apply();
                idx = 0;
                render();
            })
            .setNegativeButton("إلغاء", null));
    }

    @Override public void onShow() {
        if (st.resumeAdhkar) idx = firstIncomplete();
        render();
    }

    @Override public void onHide() { handler.removeCallbacksAndMessages(null); }
}
