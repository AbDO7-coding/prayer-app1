package app.awqat.prayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** Last known location first; otherwise one fresh fix with a 20 second timeout. */
final class LocationHelper {
    interface Callback {
        void onFix(double lat, double lng);
        void onFail(String message);
    }

    private LocationManager lm;
    private LocationListener listener;
    private Runnable timeout;
    private final Handler handler = new Handler(Looper.getMainLooper());

    static boolean hasPermission(Activity a) {
        return a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void request(Activity a, final Callback cb) {
        cancel();
        lm = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String prov : providers) {
            try {
                Location l = lm.getLastKnownLocation(prov);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException | IllegalArgumentException ignored) { /* provider unavailable */ }
        }
        if (best != null) {
            cb.onFix(best.getLatitude(), best.getLongitude());
            return;
        }
        String provider = null;
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) provider = LocationManager.NETWORK_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) provider = LocationManager.GPS_PROVIDER;
        } catch (Exception ignored) { /* treated as no provider */ }
        if (provider == null) {
            cb.onFail("فعّل خدمة الموقع في الهاتف أو اختر مدينة.");
            return;
        }
        listener = new LocationListener() {
            @Override public void onLocationChanged(Location l) {
                cancel();
                cb.onFix(l.getLatitude(), l.getLongitude());
            }
            @Override public void onStatusChanged(String s, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String s) {}
            @Override public void onProviderDisabled(String s) {}
        };
        try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper());
        } catch (SecurityException e) {
            listener = null;
            cb.onFail("لم يتم منح إذن الموقع.");
            return;
        }
        timeout = new Runnable() {
            @Override public void run() {
                cancel();
                cb.onFail("تعذر تحديد موقعك. اختر مدينة.");
            }
        };
        handler.postDelayed(timeout, 20000);
    }

    void cancel() {
        if (listener != null && lm != null) {
            try { lm.removeUpdates(listener); } catch (Exception ignored) { /* already removed */ }
        }
        listener = null;
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
    }
}
