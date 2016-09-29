package science.jondubois.lnz;


import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import science.jondubois.lnzlib.LnzPrefs;
import science.jondubois.lnzlib.LnzRenderer;
import science.jondubois.lnzlib.LnzView;

public class LnzActivity extends Activity{
    public LnzRenderer renderer;
    private LnzPrefs mPrefsFragment;

    @Override
    public void onCreate( Bundle savedInstanceState ){
        super.onCreate(savedInstanceState);
        setContentView( R.layout.preview );
        LnzView lnzView = new LnzView( this );
        renderer = lnzView.renderer;
        FrameLayout frame = (FrameLayout)findViewById( R.id.lnzPreview );
        frame.addView( lnzView );

        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        mPrefsFragment = new LnzPrefs( getApplicationContext(), renderer, getResources());
        mFragmentTransaction.replace(R.id.lnzSettings, mPrefsFragment);
        mFragmentTransaction.commit();
        FrameLayout layout = (FrameLayout)findViewById(R.id.lnzSettings);
        layout.setVisibility(View.INVISIBLE);
    }
    
    @Override
    protected void onResume(){ super.onResume(); }

    @Override
    protected void onPause(){ super.onPause(); }
    
    public void onWallpaperSettings( View view ){
        Intent wallpaperSettings = new Intent( Intent.ACTION_SET_WALLPAPER );
        startActivity(wallpaperSettings);
    }
    public void onSettings( View view ){
        FrameLayout layout = (FrameLayout)findViewById(R.id.lnzSettings);
        if(layout.getVisibility() == View.INVISIBLE ) {
            layout.setVisibility(View.VISIBLE);
            Button mButton = (Button) view.findViewById(R.id.settingsButton);
            mButton.setText(R.string.close);
        }else{
            layout.setVisibility(View.INVISIBLE);
            Button mButton=(Button)view.findViewById(R.id.settingsButton);
            mButton.setText(R.string.settings);
        }
    }

    private static float previousX, previousY;
    static void touch( LnzRenderer r, MotionEvent e ) {
        if( r.interaction ) {
            float x = e.getX();
            float y = e.getY();
            float x2 = x;
            float y2 = y;
            if (e.getPointerCount() == 2) {
                x2 = e.getX(1);
                y2 = e.getY(1);
            }
            if (x != x2 || y != y2)
                r.camera(x2 - x, y2 - y, (x2 + x) / 2.0f, (y2 + y) / 2.0f);
            else {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - previousX;
                        float dy = y - previousY;
                        r.move(dx, dy);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        r.press();
                        break;
                    case MotionEvent.ACTION_UP:
                        r.release();
                        break;
                }

            }

            previousX = x;
            previousY = y;
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        touch( renderer, e );
        return true;
    }


}