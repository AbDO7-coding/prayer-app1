package app.awqat.prayer;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.ColorStateList;

import java.time.LocalDate;
import java.util.Locale;

/** Prayer times, the next-prayer countdown with the sun element, the Hadith of the day, and the offsets panel. */
final class HomeScreen implements Screen {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Library lib;
    private final Themes.Palette p;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LocationHelper locator = new LocationHelper();

    private LocalDate day, todayCached;
    private String lastKey = "";
    private boolean busy, detailsOpen, shown;
    private int hadithOffset;

    private TextView lNext, lCd, lAt, lSr, lSs, lDate, lErr, bCountry, bCity, bDetails, lNote;
    private TextView hadithBody, hadithRef, hadithGrade;
    private SkyView sky;
    private LinearLayout table, details, ramRow;
    private CheckBox ramCheck;
    private final EditText[] adjIn = new EditText[6];
    private Hadith current;

    private final Runnable tickRun = new Runnable() {
        @Override public void run() {
            if (!shown) return;
            tick();
            handler.postDelayed(this, 1000);
        }
    };

    HomeScreen(MainActivity act, AppState st, Ui ui, Library lib) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.lib = lib;
        this.p = ui.p;
        long ed = LocalDate.now().toEpochDay();
        int off = st.prefs.getInt("hod_off", 0);
        off = (st.prefs.getLong("hod_day", -1) == ed) ? off + 1 : 0;   // a new session shows a different hadith
        hadithOffset = off;
        st.prefs.edit().putLong("hod_day", ed).putInt("hod_off", off).apply();
        current = pickHadith();
    }

    private Hadith pickHadith() {
        int n = HadithData.ALL.length;
        return HadithData.ALL[DailyPick.index(LocalDate.now().toEpochDay(), n, hadithOffset)];
    }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));

        root.addView(ui.text("أوقات الصلاة", 28, p.accent, true));
        root.addView(ui.text("في مدن المغرب ومصر، حسب طريقة الجهة الرسمية في كل بلد", 13, p.muted, false),
            ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 14));
        if (st.startupCard) root.addView(buildHadithCard());
        root.addView(buildHero());
        root.addView(buildSelector());
        lErr = ui.text("", 13, p.danger, false);
        lErr.setVisibility(View.GONE);
        root.addView(lErr, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        root.addView(buildTimesCard());
        bDetails = ui.button("الطريقة والضبط  ▾", false, v -> toggleDetails());
        root.addView(bDetails, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        details = buildDetails();
        details.setVisibility(View.GONE);
        root.addView(details);
        sv.addView(root);
        day = PrayerCalc.localDate(st.country, System.currentTimeMillis() / 1000);
        todayCached = day;
        refreshAll();
        return sv;
    }

    // ------------------------------------------------------------ Hadith of the day
    private View buildHadithCard() {
        LinearLayout card = ui.card();
        card.setBackground(ui.box(p.card, p.accent2, 16));
        card.addView(ui.text("حديث اليوم", 13, p.accent, true));
        hadithBody = ui.text("", 18, p.ink, false);
        hadithBody.setLineSpacing(0f, 1.45f);
        card.addView(hadithBody, ui.m(Ui.MATCH, Ui.WRAP, 0, 8, 0, 8));
        hadithRef = ui.text("", 12, p.muted, false);
        card.addView(hadithRef);
        hadithGrade = ui.text("", 12, p.ok, true);
        card.addView(hadithGrade, ui.m(Ui.MATCH, Ui.WRAP, 0, 2, 0, 10));
        LinearLayout actions = ui.row();
        actions.addView(ui.button("نسخ", false, v -> HadithUi.copy(act, HadithUi.shareText(current))), ui.weight(1, 0, 6));
        actions.addView(ui.button("مشاركة", false, v -> HadithUi.share(act, HadithUi.shareText(current))), ui.weight(1, 0, 6));
        final TextView fav = ui.button(lib.isFav(current.id) ? "محفوظ" : "حفظ", false, null);
        fav.setOnClickListener(v -> fav.setText(lib.toggleFav(current.id) ? "محفوظ" : "حفظ"));
        actions.addView(fav, ui.weight(1, 0, 6));
        actions.addView(ui.button("حديث آخر", false, v -> {
            hadithOffset++;
            st.prefs.edit().putInt("hod_off", hadithOffset).apply();
            current = pickHadith();
            bindHadith();
            fav.setText(lib.isFav(current.id) ? "محفوظ" : "حفظ");
        }), ui.weight(1.2f, 0, 0));
        card.addView(actions);
        card.setOnClickListener(v -> HadithUi.showHadith(act, st, ui, lib, current, null));
        bindHadith();
        if (!ui.reduced) {                       // subtle entrance animation
            card.setAlpha(0f);
            card.setTranslationY(ui.dp(14));
            card.animate().alpha(1f).translationY(0f).setDuration(380).setStartDelay(120).start();
        }
        return card;
    }

    private void bindHadith() {
        hadithBody.setText(current.text);
        hadithRef.setText(current.narrator + " — " + current.reference());
        hadithGrade.setText("الدرجة: " + HadithUi.gradeLine(current));
    }

    // ------------------------------------------------------------ hero with the sun
    private View buildHero() {
        FrameLayout hero = new FrameLayout(act);
        hero.setLayoutParams(ui.m(Ui.MATCH, ui.dp(360), 0, 0, 0, 12));
        sky = new SkyView(act, p, ui.dp(22), ui.reduced);
        hero.addView(sky, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        LinearLayout over = ui.col();
        over.setGravity(Gravity.CENTER_HORIZONTAL);
        over.setPadding(ui.dp(16), ui.dp(20), ui.dp(16), 0);
        over.addView(ui.text("الصلاة القادمة", 14, 0xD9FFFFFF, false));
        lNext = ui.text("", 40, Ui.WHITE, true);
        over.addView(lNext);
        lCd = ui.text("00:00:00", 44, Ui.WHITE, false);
        lCd.setTypeface(Typeface.DEFAULT);
        lCd.setFontFeatureSettings("tnum");
        lCd.setTextDirection(View.TEXT_DIRECTION_LTR);
        over.addView(lCd);
        lAt = ui.text("", 14, 0xD9FFFFFF, false);
        over.addView(lAt);
        hero.addView(over, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        LinearLayout ends = new LinearLayout(act);
        ends.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ends.setPadding(ui.dp(18), 0, ui.dp(18), ui.dp(8));
        lSr = ui.text("", 13, 0xE6FFFFFF, false);
        lSs = ui.text("", 13, 0xE6FFFFFF, false);
        lSs.setGravity(Gravity.END);
        ends.addView(lSr, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        ends.addView(lSs, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM);
        hero.addView(ends, elp);
        return hero;
    }

    // ------------------------------------------------------------ country / city / location
    private View buildSelector() {
        LinearLayout card = ui.card();
        LinearLayout row = ui.row();
        bCountry = ui.button("", false, v -> chooseCountry());
        bCity = ui.button("", false, v -> chooseCity());
        row.addView(bCountry, ui.weight(1f, 0, 8));
        row.addView(bCity, ui.weight(1.4f, 0, 8));
        row.addView(ui.button("موقعي", true, v -> onMyLocation()), new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
        card.addView(row);
        return card;
    }

    private void chooseCountry() {
        final String[] names = {PrayerCalc.MOROCCO.label, PrayerCalc.EGYPT.label};
        Ui.show(ui.dialog().setTitle("البلد").setItems(names, (d, which) -> {
            PrayerCalc.Country c = PrayerCalc.COUNTRIES[which];
            if (c != st.country) {
                st.setCountry(c, true);
                day = PrayerCalc.localDate(c, System.currentTimeMillis() / 1000);
                todayCached = day;
                setErr("");
                refreshAll();
            }
        }));
    }

    private void chooseCity() {
        final PrayerCalc.City[] cities = st.country.cities;
        final int shift = st.hasGps ? 1 : 0;
        String[] names = new String[cities.length + shift];
        if (st.hasGps) names[0] = "موقعي الحالي";
        for (int i = 0; i < cities.length; i++) names[i + shift] = cities[i].name;
        Ui.show(ui.dialog().setTitle("المدينة").setItems(names, (d, which) -> {
            if (st.hasGps && which == 0) st.useGps = true;
            else st.setCity(which - shift);
            setErr("");
            refreshAll();
        }));
    }

    private void onMyLocation() {
        setErr("");
        act.requestLocation(this::fetchLocation,
            () -> setErr("لم يتم منح إذن الموقع. اختر مدينة أو فعّله من إعدادات التطبيق."));
    }

    private void fetchLocation() {
        setErr("جارٍ تحديد موقعك…");
        locator.request(act, new LocationHelper.Callback() {
            @Override public void onFix(double lat, double lng) {
                setErr("");
                st.setGps(lat, lng);
                refreshAll();
            }
            @Override public void onFail(String message) { setErr(message); }
        });
    }

    private void setErr(String s) {
        lErr.setText(s);
        lErr.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------ times table
    private View buildTimesCard() {
        LinearLayout card = ui.card();
        LinearLayout nav = ui.row();
        nav.addView(ui.button("اليوم السابق", false, v -> shift(-1)), ui.weight(1, 0, 6));
        nav.addView(ui.button("اليوم", false, v -> goToday()), ui.weight(0.7f, 0, 6));
        nav.addView(ui.button("اليوم التالي", false, v -> shift(1)), ui.weight(1, 0, 0));
        card.addView(nav);
        lDate = ui.text("", 17, p.ink, true);
        card.addView(lDate, ui.m(Ui.MATCH, Ui.WRAP, 0, 12, 0, 4));
        table = ui.col();
        card.addView(table);
        return card;
    }

    private void shift(int n) { day = day.plusDays(n); renderTable(); }

    private void goToday() {
        day = PrayerCalc.localDate(st.country, System.currentTimeMillis() / 1000);
        renderTable();
    }

    private void renderTable() {
        double[] ll = st.latLng();
        long[] t = PrayerCalc.compute(day, ll[0], ll[1], st.country, st.adj, st.ramadan);
        lDate.setText(PrayerCalc.fmtDate(st.country, day));
        long now = System.currentTimeMillis() / 1000;
        int hi = -1;
        if (day.equals(PrayerCalc.localDate(st.country, now))) {
            PrayerCalc.Next n = PrayerCalc.next(st.country, ll[0], ll[1], st.adj, st.ramadan, now);
            if (!n.tomorrow) hi = n.index;
        }
        table.removeAllViews();
        for (int i = 0; i < 6; i++) {
            boolean on = i == hi;
            LinearLayout r = ui.row();
            if (on) r.setBackground(ui.box(p.hi, 0, 10));
            r.setPadding(ui.dp(4), ui.dp(12), ui.dp(12), ui.dp(12));
            View bar = new View(act);
            bar.setBackgroundColor(on ? p.danger : 0x00000000);
            r.addView(bar, new LinearLayout.LayoutParams(ui.dp(4), ui.dp(26)));
            TextView name = ui.text(PrayerCalc.NAMES[i], 22, i == PrayerCalc.SUNRISE ? p.muted : p.ink, true);
            r.addView(name, ui.weight(1f, 12, 0));
            TextView time = ui.text(PrayerCalc.fmtTime(st.country, t[i]), 22, i == PrayerCalc.SUNRISE ? p.muted : p.ink, false);
            time.setFontFeatureSettings("tnum");
            time.setTextDirection(View.TEXT_DIRECTION_LTR);
            r.addView(time);
            table.addView(r);
            if (i < 5) {
                View line = new View(act);
                line.setBackgroundColor(p.line);
                table.addView(line, new LinearLayout.LayoutParams(Ui.MATCH, 1));
            }
        }
    }

    // ------------------------------------------------------------ offsets, Ramadan, method note
    private LinearLayout buildDetails() {
        LinearLayout box = ui.card();
        lNote = ui.text("", 14, p.muted, false);
        box.addView(lNote);
        LinearLayout r1 = ui.row(), r2 = ui.row();
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            LinearLayout cell = ui.col();
            cell.addView(ui.text(PrayerCalc.NAMES[i], 13, p.muted, false));
            EditText e = new EditText(act);
            e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            e.setTextColor(p.ink);
            e.setTextSize(16);
            e.setGravity(Gravity.CENTER);
            e.setTextDirection(View.TEXT_DIRECTION_LTR);
            e.setBackground(ui.box(p.hi, 0, 10));
            e.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8));
            e.setSingleLine(true);
            e.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (busy) return;
                    int v;
                    try { v = Math.max(-60, Math.min(60, Integer.parseInt(s.toString().trim()))); }
                    catch (NumberFormatException ex) { v = 0; }
                    st.adj[idx] = v;
                    lastKey = "";
                    renderTable();
                    tick();
                }
            });
            adjIn[i] = e;
            cell.addView(e, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
            (i < 3 ? r1 : r2).addView(cell, ui.weight(1f, 0, i % 3 == 2 ? 0 : 8));
        }
        box.addView(r1, ui.m(Ui.MATCH, Ui.WRAP, 0, 12, 0, 8));
        box.addView(r2);
        ramRow = ui.row();
        ramCheck = new CheckBox(act);
        ramCheck.setButtonTintList(ColorStateList.valueOf(p.accent));
        ramCheck.setOnCheckedChangeListener((b, on) -> {
            if (busy) return;
            st.ramadan = on && st.country.ramadanOption;
            lastKey = "";
            renderTable();
            tick();
        });
        ramRow.addView(ramCheck);
        ramRow.addView(ui.text("رمضان: العشاء بعد 15 دقيقة", 15, p.ink, false));
        box.addView(ramRow, ui.m(Ui.MATCH, Ui.WRAP, 0, 8, 0, 8));
        box.addView(ui.button("إعادة الضبط", false, v -> {
            st.adj = st.country.adj.clone();
            st.ramadan = false;
            syncInputs();
            lastKey = "";
            renderTable();
            tick();
        }));
        box.addView(ui.text("قد تختلف النتائج دقيقة أو دقيقتين عن الجدول الرسمي. للعبادة، اعتمد مسجد حيّك أو الجهة الرسمية في بلدك.",
            13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));
        return box;
    }

    private void toggleDetails() {
        detailsOpen = !detailsOpen;
        details.setVisibility(detailsOpen ? View.VISIBLE : View.GONE);
        bDetails.setText(detailsOpen ? "الطريقة والضبط  ▴" : "الطريقة والضبط  ▾");
    }

    private void syncInputs() {
        busy = true;
        for (int i = 0; i < 6; i++) adjIn[i].setText(String.valueOf(st.adj[i]));
        ramCheck.setChecked(st.ramadan);
        ramRow.setVisibility(st.country.ramadanOption ? View.VISIBLE : View.GONE);   // hidden for Egypt
        busy = false;
    }

    private void refreshAll() {
        bCountry.setText("البلد: " + st.country.label);
        bCity.setText(st.useGps && st.hasGps ? "موقعي الحالي" : st.country.cities[st.cityIdx].name);
        lNote.setText(st.country.note);
        syncInputs();
        lastKey = "";
        renderTable();
        tick();
    }

    // ------------------------------------------------------------ live countdown and sun
    private void tick() {
        if (lNext == null) return;
        long now = System.currentTimeMillis() / 1000;
        double[] ll = st.latLng();
        PrayerCalc.Next n = PrayerCalc.next(st.country, ll[0], ll[1], st.adj, st.ramadan, now);
        LocalDate ld = PrayerCalc.localDate(st.country, now);
        if (!ld.equals(todayCached)) {                 // local midnight passed
            if (day.equals(todayCached)) day = ld;
            todayCached = ld;
            lastKey = "";
        }
        lNext.setText(PrayerCalc.NAMES[n.index]);
        long diff = Math.max(0, n.epoch - now);
        lCd.setText(String.format(Locale.ENGLISH, "%02d:%02d:%02d", diff / 3600, diff / 60 % 60, diff % 60));
        lAt.setText((n.tomorrow ? "غدًا" : "اليوم") + " على الساعة " + PrayerCalc.fmtTime(st.country, n.epoch));
        double[] s = PrayerCalc.sunState(n.today, now);
        sky.setState(s[0], s[1] == 1, s[2]);
        lSr.setText("الشروق " + PrayerCalc.fmtTime(st.country, n.today[PrayerCalc.SUNRISE]));
        lSs.setText("المغرب " + PrayerCalc.fmtTime(st.country, n.today[PrayerCalc.MAGHRIB]));
        String key = n.index + "|" + n.tomorrow + "|" + day;
        if (!key.equals(lastKey)) {
            lastKey = key;
            renderTable();
        }
    }

    @Override public void onShow() {
        shown = true;
        handler.removeCallbacks(tickRun);
        handler.post(tickRun);
    }

    @Override public void onHide() {
        shown = false;
        handler.removeCallbacks(tickRun);
        locator.cancel();
    }
}
