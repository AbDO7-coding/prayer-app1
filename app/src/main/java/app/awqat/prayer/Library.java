package app.awqat.prayer;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Favourites and recently viewed items (ids of hadiths and remembrances). */
final class Library {
    private final SharedPreferences prefs;

    Library(AppState st) { prefs = st.prefs; }

    Set<String> favs() { return new HashSet<>(prefs.getStringSet("fav", new HashSet<String>())); }

    boolean isFav(String id) { return favs().contains(id); }

    /** Returns true when the item is now a favourite. */
    boolean toggleFav(String id) {
        Set<String> s = favs();
        boolean now = !s.remove(id);
        if (now) s.add(id);
        prefs.edit().putStringSet("fav", s).apply();
        return now;
    }

    List<String> recents() {
        List<String> out = new ArrayList<>();
        String s = prefs.getString("recent", "");
        if (s != null && !s.isEmpty()) for (String x : s.split(",")) if (!x.isEmpty()) out.add(x);
        return out;
    }

    void addRecent(String id) {
        List<String> r = recents();
        r.remove(id);
        r.add(0, id);
        while (r.size() > 20) r.remove(r.size() - 1);
        StringBuilder b = new StringBuilder();
        for (String x : r) {
            if (b.length() > 0) b.append(',');
            b.append(x);
        }
        prefs.edit().putString("recent", b.toString()).apply();
    }
}
