package science.jondubois.lnzlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class LnzPrefs extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context context;
    private LnzRenderer lnzRenderer;
    private static Resources res;

    public LnzPrefs(){}
    @SuppressLint("ValidFragment")
    public LnzPrefs( Context c, LnzRenderer r, Resources rs ){
        context = c; lnzRenderer = r; res = rs;
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        SharedPreferences sharedPreferences;
        if( context != null ) {
            PreferenceManager.setDefaultValues(context, R.xml.lnz_prefs, false);
            addPreferencesFromResource(R.xml.lnz_prefs);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            setPreferences(context, sharedPreferences, lnzRenderer);
        }
    }
    static public void setPreferences(Context c, SharedPreferences sp, LnzRenderer lr ){
        if( res == null )
            res = c.getResources();
        if( res != null && lr != null ) {
            int mn = res.getInteger(R.integer.speedMin);
            int spd = sp.getInt("speed", res.getInteger(R.integer.defaultSpeed) - mn);
            lr.setSpeed(mn + spd);
            int dmin = res.getInteger(R.integer.dimmerMin);
            int dval= sp.getInt("dimmer", res.getInteger(R.integer.defaultDimmer) - dmin);
            lr.setDimmer(dval + dmin);
            lr.setRtt(sp.getBoolean("rtt", res.getBoolean(R.bool.defaultRtt)));
            lr.setInteraction(sp.getBoolean("interaction", res.getBoolean(R.bool.defaultInteraction)));
            int psmn = res.getInteger(R.integer.pixelSizeMin);
            int psval= sp.getInt("pixelSize", res.getInteger(R.integer.defaultPixelSize) - psmn);
            lr.setPixelSize(psmn + psval);
            lr.setSkipping(sp.getBoolean("skipping", res.getBoolean(R.bool.defaultSkipping)));
            int imin = res.getInteger(R.integer.iterationsMin);
            int ival= sp.getInt("iterations", res.getInteger(R.integer.defaultIterations) - psmn);
            lr.setIterations(ival + imin);
            lr.setPalette(Integer.parseInt(sp.getString("palette", res.getString(R.string.defaultPalette))));
            lr.setHighp(sp.getBoolean("highp", res.getBoolean(R.bool.defaultHighp)));
            lr.setMsaa(sp.getBoolean("msaa", res.getBoolean(R.bool.defaultMsaa)));
            lr.init();

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "speed":
                int smin = res.getInteger(R.integer.speedMin);
                lnzRenderer.setSpeed(sharedPreferences.getInt("speed", res.getInteger(R.integer.defaultSpeed) - smin)
                        + smin);
                break;
            case "dimmer":
                int dmin = res.getInteger(R.integer.dimmerMin);
                lnzRenderer.setDimmer(sharedPreferences.getInt("dimmer", res.getInteger(R.integer.defaultDimmer) - dmin) + dmin);
                break;
            case "rtt":
                lnzRenderer.setRtt(sharedPreferences.getBoolean("rtt", res.getBoolean(R.bool.defaultRtt)));
                lnzRenderer.init();
                break;
            case "interaction":
                lnzRenderer.setInteraction(sharedPreferences.getBoolean("interaction", res.getBoolean(R.bool.defaultInteraction)));
                lnzRenderer.init();
                break;
            case "msaa":
                boolean fx = sharedPreferences.getBoolean("msaa", res.getBoolean(R.bool.defaultMsaa));
                lnzRenderer.setMsaa(fx);
                lnzRenderer.init();
                break;
            case "skipping":
                lnzRenderer.setSkipping(sharedPreferences.getBoolean("skipping", res.getBoolean(R.bool.defaultSkipping)));
                lnzRenderer.init();
                break;
            case "pixelSize":
                int psmin = res.getInteger( R.integer.pixelSizeMin );
                lnzRenderer.setPixelSize(sharedPreferences.getInt("pixelSize", res.getInteger(R.integer.defaultPixelSize) - psmin
                ) + psmin);
                lnzRenderer.init();
                break;
            case "iterations":
                int imin = res.getInteger( R.integer.iterationsMin );
                lnzRenderer.setIterations(sharedPreferences.getInt("iterations", res.getInteger(R.integer.defaultIterations) - imin
                ) + imin);
                lnzRenderer.init();
                break;
            case "palette":
                lnzRenderer.setPalette(Integer.parseInt( sharedPreferences.getString("palette", res.getString( R.string.defaultPalette))));
                lnzRenderer.init();
                break;

        }
    }
}
