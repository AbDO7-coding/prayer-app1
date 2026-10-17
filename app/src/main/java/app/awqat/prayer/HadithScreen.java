package app.awqat.prayer;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Search, categories, favourites and recently viewed across the hadith and remembrance database (offline). */
final class HadithScreen implements Screen {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Library lib;
    private final Themes.Palette p;

    private String query = "";
    private String filter = "all";            // all, fav, recent, or a category code
    private LinearLayout list, chips;
    private TextView lCount;

    HadithScreen(MainActivity act, AppState st, Ui ui, Library lib) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.lib = lib;
        this.p = ui.p;
    }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        sv.setFillViewport(true);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));
        root.addView(ui.text("الأحاديث والأذكار", 28, p.accent, true));
        root.addView(ui.text("بحث دون اتصال في نص الحديث أو المصدر أو التصنيف", 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        EditText search = new EditText(act);
        search.setHint("ابحث هنا");
        search.setHintTextColor(p.muted);
        search.setTextColor(p.ink);
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackground(ui.box(p.card, p.line, 14));
        search.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                rebuildList();
            }
        });
        root.addView(search, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));

        HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        chips = ui.row();
        hs.addView(chips);
        root.addView(hs, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));

        lCount = ui.text("", 13, p.muted, false);
        root.addView(lCount, ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        list = ui.col();
        root.addView(list);
        sv.addView(root);
        rebuildChips();
        rebuildList();
        return sv;
    }

    private void rebuildChips() {
        chips.removeAllViews();
        addChip("الكل", "all");
        addChip("المفضلة", "fav");
        addChip("الأخيرة", "recent");
        for (int i = 0; i < HadithData.CATEGORY_CODES.length; i++) addChip(HadithData.CATEGORY_NAMES[i], HadithData.CATEGORY_CODES[i]);
    }

    private void addChip(String label, final String code) {
        chips.addView(ui.chip(label, code.equals(filter), v -> {
            filter = code;
            rebuildChips();
            rebuildList();
        }), ui.m(Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
    }

    private void rebuildList() {
        if (list == null) return;
        list.removeAllViews();
        List<Hadith> hs = new ArrayList<>();
        List<Dhikr> ds = new ArrayList<>();
        Set<String> dhikrMatch = new HashSet<>();
        for (Dhikr d : ContentSearch.dhikr(query)) dhikrMatch.add(d.id);
        boolean noQuery = ArabicText.normalize(query).isEmpty();
        if (filter.equals("fav") || filter.equals("recent")) {
            List<String> ids = new ArrayList<>();
            if (filter.equals("recent")) ids.addAll(lib.recents());
            else ids.addAll(lib.favs());
            for (String id : ids) {
                Hadith h = ContentSearch.hadithById(id);
                if (h != null && ArabicText.matches(h.searchKey, query)) hs.add(h);
                Dhikr d = ContentSearch.dhikrById(id);
                if (d != null && (noQuery || dhikrMatch.contains(d.id))) ds.add(d);
            }
        } else {
            hs = ContentSearch.hadith(query, filter.equals("all") ? null : filter);
            if (filter.equals("all")) ds = ContentSearch.dhikr(query);
        }
        lCount.setText("النتائج: " + (hs.size() + ds.size()) + " (من " + HadithData.ALL.length + " حديثًا و " + AdhkarData.ALL.length + " ذكرًا)");
        if (hs.isEmpty() && ds.isEmpty()) {
            LinearLayout empty = ui.card();
            empty.addView(ui.text(filter.equals("fav") ? "لا توجد عناصر في المفضلة بعد" : filter.equals("recent") ? "لم تعرض أي عنصر بعد" : "لا توجد نتائج مطابقة", 16, p.ink, true));
            empty.addView(ui.text("جرّب كلمة أخرى، أو اختر تصنيف «الكل»", 13, p.muted, false), ui.m(Ui.MATCH, Ui.WRAP, 0, 4, 0, 0));
            list.addView(empty);
            return;
        }
        for (Hadith h : hs) list.addView(hadithCard(h));
        for (Dhikr d : ds) list.addView(dhikrCard(d));
    }

    private View hadithCard(final Hadith h) {
        LinearLayout c = ui.card();
        c.addView(ui.text(h.gradeText + (lib.isFav(h.id) ? "  —  محفوظ" : ""), 12, p.ok, true));
        TextView body = ui.text(h.text, 17, p.ink, false);
        body.setLineSpacing(0f, 1.4f);
        body.setMaxLines(4);
        body.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(body, ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 6));
        c.addView(ui.text(h.narrator + " — " + h.collection + " " + h.number, 12, p.muted, false));
        c.setOnClickListener(v -> HadithUi.showHadith(act, st, ui, lib, h, this::rebuildList));
        return c;
    }

    private View dhikrCard(final Dhikr d) {
        LinearLayout c = ui.card();
        c.addView(ui.text(("morning".equals(d.period) ? "أذكار الصباح" : "أذكار المساء") + " — " + d.typeLabel()
            + (lib.isFav(d.id) ? "  —  محفوظ" : ""), 12, p.accent, true));
        TextView body = ui.text(d.text, 17, p.ink, false);
        body.setLineSpacing(0f, 1.4f);
        body.setMaxLines(3);
        body.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(body, ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 6));
        c.addView(ui.text(d.source, 12, p.muted, false));
        c.setOnClickListener(v -> HadithUi.showDhikr(act, st, ui, lib, d, this::rebuildList));
        return c;
    }

    @Override public void onShow() { rebuildList(); }   // refresh favourites / recents after viewing elsewhere
}
