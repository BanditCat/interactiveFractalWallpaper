package science.jondubois.lnzlib;


import android.content.Context;
import android.opengl.GLSurfaceView;

public class LnzView extends GLSurfaceView{
    public LnzRenderer renderer;

    public LnzView( Context context ){
        super( context );

        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        renderer = new LnzRenderer( context, getWidth(), getHeight() );
        setRenderer( renderer );
    }

}
