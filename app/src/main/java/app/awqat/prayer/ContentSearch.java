package app.awqat.prayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Search over hadiths and remembrances. Arabic is normalised for matching only; displayed text is untouched. */
public final class ContentSearch {
    private ContentSearch() {}

    private static final Map<String, String> DHIKR_KEYS = new HashMap<>();

    private static String key(Dhikr d) {
        String k = DHIKR_KEYS.get(d.id);
        if (k == null) {
            k = ArabicText.normalize(d.text + " " + d.source + " " + d.grade + " " + d.countNote + " "
                + ("morning".equals(d.period) ? "أذكار الصباح" : "أذكار المساء") + " " + d.typeLabel());
            DHIKR_KEYS.put(d.id, k);
        }
        return k;
    }

    /** category: a code from HadithData.CATEGORY_CODES, or null/"" for all. */
    public static List<Hadith> hadith(String query, String category) {
        List<Hadith> out = new ArrayList<>();
        for (Hadith h : HadithData.ALL) {
            if (category != null && !category.isEmpty() && !h.inCategory(category)) continue;
            if (ArabicText.matches(h.searchKey, query)) out.add(h);
        }
        return out;
    }

    public static List<Dhikr> dhikr(String query) {
        List<Dhikr> out = new ArrayList<>();
        if (query == null || ArabicText.normalize(query).isEmpty()) return out;   // only on an actual query
        for (Dhikr d : AdhkarData.ALL) if (ArabicText.matches(key(d), query)) out.add(d);
        return out;
    }

    public static Hadith hadithById(String id) {
        for (Hadith h : HadithData.ALL) if (h.id.equals(id)) return h;
        return null;
    }

    public static Dhikr dhikrById(String id) {
        for (Dhikr d : AdhkarData.ALL) if (d.id.equals(id)) return d;
        return null;
    }
}
