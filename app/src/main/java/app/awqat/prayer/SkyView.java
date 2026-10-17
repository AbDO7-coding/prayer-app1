package app.awqat.prayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

/**
 * The sun element: a themed sky that moves from sunrise to sunset, with a soft glow, drifting clouds,
 * dunes on the horizon, and a moon and stars at night. Drawn with gradients (no bitmaps), at about 8 frames
 * per second only while attached, and static when the system has animations turned off.
 */
final class SkyView extends View {
    private final Themes.Palette p;
    private final float cornerPx, density;
    private final boolean reduced;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path(), hills = new Path();
    private final RectF oval = new RectF();
    private final float[] starX = new float[40], starY = new float[40], starR = new float[40];
    private double prog = 0.5, night = 0;
    private boolean day = true;
    private float cloud = 0f;
    private boolean running;

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            cloud += 0.0008f;
            if (cloud > 1f) cloud -= 1f;
            invalidate();
            postDelayed(this, 120);
        }
    };

    SkyView(Context c, Themes.Palette p, float cornerPx, boolean reduced) {
        super(c);
        this.p = p;
        this.cornerPx = cornerPx;
        this.reduced = reduced;
        this.density = c.getResources().getDisplayMetrics().density;
        Random r = new Random(7);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = r.nextFloat();
            starY[i] = r.nextFloat() * 0.7f;
            starR[i] = 0.6f + r.nextFloat() * 1.2f;
        }
    }

    /** prog: 0..1 from sunrise to sunset; night: 0..1 from sunset to sunrise. */
    void setState(double prog, boolean day, double night) {
        this.prog = prog;
        this.day = day;
        this.night = night;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!reduced && !running) {
            running = true;
            postDelayed(frame, 120);
        }
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        removeCallbacks(frame);
        super.onDetachedFromWindow();
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int alpha(int color, int a) { return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24); }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        clip.reset();
        clip.addRoundRect(0, 0, w, h, cornerPx, cornerPx, Path.Direction.CW);
        c.save();
        c.clipPath(clip);

        // sky colours: theme gradient by day, deep blue at night, warm tint near the horizon
        float noonFactor = day ? (float) Math.sin(Math.PI * prog) : 0f;
        float dayAmt = day ? 0.35f + 0.65f * noonFactor : 0f;
        int nightTop = blend(p.heroB, 0xFF050814, 0.80f);
        int nightBottom = blend(p.heroB, 0xFF111A33, 0.55f);
        int top = blend(nightTop, p.heroA, dayAmt);
        int bottom = blend(nightBottom, p.heroB, dayAmt);
        if (day) bottom = blend(bottom, 0xFFFF9A5A, 0.35f * (1f - noonFactor));
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        if (!day) {
            paint.setColor(0xFFFFFFFF);
            for (int i = 0; i < starX.length; i++) {
                paint.setAlpha(110 + (i % 3) * 40);
                c.drawCircle(starX[i] * w, starY[i] * h, starR[i] * density, paint);
            }
        }

        float horizon = h * 0.88f, rx = w * 0.40f, ry = h * 0.30f, cx = w / 2f;
        double t = day ? prog : night;
        float sx = cx - rx * (float) Math.cos(Math.PI * t);
        float sy = horizon - ry * (float) Math.sin(Math.PI * t);

        // soft glow
        float glowR = Math.min(w, h) * 0.34f;
        paint.setShader(new RadialGradient(sx, sy, glowR, alpha(day ? p.glow : 0xFFB8C4FF, day ? 150 : 60),
            alpha(day ? p.glow : 0xFFB8C4FF, 0), Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, glowR, paint);
        paint.setShader(null);

        // clouds
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(day ? 46 : 20);
        for (int k = 0; k < 3; k++) {
            float x = (((cloud + k * 0.37f) % 1f) * (w + 160 * density)) - 80 * density;
            float y = h * (0.20f + 0.13f * k);
            oval.set(x - 46 * density, y - 9 * density, x + 46 * density, y + 9 * density);
            c.drawOval(oval, paint);
            oval.set(x - 24 * density, y - 17 * density, x + 22 * density, y + 4 * density);
            c.drawOval(oval, paint);
        }

        // sun or moon
        if (day) {
            float r = 20 * density;
            paint.setShader(new RadialGradient(sx - r * 0.25f, sy - r * 0.25f, r * 1.2f, 0xFFFFFFFF, p.sun, Shader.TileMode.CLAMP));
            c.drawCircle(sx, sy, r, paint);
            paint.setShader(null);
        } else {
            float r = 14 * density;
            paint.setColor(0xFFEAEFFA);
            paint.setAlpha(255);
            c.drawCircle(sx, sy, r, paint);
            paint.setColor(blend(nightTop, nightBottom, 0.5f));
            c.drawCircle(sx + r * 0.45f, sy - r * 0.2f, r * 0.9f, paint);
        }

        // dunes on the horizon (the sun sets behind them)
        hills.reset();
        hills.moveTo(0, h);
        hills.lineTo(0, horizon);
        hills.quadTo(w * 0.22f, horizon - h * 0.07f, w * 0.45f, horizon - h * 0.02f);
        hills.quadTo(w * 0.70f, horizon + h * 0.03f, w, horizon - h * 0.05f);
        hills.lineTo(w, h);
        hills.close();
        paint.setColor(blend(p.heroB, 0xFF000000, 0.45f));
        paint.setAlpha(200);
        c.drawPath(hills, paint);
        paint.setAlpha(255);
        c.restore();
    }
}
