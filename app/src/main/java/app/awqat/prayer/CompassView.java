package app.awqat.prayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Rotating dial with the Kaaba marker. The red triangle at the top is the direction you are facing. */
final class CompassView extends View {
    private final Themes.Palette p;
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tri = new Path();
    private float heading, qibla;
    private boolean aligned;

    CompassView(Context c, Themes.Palette p) {
        super(c);
        this.p = p;
        this.density = c.getResources().getDisplayMetrics().density;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
    }

    void setHeading(float h) { heading = h; invalidate(); }
    void setQibla(float q) { qibla = q; invalidate(); }
    void setAligned(boolean a) { if (a != aligned) { aligned = a; invalidate(); } }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int side = Math.min(w, Math.round(300 * density));
        setMeasuredDimension(side, side);
    }

    @Override protected void onDraw(Canvas c) {
        float side = Math.min(getWidth(), getHeight());
        float s = side / 240f, cx = getWidth() / 2f, cy = getHeight() / 2f;

        fill.setColor(p.danger);                       // fixed pointer: where you are facing
        tri.reset();
        tri.moveTo(cx, cy - 118 * s);
        tri.lineTo(cx + 9 * s, cy - 103 * s);
        tri.lineTo(cx - 9 * s, cy - 103 * s);
        tri.close();
        c.drawPath(tri, fill);

        fill.setColor(p.card);
        c.drawCircle(cx, cy, 100 * s, fill);
        stroke.setColor(aligned ? p.ok : p.line);
        stroke.setStrokeWidth((aligned ? 6 : 3) * density);
        c.drawCircle(cx, cy, 100 * s, stroke);

        c.save();
        c.rotate(-heading, cx, cy);                    // the dial turns opposite to the phone
        stroke.setColor(p.muted);
        stroke.setStrokeWidth(1.5f * density);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30.0);
            float r1 = (i % 3 == 0 ? 80 : 84) * s, r2 = (i % 3 == 0 ? 94 : 90) * s;
            c.drawLine(cx + r1 * (float) Math.sin(a), cy - r1 * (float) Math.cos(a),
                cx + r2 * (float) Math.sin(a), cy - r2 * (float) Math.cos(a), stroke);
        }
        label.setColor(p.muted);
        label.setTextSize(15 * s);
        Paint.FontMetrics fm = label.getFontMetrics();
        c.drawText("شمال", cx, cy - 68 * s - (fm.ascent + fm.descent) / 2f, label);

        c.save();
        c.rotate(qibla, cx, cy);                       // Kaaba marker at the Qibla bearing
        stroke.setColor(p.danger);
        stroke.setStrokeWidth(3 * density);
        c.drawLine(cx, cy, cx, cy - 76 * s, stroke);
        fill.setColor(p.ink);
        c.drawRect(cx - 12 * s, cy - 98 * s, cx + 12 * s, cy - 74 * s, fill);
        fill.setColor(p.accent2);
        c.drawRect(cx - 12 * s, cy - 91 * s, cx + 12 * s, cy - 87 * s, fill);
        c.restore();

        fill.setColor(p.danger);
        c.drawCircle(cx, cy, 6 * s, fill);
        c.restore();
    }
}
