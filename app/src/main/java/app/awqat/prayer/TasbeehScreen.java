package app.awqat.prayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Digital Tasbeeh: 20 phrases, independent saved counts, targets, history, timer and a focus mode. */
final class TasbeehScreen implements Screen {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Themes.Palette p;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TasbeehLogic.TapGuard guard = new TasbeehLogic.TapGuard(25);

    private int phrase;
    private boolean focus, shown;
    private TextView lPhrase, lCount, lTarget, lRemain, lTimer, lDone, bExit;
    private RingView ring;
    private LinearLayout chrome, chipRow, phraseCard;
    private FrameLayout ringBox;

    private final Runnable timerRun = new Runnable() {
        @Override public void run() {
            if (!shown) return;
            updateTimer();
            handler.postDelayed(this, 1000);
        }
    };

    TasbeehScreen(MainActivity act, AppState st, Ui ui) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.p = ui.p;
        phrase = Math.max(0, Math.min(TasbeehData.PHRASES.length - 1, st.prefs.getInt("tp", 0)));
    }

    private int count() { return st.prefs.getInt("tc_" + phrase, 0); }
    private int target() { return st.prefs.getInt("tt_" + phrase, TasbeehData.DEFAULT_TARGET); }
    private long started() { return st.prefs.getLong("ts_" + phrase, 0L); }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));

        bExit = ui.button("خروج من وضع التركيز", false, v -> setFocus(false));
        bExit.setVisibility(View.GONE);
        root.addView(bExit, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));

        chrome = ui.col();
        chrome.addView(ui.text("المسبحة", 28, p.accent, true));
        chrome.addView(ui.text("عدّاد التسبيح. الهدف من اختيارك ولا يُقصد به عدد محدد", 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
        root.addView(chrome);

        phraseCard = ui.card();
        LinearLayout prow = ui.row();
        prow.addView(ui.button("›", false, v -> switchPhrase(phrase - 1)), new LinearLayout.LayoutParams(ui.dp(48), Ui.WRAP));
        lPhrase = ui.text("", 20, p.ink, true);
        lPhrase.setGravity(Gravity.CENTER);
        lPhrase.setLineSpacing(0f, 1.3f);
        prow.addView(lPhrase, ui.weight(1f, 8, 8));
        prow.addView(ui.button("‹", false, v -> switchPhrase(phrase + 1)), new LinearLayout.LayoutParams(ui.dp(48), Ui.WRAP));
        phraseCard.addView(prow);
        phraseCard.addView(ui.button("اختيار الذكر من القائمة", false, v -> choosePhrase()), ui.m(Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));
        root.addView(phraseCard);

        ringBox = new FrameLayout(act);
        ring = new RingView(act, p.hi, p.accent, p.ok, ui.dp(12));
        ringBox.addView(ring, new FrameLayout.LayoutParams(ui.dp(250), ui.dp(250), Gravity.CENTER));
        LinearLayout center = ui.col();
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        lCount = ui.text("0", 80, p.ink, true);
        lCount.setTextDirection(View.TEXT_DIRECTION_LTR);
        lCount.setFontFeatureSettings("tnum");
        center.addView(lCount);
        lTarget = ui.text("", 14, p.muted, false);
        center.addView(lTarget);
        ringBox.addView(center, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.CENTER));
        ringBox.setClickable(true);
        // Count on finger-down so rapid taps are responsive; one touch = exactly one count.
        // Because the touch listener consumes the gesture, onClick only fires for accessibility (TalkBack).
        ringBox.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) tap(v);
            return a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL;
        });
        ringBox.setOnClickListener(v -> tap(v));
        root.addView(ringBox, ui.m(Ui.MATCH, ui.dp(266), 0, 6, 0, 6));
        root.addView(ui.text("اضغط على الدائرة للتسبيح", 13, p.muted, false), ui.m(Ui.WRAP, Ui.WRAP, 0, 0, 0, 8));
        ((LinearLayout.LayoutParams) root.getChildAt(root.getChildCount() - 1).getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;

        LinearLayout info = ui.row();
        lRemain = ui.text("", 14, p.ink, true);
        lTimer = ui.text("", 14, p.muted, false);
        lTimer.setTextDirection(View.TEXT_DIRECTION_LTR);
        info.addView(lRemain, ui.weight(1f, 0, 0));
        info.addView(lTimer);
        root.addView(info, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 4));
        lDone = ui.text("اكتمل الهدف", 16, p.ok, true);
        lDone.setVisibility(View.GONE);
        root.addView(lDone, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));

        LinearLayout more = ui.col();
        more.addView(ui.text("الهدف", 13, p.muted, true), ui.m(Ui.MATCH, Ui.WRAP, 0, 8, 0, 6));
        chipRow = ui.row();
        more.addView(chipRow);
        LinearLayout controls = ui.row();
        controls.addView(ui.button("إعادة", false, v -> confirmReset()), ui.weight(1, 0, 6));
        controls.addView(ui.button("السجل", false, v -> showHistory()), ui.weight(1, 0, 6));
        controls.addView(ui.button("حجم العدّاد", false, v -> toggleSize()), ui.weight(1.2f, 0, 6));
        controls.addView(ui.button("تركيز", false, v -> setFocus(true)), ui.weight(1, 0, 0));
        more.addView(controls, ui.m(Ui.MATCH, Ui.WRAP, 0, 12, 0, 8));
        more.addView(toggle("اهتزاز عند العدّ", st.haptics, on -> { st.haptics = on; st.putBool("haptics", on); }));
        more.addView(toggle("صوت النقر", st.sound, on -> { st.sound = on; st.putBool("sound", on); }));
        root.addView(more);
        chromeRest = more;
        sv.addView(root);
        render();
        return sv;
    }

    private LinearLayout chromeRest;

    private interface OnToggle { void on(boolean checked); }

    private View toggle(String label, boolean initial, final OnToggle cb) {
        LinearLayout r = ui.row();
        CheckBox c = new CheckBox(act);
        c.setChecked(initial);
        c.setButtonTintList(ColorStateList.valueOf(p.accent));
        c.setOnCheckedChangeListener((b, on) -> cb.on(on));
        r.addView(c);
        r.addView(ui.text(label, 15, p.ink, false));
        return r;
    }

    private void tap(View src) {
        if (!guard.accept(SystemClock.uptimeMillis())) return;
        int c = count() + 1;
        SharedPreferences.Editor e = st.prefs.edit().putInt("tc_" + phrase, c);
        if (started() == 0L) e.putLong("ts_" + phrase, System.currentTimeMillis());
        e.apply();
        boolean hit = c == target();
        render();
        if (st.haptics) {
            if (hit) strongBuzz();
            else src.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (st.sound) src.playSoundEffect(SoundEffectConstants.CLICK);
    }

    private void strongBuzz() {
        try {
            Vibrator vib = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) { /* no vibrator */ }
    }

    private void render() {
        int c = count(), t = target();
        lPhrase.setText(TasbeehData.PHRASES[phrase]);
        lCount.setText(String.valueOf(c));
        lCount.setTextSize(st.compactCounter ? 48 : 80);
        lTarget.setText(t > 0 ? "الهدف " + t : "دون هدف");
        boolean reached = TasbeehLogic.reached(c, t);
        ring.set(TasbeehLogic.progress(c, t), reached);
        lRemain.setText(t > 0 ? "المتبقي: " + TasbeehLogic.remaining(c, t) : "");
        lDone.setVisibility(reached ? View.VISIBLE : View.GONE);
        lCount.setTextColor(reached ? p.ok : p.ink);
        chipRow.removeAllViews();
        int[] choices = TasbeehData.TARGET_CHOICES;
        boolean custom = true;
        for (int x : choices) if (x == t) custom = false;
        for (int x : choices) {
            final int v = x;
            chipRow.addView(ui.chip(String.valueOf(x), x == t, view -> setTarget(v)), ui.weight(1, 0, 6));
        }
        chipRow.addView(ui.chip(custom && t > 0 ? String.valueOf(t) : "مخصص", custom && t > 0, view -> customTarget()), ui.weight(1.3f, 0, 0));
        updateTimer();
    }

    private void updateTimer() {
        long s = started();
        lTimer.setText(s == 0L ? "" : "الوقت " + TasbeehLogic.duration((System.currentTimeMillis() - s) / 1000));
    }

    private void setTarget(int t) {
        st.prefs.edit().putInt("tt_" + phrase, t).apply();
        render();
    }

    private void customTarget() {
        final EditText e = new EditText(act);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setTextColor(p.ink);
        e.setHint("مثال: 500");
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        Ui.show(ui.dialog().setTitle("هدف مخصص").setView(e)
            .setPositiveButton("حفظ", (d, w) -> {
                try {
                    int v = Integer.parseInt(e.getText().toString().trim());
                    if (v > 0 && v <= 100000) setTarget(v);
                } catch (NumberFormatException ignored) { /* keep the current target */ }
            })
            .setNegativeButton("إلغاء", null));
    }

    private void switchPhrase(int i) {
        int n = TasbeehData.PHRASES.length;
        phrase = ((i % n) + n) % n;
        st.prefs.edit().putInt("tp", phrase).apply();
        render();
    }

    private void choosePhrase() {
        String[] items = new String[TasbeehData.PHRASES.length];
        for (int i = 0; i < items.length; i++) {
            int c = st.prefs.getInt("tc_" + i, 0);
            items[i] = TasbeehData.PHRASES[i] + (c > 0 ? "  (" + c + ")" : "");
        }
        Ui.show(ui.dialog().setTitle("اختر الذكر").setItems(items, (d, which) -> switchPhrase(which)));
    }

    private void toggleSize() {
        st.compactCounter = !st.compactCounter;
        st.putBool("compact", st.compactCounter);
        render();
    }

    private void setFocus(boolean on) {
        focus = on;
        int vis = on ? View.GONE : View.VISIBLE;
        chrome.setVisibility(vis);
        chromeRest.setVisibility(vis);
        bExit.setVisibility(on ? View.VISIBLE : View.GONE);
        act.setNavVisible(!on);
    }

    private void confirmReset() {
        final int c = count();
        if (c == 0) return;
        final long start = started();
        Ui.show(ui.dialog().setTitle("إعادة العدّاد")
            .setMessage("العدّ الحالي " + c + ". هل تريد إعادته إلى الصفر؟")
            .setPositiveButton("حفظ في السجل ثم إعادة", (d, w) -> {
                long dur = start == 0L ? 0L : (System.currentTimeMillis() - start) / 1000;
                List<long[]> h = TasbeehLogic.decodeHistory(st.prefs.getString("th", ""));
                h = TasbeehLogic.addToHistory(h, new long[]{System.currentTimeMillis() / 1000, phrase, c, dur}, 60);
                st.prefs.edit().putString("th", TasbeehLogic.encodeHistory(h)).apply();
                clearCount();
            })
            .setNeutralButton("إعادة دون حفظ", (d, w) -> clearCount())
            .setNegativeButton("إلغاء", null));
    }

    private void clearCount() {
        st.prefs.edit().putInt("tc_" + phrase, 0).putLong("ts_" + phrase, 0L).apply();
        render();
    }

    private void showHistory() {
        List<long[]> h = TasbeehLogic.decodeHistory(st.prefs.getString("th", ""));
        if (h.isEmpty()) {
            Ui.show(ui.dialog().setTitle("سجل الجلسات").setMessage("لا توجد جلسات محفوظة بعد").setPositiveButton("إغلاق", null));
            return;
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        List<String> items = new ArrayList<>();
        for (long[] e : h) {
            String when = LocalDateTime.ofInstant(Instant.ofEpochSecond(e[0]), ZoneId.systemDefault()).format(f);
            int pi = (int) Math.max(0, Math.min(TasbeehData.PHRASES.length - 1, e[1]));
            items.add(when + "\n" + TasbeehData.PHRASES[pi] + " — " + e[2] + " (" + TasbeehLogic.duration(e[3]) + ")");
        }
        Ui.show(ui.dialog().setTitle("سجل الجلسات").setItems(items.toArray(new String[0]), null)
            .setPositiveButton("إغلاق", null));
    }

    @Override public void onShow() {
        shown = true;
        handler.removeCallbacks(timerRun);
        handler.post(timerRun);
    }

    @Override public void onHide() {
        shown = false;
        handler.removeCallbacks(timerRun);
        if (focus) setFocus(false);
    }
}
