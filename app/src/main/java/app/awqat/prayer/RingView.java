package app.awqat.prayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Circular progress ring (used by the Tasbeeh counter and the Adhkar counter). */
final class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final int barColor, doneColor;
    private final float stroke;
    private float progress;
    private boolean done;

    RingView(Context c, int trackColor, int barColor, int doneColor, float strokePx) {
        super(c);
        this.barColor = barColor;
        this.doneColor = doneColor;
        this.stroke = strokePx;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(strokePx);
        track.setColor(trackColor);
        bar.setStyle(Paint.Style.STROKE);
        bar.setStrokeWidth(strokePx);
        bar.setStrokeCap(Paint.Cap.ROUND);
    }

    void set(float progress, boolean done) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.done = done;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float half = stroke / 2f;
        oval.set(half, half, getWidth() - half, getHeight() - half);
        c.drawArc(oval, 0f, 360f, false, track);
        if (progress > 0f) {
            bar.setColor(done ? doneColor : barColor);
            c.drawArc(oval, -90f, 360f * progress, false, bar);
        }
    }
}
