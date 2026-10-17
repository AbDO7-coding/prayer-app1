package app.awqat.prayer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small view factory so every screen shares the same design tokens. */
final class Ui {
    static final int WHITE = 0xFFFFFFFF;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    final Context ctx;
    final Themes.Palette p;
    final float density;
    final boolean reduced;

    Ui(Context c, Themes.Palette p) {
        this.ctx = c;
        this.p = p;
        this.density = c.getResources().getDisplayMetrics().density;
        this.reduced = reducedMotion(c);
    }

    static boolean reducedMotion(Context c) {
        try {
            return Settings.Global.getFloat(c.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    int dp(float v) { return Math.round(v * density); }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    GradientDrawable box(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(1)), stroke);
        return g;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    /** Layout params with margins in dp, relative to the reading direction (start/end). */
    LinearLayout.LayoutParams m(int w, int h, float start, float top, float end, float bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.setMarginStart(dp(start));
        lp.setMarginEnd(dp(end));
        lp.topMargin = dp(top);
        lp.bottomMargin = dp(bottom);
        return lp;
    }

    LinearLayout.LayoutParams weight(float wt, float start, float end) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, wt);
        lp.setMarginStart(dp(start));
        lp.setMarginEnd(dp(end));
        return lp;
    }

    LinearLayout card() {
        LinearLayout l = col();
        l.setBackground(box(p.card, p.line, 16));
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        l.setLayoutParams(m(MATCH, WRAP, 0, 0, 0, 12));
        return l;
    }

    TextView button(String label, boolean strong, View.OnClickListener l) {
        TextView b = text(label, 15, strong ? WHITE : p.accent, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), dp(11), dp(12), dp(11));
        b.setBackground(box(strong ? p.heroA : p.hi, 0, 12));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    TextView chip(String label, boolean selected, View.OnClickListener l) {
        TextView b = text(label, 13, selected ? WHITE : p.ink, selected);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setBackground(box(selected ? p.heroA : p.hi, 0, 18));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    View space(float hDp) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(hDp)));
        return v;
    }

    AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(ctx, p.dark ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    static AlertDialog show(AlertDialog.Builder b) {
        AlertDialog d = b.create();
        d.show();
        if (d.getWindow() != null) d.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return d;
    }
}
