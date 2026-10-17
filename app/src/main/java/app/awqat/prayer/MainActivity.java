package app.awqat.prayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** App shell: applies the saved theme before the first frame, hosts the six screens and the bottom navigation. */
public class MainActivity extends Activity {
    private static final int REQ_LOC = 11;
    private static final String[] LABELS = {"المواقيت", "الأذكار", "المسبحة", "الأحاديث", "القبلة", "الإعدادات"};

    private AppState st;
    private Ui ui;
    private Library lib;
    private Screen[] screens = new Screen[6];
    private View[] views = new View[6];
    private int tab = 0;
    private boolean paused;
    private LinearLayout root, navBar;
    private FrameLayout content;
    private NavIconView[] icons;
    private TextView[] labels;
    private Runnable pendingGranted, pendingDenied;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new AppState(this);                 // reads the saved theme first, so there is no flash of another theme
        lib = new Library(st);
        applyWindow();
        buildShell();
        show(0, false);
        if (!st.prefs.getBoolean("asked_loc", false)) {      // ask for the location permission on first launch
            st.putBool("asked_loc", true);
            requestLocation(null, null);
        }
    }

    private void applyWindow() {
        Themes.Palette pal = st.pal;
        Window w = getWindow();
        w.getDecorView().setBackgroundColor(pal.bg);
        w.setStatusBarColor(pal.bg);
        w.setNavigationBarColor(pal.card);
        int flags = 0;
        if (!pal.dark) flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    private void buildShell() {
        Themes.Palette pal = st.pal;
        ui = new Ui(this, pal);
        screens = new Screen[6];
        views = new View[6];
        root = ui.col();
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(pal.bg);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        LinearLayout nav = ui.col();
        nav.setBackgroundColor(pal.card);
        View line = new View(this);
        line.setBackgroundColor(pal.line);
        nav.addView(line, new LinearLayout.LayoutParams(Ui.MATCH, 1));
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.HORIZONTAL);
        icons = new NavIconView[6];
        labels = new TextView[6];
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            LinearLayout item = ui.col();
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(0, ui.dp(8), 0, ui.dp(8));
            icons[i] = new NavIconView(this, i);
            item.addView(icons[i], new LinearLayout.LayoutParams(ui.dp(24), ui.dp(24)));
            labels[i] = ui.text(LABELS[i], 10.5f, pal.muted, false);
            labels[i].setGravity(Gravity.CENTER);
            labels[i].setSingleLine(true);
            item.addView(labels[i], ui.m(Ui.WRAP, Ui.WRAP, 0, 3, 0, 0));
            item.setClickable(true);
            item.setOnClickListener(v -> show(idx, true));
            items.addView(item, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        }
        nav.addView(items);
        navBar = nav;
        root.addView(nav, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        setContentView(root);
    }

    private Screen create(int i) {
        switch (i) {
            case 0: return new HomeScreen(this, st, ui, lib);
            case 1: return new AdhkarScreen(this, st, ui, lib);
            case 2: return new TasbeehScreen(this, st, ui);
            case 3: return new HadithScreen(this, st, ui, lib);
            case 4: return new QiblaScreen(this, st, ui);
            default: return new SettingsScreen(this, st, ui);
        }
    }

    private void show(int i, boolean animate) {
        if (screens[tab] != null) screens[tab].onHide();
        tab = i;
        if (screens[i] == null) {
            screens[i] = create(i);
            views[i] = screens[i].build();
        }
        content.removeAllViews();
        content.addView(views[i], new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        if (animate && !ui.reduced) {
            views[i].setAlpha(0f);
            views[i].animate().alpha(1f).setDuration(160).start();
        }
        for (int k = 0; k < 6; k++) {
            int c = k == i ? st.pal.accent : st.pal.muted;
            icons[k].setColor(c);
            labels[k].setTextColor(c);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
        screens[i].onShow();
    }

    /** Called by the settings screen after a theme or appearance change. */
    void onThemeChanged() {
        final int keep = tab;
        if (screens[tab] != null) screens[tab].onHide();
        final Runnable swap = () -> {
            st.reloadPalette();
            applyWindow();
            buildShell();
            show(keep, false);
            if (ui.reduced) {
                root.setAlpha(1f);
            } else {
                root.setAlpha(0f);
                root.animate().alpha(1f).setDuration(220).start();
            }
        };
        if (ui.reduced || root == null) swap.run();
        else root.animate().alpha(0f).setDuration(120).withEndAction(swap).start();
    }

    /** Drops a cached screen so it is rebuilt the next time it is shown (e.g. after a setting changed). */
    void invalidateScreen(int i) {
        if (i != tab) {
            screens[i] = null;
            views[i] = null;
        }
    }

    void setNavVisible(boolean visible) { navBar.setVisibility(visible ? View.VISIBLE : View.GONE); }

    /** Runs granted when the location permission is available, denied otherwise. Either may be null. */
    void requestLocation(Runnable granted, Runnable denied) {
        if (LocationHelper.hasPermission(this)) {
            if (granted != null) granted.run();
            return;
        }
        pendingGranted = granted;
        pendingDenied = denied;
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQ_LOC) return;
        boolean ok = false;
        for (int r : results) if (r == PackageManager.PERMISSION_GRANTED) ok = true;
        Runnable cb = ok ? pendingGranted : pendingDenied;
        pendingGranted = null;
        pendingDenied = null;
        if (cb != null) cb.run();
    }

    @Override protected void onPause() {
        super.onPause();
        paused = true;
        if (screens[tab] != null) screens[tab].onHide();
    }

    @Override protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
            if (screens[tab] != null) screens[tab].onShow();
        }
    }
}
