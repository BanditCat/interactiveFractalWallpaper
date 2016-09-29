package science.jondubois.lnz;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import science.jondubois.lnzlib.LnzPrefs;
import science.jondubois.lnzlib.LnzRenderer;

public class LnzWallpaperService extends WallpaperService {
    private Context cntxt = null;

    @Override
    public Engine onCreateEngine(){
        cntxt = getApplicationContext();
        return new LnzWallpaperEngine();

    }



    private class LnzWallpaperEngine extends Engine {
        private LnzRenderer renderer;


        LnzWallpaperEngine(){
            setTouchEventsEnabled(true);

        }

        @Override
        public void onTouchEvent(MotionEvent e) { LnzActivity.touch( renderer, e ); }
        private float width, height;
        class LnzWallpaperView extends GLSurfaceView {

            LnzWallpaperView( Context context ){ super( context); width = getWidth(); height = getHeight();}

            @Override
            public SurfaceHolder getHolder(){ return getSurfaceHolder(); }

            @Override
            public void surfaceChanged( SurfaceHolder h, int f, int w, int he ){
                super.surfaceChanged( h, f, w, he );
                width = w;
                height = he;
                renderer.onSurfaceChanged( null, w, he );
            }
            public void onDestroy(){ super.onDetachedFromWindow(); }
        }

        private LnzWallpaperView lwv = null;


        @Override
        public void onCreate( SurfaceHolder surfaceHolder ){
            super.onCreate( surfaceHolder );


            lwv = new LnzWallpaperView( LnzWallpaperService.this );

            setEGLContextClientVersion(2);
            setPreserveEGLContextOnPause(true);

            renderer = new LnzRenderer( getApplicationContext(), width, height );
            setRenderer( renderer );

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( cntxt );
            LnzPrefs.setPreferences(cntxt, sharedPreferences, renderer);
        }

        @Override
        public void onVisibilityChanged( boolean visible ){
            super.onVisibilityChanged(visible);
            if( lwv != null ){
                if( visible ) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( cntxt );
                    LnzPrefs.setPreferences(cntxt, sharedPreferences, renderer);
                    lwv.onResume();
                } else {
                    lwv.onPause();
                }
            }

        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            lwv.onDestroy();
        }

        protected void setRenderer( GLSurfaceView.Renderer renderer ){
            lwv.setRenderer( renderer );
        }

        protected void setPreserveEGLContextOnPause( boolean preserve ){
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
                lwv.setPreserveEGLContextOnPause(preserve);
            }
        }

        protected void setEGLContextClientVersion(int version){
            lwv.setEGLContextClientVersion(version);
        }

    }


}
