package app.awqat.prayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Simple line icons drawn on a 24 x 24 grid with one stroke width. */
final class NavIconView extends View {
    static final int TIMES = 0, ADHKAR = 1, TASBEEH = 2, HADITH = 3, QIBLA = 4, SETTINGS = 5;
    private final int kind;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    NavIconView(Context c, int kind) {
        super(c);
        this.kind = kind;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
    }

    void setColor(int color) {
        stroke.setColor(color);
        fill.setColor(color);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        c.save();
        c.scale(s, s);
        switch (kind) {
            case TIMES:
                c.drawCircle(12, 12, 9, stroke);
                c.drawLine(12, 12, 12, 7, stroke);
                c.drawLine(12, 12, 16, 14, stroke);
                break;
            case ADHKAR:
                path.reset();
                path.moveTo(3, 6); path.lineTo(11, 8); path.lineTo(11, 20); path.lineTo(3, 18); path.close();
                path.moveTo(21, 6); path.lineTo(13, 8); path.lineTo(13, 20); path.lineTo(21, 18); path.close();
                c.drawPath(path, stroke);
                break;
            case TASBEEH:
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45.0);
                    c.drawCircle(12f + (float) (8 * Math.cos(a)), 12f + (float) (8 * Math.sin(a)), 1.9f, fill);
                }
                c.drawCircle(12, 12, 2.2f, stroke);
                break;
            case HADITH:
                rect.set(5, 3, 19, 21);
                c.drawRoundRect(rect, 2.5f, 2.5f, stroke);
                c.drawLine(8, 9, 16, 9, stroke);
                c.drawLine(8, 13, 16, 13, stroke);
                c.drawLine(8, 17, 13, 17, stroke);
                break;
            case QIBLA:
                c.drawCircle(12, 12, 9, stroke);
                path.reset();
                path.moveTo(12, 5); path.lineTo(15, 12); path.lineTo(9, 12); path.close();
                c.drawPath(path, fill);
                path.reset();
                path.moveTo(12, 19); path.lineTo(15, 12); path.lineTo(9, 12); path.close();
                c.drawPath(path, stroke);
                break;
            default:
                c.drawLine(4, 7, 20, 7, stroke);
                c.drawLine(4, 12, 20, 12, stroke);
                c.drawLine(4, 17, 20, 17, stroke);
                c.drawCircle(9, 7, 2.2f, fill);
                c.drawCircle(15, 12, 2.2f, fill);
                c.drawCircle(8, 17, 2.2f, fill);
        }
        c.restore();
    }
}
