package app.awqat.prayer;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/** Live Qibla compass. Android has no permission dialog for the compass sensors, so the screen asks consent itself. */
final class QiblaScreen implements Screen, SensorEventListener {
    private final MainActivity act;
    private final AppState st;
    private final Ui ui;
    private final Themes.Palette p;

    private CompassView cv;
    private TextView lDeg, lKm, lStatus;
    private SensorManager sm;
    private boolean running, got, aligned, noSensor;
    private float heading, declination, qibla;
    private float[] grav, geo;
    private int accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

    QiblaScreen(MainActivity act, AppState st, Ui ui) {
        this.act = act;
        this.st = st;
        this.ui = ui;
        this.p = ui.p;
    }

    @Override public View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(24));
        root.addView(ui.text("القبلة", 28, p.accent, true));
        root.addView(ui.text("أمسك الهاتف أفقيًا وأدره حتى تصل علامة الكعبة إلى المؤشر الأحمر", 13, p.muted, false),
            ui.m(Ui.MATCH, Ui.WRAP, 0, 0, 0, 14));
        LinearLayout card = ui.card();
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        cv = new CompassView(act, p);
        card.addView(cv, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
        lDeg = ui.text("", 32, p.accent, true);
        lDeg.setGravity(Gravity.CENTER);
        card.addView(lDeg, ui.m(Ui.MATCH, Ui.WRAP, 0, 6, 0, 0));
        lKm = ui.text("", 14, p.muted, false);
        lKm.setGravity(Gravity.CENTER);
        card.addView(lKm);
        lStatus = ui.text("", 14, p.muted, false);
        lStatus.setGravity(Gravity.CENTER);
        card.addView(lStatus, ui.m(Ui.MATCH, Ui.WRAP, 0, 10, 0, 12));
        card.addView(ui.button("تفعيل البوصلة", true, v -> consentGiven()), new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        root.addView(card);
        sv.addView(root);
        return sv;
    }

    private void updateQibla() {
        double[] ll = st.latLng();
        double[] q = PrayerCalc.qibla(ll[0], ll[1]);
        qibla = (float) q[0];
        cv.setQibla(qibla);
        lDeg.setText(String.format(Locale.ENGLISH, "%d° ", Math.round(q[0])) + PrayerCalc.DIRS[(int) Math.round(q[0] / 45.0) % 8]);
        lKm.setText("المسافة إلى مكة المكرمة نحو " + String.format(Locale.ENGLISH, "%,d", Math.round(q[1])) + " كم");
        try {
            declination = new GeomagneticField((float) ll[0], (float) ll[1], 0f, System.currentTimeMillis()).getDeclination();
        } catch (Exception e) {
            declination = 0f;
        }
    }

    @Override public void onShow() {
        got = false;
        aligned = false;
        updateQibla();
        cv.setHeading(0f);
        updateStatus();
        if (st.prefs.getBoolean("sensor_ok", false)) startCompass();
        else askConsent();
    }

    @Override public void onHide() { stopCompass(); }

    private void askConsent() {
        Ui.show(ui.dialog().setTitle("إذن مستشعر البوصلة")
            .setMessage("يحتاج تحديد القبلة إلى مستشعر البوصلة في هاتفك. هل تسمح للتطبيق باستخدامه؟")
            .setCancelable(false)
            .setPositiveButton("سماح", (d, w) -> consentGiven())
            .setNegativeButton("لاحقًا", (d, w) -> updateStatus()));
    }

    private void consentGiven() {
        st.putBool("sensor_ok", true);
        stopCompass();
        startCompass();
    }

    private void startCompass() {
        if (running) return;
        sm = (SensorManager) act.getSystemService(Context.SENSOR_SERVICE);
        boolean ok = false;
        Sensor rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation != null) ok = sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        if (!ok) {
            Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (acc != null && mag != null) {
                ok = sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_GAME)
                    & sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
            }
        }
        running = ok;
        noSensor = !ok;
        updateStatus();
    }

    private void stopCompass() {
        if (sm != null) sm.unregisterListener(this);
        running = false;
        grav = null;
        geo = null;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int acc) {
        accuracy = acc;
        updateStatus();
    }

    @Override public void onSensorChanged(SensorEvent e) {
        float[] r = new float[9], o = new float[3];
        boolean ok = false;
        int type = e.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(r, e.values);
            ok = true;
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            grav = lowpass(grav, e.values);
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            geo = lowpass(geo, e.values);
        }
        if (!ok && grav != null && geo != null) ok = SensorManager.getRotationMatrix(r, null, grav, geo);
        if (!ok) return;
        SensorManager.getOrientation(r, o);
        float deg = (float) Math.toDegrees(o[0]) + declination;     // magnetic north -> true north
        deg = (deg % 360f + 360f) % 360f;
        if (!got) {
            heading = deg;
            got = true;
        } else {
            float d = ((deg - heading + 540f) % 360f) - 180f;
            heading = (heading + d * 0.25f + 360f) % 360f;
        }
        cv.setHeading(heading);
        float diff = ((qibla - heading + 540f) % 360f) - 180f;
        boolean now = Math.abs(diff) <= 4f;
        if (now && !aligned) buzz();
        aligned = now;
        cv.setAligned(now);
        updateStatus();
    }

    private static float[] lowpass(float[] old, float[] v) {
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) out[i] = old == null ? v[i] : old[i] + 0.2f * (v[i] - old[i]);
        return out;
    }

    private void buzz() {
        if (!st.haptics) return;
        try {
            Vibrator vib = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) { /* no vibrator */ }
    }

    private void updateStatus() {
        String t;
        if (noSensor) t = "لا تتوفر بوصلة على هذا الجهاز. اتجاه القبلة هو الزاوية أعلاه ابتداءً من الشمال.";
        else if (got && aligned) t = "أنت متجه نحو القبلة";
        else if (got) {
            t = "أمسك الهاتف أفقيًا وأدره حتى تصل علامة الكعبة إلى المؤشر الأحمر في الأعلى.";
            if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) t += " حرّك الهاتف على شكل رقم 8 لمعايرة البوصلة.";
        } else if (running) t = "جارٍ قراءة البوصلة…";
        else t = "اضغط تفعيل البوصلة ثم أمسك الهاتف أفقيًا.";
        lStatus.setText(t);
        lStatus.setTextColor(aligned ? p.ok : p.muted);
    }
}
