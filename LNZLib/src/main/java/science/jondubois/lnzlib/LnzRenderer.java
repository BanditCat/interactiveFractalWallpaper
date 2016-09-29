// palette rotation, simple shader.

package science.jondubois.lnzlib;


import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LnzRenderer implements GLSurfaceView.Renderer{
    private Context context;
    private Random r = new Random();

    private MovingValues lineMovingValues = new MovingValues(
            new float[]{ -0.3f, -0.3f, -3.1f },
            new float[]{ 0.3f, 0.3f, 3.1f });
    // X, Y, zoom, rot
    private MovingValues zoomMovingValues = new MovingValues(
            new float[]{ -1.3f, -0.4f, -0.3f, -2.1f },
            new float[]{ 0.3f, 0.4f, 0.7f, 2.1f});
    private MovingValues perturbationMovingValues = new MovingValues(
            new float[]{ -0.5f, -0.5f },
            new float[]{ 0.5f, 0.5f });

    private final FloatBuffer screenQuad;
    private final ByteBuffer[] paletteBytes = new ByteBuffer[4];
    private int[] paletteSize = new int[ 4 ];
    float maxSpeed = 0.00021f;
    public void setSpeed( int percent ){
        maxSpeed = ( (float)percent / 100.0f ) * 0.00021f;
    }
    public void setDimmer( int percent ){
        dimmer = (float) percent / 100.0f;
    }

    private int positionAttributeLocation;
    private int renderPositionAttributeLocation;

    private int lineParametersUniformLocation;
    private int linePointUniformLocation;
    private int matrixUniformLocation;
    private int perturbationUniformLocation;
    private int renderBufferUniformLocation;
    private int renderBufferTextureHandle;
    private int paletteUniformLocation;
    private int dimmerUniformLocation;
    private int pixelOffsetUniformLocation;
    private int paletteHandle;
    private float dimmer = 1.0f;


    private int programHandle;
    private int renderProgramHandle;

    private int fboHandle;
    private int renderBufferHandle;

    private int mode;
    private float modeTime;

    private boolean buttonPressed;
    private boolean buttonJustPressed;

    private float width;
    private float height;

    private boolean skipping = true;
    public void setSkipping( boolean s ){
        skipping = s;
    }
    private boolean msaa = true;
    public void setMsaa(boolean f){
        msaa = f;
    }

    private float screenDiv = 2.0f;
    public void setPixelSize( int size ){
        screenDiv = size;
    }
    private int iterations = 12;
    public void setIterations( int iter ){
        iterations = iter;
    }
    private int palette = 0;
    public void setPalette( int p ){
        palette = p;
    }
    private boolean highp = true;
    public void setHighp( boolean h ){
        highp = h;
    }
    private boolean rtt;
    public void setRtt( boolean val ){
        rtt = val;
    }
    public boolean interaction = true;
    public void setInteraction( boolean val ){
        interaction = val;
    }

    public LnzRenderer( Context c, float x, float y ) {
        context = c;
        width = x;
        height = y;

        final float[] screenQuadData = {
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };


        screenQuad = ByteBuffer.allocateDirect(screenQuadData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        screenQuad.put(screenQuadData).position(0);

        final byte[] paletteData = {
                (byte)0x6E, (byte)0x5B, (byte)0x47,
                (byte)0xAB, (byte)0x70, (byte)0x59,
                (byte)0x4E, (byte)0x6A, (byte)0x7B,
                (byte)0x0C, (byte)0x08, (byte)0x05,
                (byte)0xAB, (byte)0xAE, (byte)0xB0};
        paletteBytes[ 0 ] = ByteBuffer.allocateDirect(paletteData.length);
        paletteBytes[ 0 ].put(paletteData).position(0);
        paletteSize[ 0 ] = paletteData.length / 3;

        final byte[] paletteData1 = {
                (byte)255, 0, 0,
                0, (byte)255, 0,
                0, 0, (byte)255,
                0, (byte)255, (byte)255,
                (byte)255, (byte)255, 0,
                (byte)255, 0, (byte)255};
        paletteBytes[ 1 ] = ByteBuffer.allocateDirect(paletteData1.length);
        paletteBytes[ 1 ].put(paletteData1).position(0);
        paletteSize[ 1 ] = paletteData1.length / 3;

        final byte[] paletteData2 = {
                (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0x7F, (byte)0x7F, (byte)0x7F,
                (byte)0xFF, (byte)0xFF, (byte)0xFF};
        paletteBytes[ 2 ] = ByteBuffer.allocateDirect(paletteData2.length);
        paletteBytes[ 2 ].put(paletteData2).position(0);
        paletteSize[ 2 ] = paletteData2.length / 3;

        final byte[] paletteData3 = {
                (byte)0x10, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0x01, (byte)0xFF, (byte)0x7B};
        paletteBytes[ 3 ] = ByteBuffer.allocateDirect(paletteData3.length);
        paletteBytes[ 3 ].put(paletteData3).position(0);
        paletteSize[ 3 ] = paletteData3.length / 3;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        myInit();

    }
    private boolean needInit = false;
    public void init(){
        needInit = true;
    }
    private void myInit(){
        destroy();
        // Load in the vertex shader.
        String rfString;
        if(msaa)
            rfString = readTextFileFromRawResource(context, R.raw.msaa_fragment);
        else
            rfString = readTextFileFromRawResource(context, R.raw.pickover_fragment);
        if( highp )
            rfString = rfString.replace( "$", "highp");
        else
            rfString = rfString.replace( "$", "mediump");
        rfString = rfString.replace( "%", Integer.toString(iterations));

        int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, readTextFileFromRawResource(context, R.raw.pickover_vertex));
        int fragmentShaderHandle = loadShader( GLES20.GL_FRAGMENT_SHADER, rfString);
        programHandle = loadProgram( vertexShaderHandle, fragmentShaderHandle );
        int renderVertexShaderHandle = loadShader( GLES20.GL_VERTEX_SHADER, readTextFileFromRawResource(context, R.raw.render_vertex));
        int renderFragmentShaderHandle = loadShader( GLES20.GL_FRAGMENT_SHADER, readTextFileFromRawResource(context, R.raw.render_fragment) );
        renderProgramHandle = loadProgram( renderVertexShaderHandle, renderFragmentShaderHandle );


        GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
        positionAttributeLocation = GLES20.glGetAttribLocation(programHandle, "a_Position");
        GLES20.glBindAttribLocation(renderProgramHandle, 0, "a_Position");
        renderPositionAttributeLocation = GLES20.glGetAttribLocation(renderProgramHandle, "a_Position");
        lineParametersUniformLocation = GLES20.glGetUniformLocation(programHandle, "lineParameters");
        linePointUniformLocation = GLES20.glGetUniformLocation(programHandle, "linePoint");
        matrixUniformLocation = GLES20.glGetUniformLocation(programHandle, "mat");
        perturbationUniformLocation = GLES20.glGetUniformLocation(programHandle, "perturbation");
        paletteUniformLocation = GLES20.glGetUniformLocation(programHandle, "palette");
        renderBufferUniformLocation = GLES20.glGetUniformLocation(renderProgramHandle, "renderBuffer");
        if(msaa)
            pixelOffsetUniformLocation = GLES20.glGetUniformLocation(programHandle, "pixelOffset");
        dimmerUniformLocation = GLES20.glGetUniformLocation(programHandle, "dimmer");

        final int[] temp = new int[1];
        GLES20.glGenTextures(1, temp, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, temp[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, paletteSize[ palette ], 1, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, paletteBytes[ palette ]);
        paletteHandle = temp[ 0 ];


        GLES20.glGenFramebuffers(1, temp, 0);
        fboHandle = temp[ 0 ];
        GLES20.glGenTextures(1, temp, 0);
        renderBufferTextureHandle = temp[0];
        GLES20.glGenRenderbuffers(1, temp, 0);
        renderBufferHandle = temp[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHandle);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderBufferTextureHandle);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, (int)( width / screenDiv), (int)( height / screenDiv ), 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferHandle);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, (int)(width / screenDiv), (int)( height / screenDiv ));
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderBufferTextureHandle,0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderBufferHandle);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    public void destroy(){
        // Load in the vertex shader.
        GLES20.glUseProgram(0);
        if( programHandle != 0 ){ GLES20.glDeleteProgram(programHandle); }
        if( renderProgramHandle != 0 ){ GLES20.glDeleteProgram(renderProgramHandle); }

        int[] temp = new int[ 1];
        temp[ 0 ]=fboHandle;
        if( temp[ 0 ] != 0 ){ GLES20.glDeleteFramebuffers(1, temp, 0); }
        temp[ 0 ] = renderBufferTextureHandle;
        if( temp[ 0 ] != 0 ){ GLES20.glDeleteTextures(1, temp, 0); }
        temp[ 0 ] = paletteHandle;
        if( temp[ 0 ] != 0 ){ GLES20.glDeleteTextures(1, temp, 0); }
        temp[ 0 ] = renderBufferHandle;
        if( temp[ 0 ] != 0 ){ GLES20.glDeleteRenderbuffers(1, temp, 0); }

    }

    private float cameraTime;
    private float odx, ody, ocx, ocy, oz;
    private double orot;
    public void camera( float dx, float dy, float cx, float cy ) {
        float z = (float) Math.sqrt(dx * dx + dy * dy);
        double rot = Math.atan2(dx, dy);
        if (SystemClock.uptimeMillis() - cameraTime > 100.0f) {
            odx = dx;
            ody = dy;
            ocx = cx;
            ocy = cy;
            oz = z;
            orot = rot;
            zoomMovingValues.newValues = zoomMovingValues.values.clone();
        }

        cameraTime = SystemClock.uptimeMillis();
        mode = 1;
        float ddx = odx - dx;
        float ddy = ody - dy;
        float dcx = cx - ocx;
        float dcy = ocy - cy;
        float dz = oz - z;
        double drot = orot - rot;
        if (Math.abs(drot) > 0.1f)
            drot = 0.0f;

        float oldx = dcx;
        float torad = -(float) Math.toRadians(100.0f * zoomMovingValues.newValues[3]);
        dcx = (float) (dcx * Math.cos(torad) - dcy * Math.sin(torad));
        dcy = (float) (oldx * Math.sin(torad) + dcy * Math.cos(torad));


        zoomMovingValues.newValues[0] -= dcx / 250.0f;
        zoomMovingValues.newValues[1] -= dcy / 250.0f;
        zoomMovingValues.newValues[2] += dz / 500.0f;
        zoomMovingValues.newValues[3] -= drot;
        if (zoomMovingValues.newValues[3] > 1.8f){
            zoomMovingValues.newValues[3] -= 3.6f;
            zoomMovingValues.values[3] -= 3.6f;
        }

        if( zoomMovingValues.newValues[ 3 ] < -1.8f ) {
            zoomMovingValues.newValues[3] += 3.6f;
            zoomMovingValues.values[3] += 3.6f;
        }
        zoomMovingValues.clip();

        odx = dx; ody = dy; ocx = cx; ocy = cy; oz = z; orot = rot;
    }

    private float mx, my;
    private float movingTimer = 0.0f;
    private float pressedTimer = 0.0f;
    public void move( float dx, float dy ){
        mx = dx / 1500.0f;
        my = dy / 1500.0f;
        movingTimer = SystemClock.uptimeMillis();
        pressedTimer = movingTimer;
    }
    public void press(){
        pressedTimer = SystemClock.uptimeMillis();
        buttonJustPressed = true;
        buttonPressed = true;
    }
    public void release(){ buttonPressed = false;}


    private boolean skip = false;
    private static float lastFrame = 0.0f;
    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        float frameDelta = 0.1f;
        if( ( SystemClock.uptimeMillis() - lastFrame ) < 100.0f )
            frameDelta = ( SystemClock.uptimeMillis() - lastFrame ) / 1000.0f;
        frameDelta *= 60.0f;
        lastFrame = SystemClock.uptimeMillis();
        if( needInit ) {
            needInit = false;
            myInit();
        }
        if(buttonPressed){
            movingTimer = SystemClock.uptimeMillis();
        }
        if( SystemClock.uptimeMillis() - pressedTimer > 50.0 ){
            mx = 0.0f;
            my = 0.0f;
        }

        float[] matrix = new float[ 16 ];


        float speed = ( SystemClock.uptimeMillis() - modeTime ) * maxSpeed / 1000.0f;
        if( speed > maxSpeed)
            speed = maxSpeed;
        speed *= frameDelta;



        final float movingTimeout = 750.0f;
        // Switch out of moving mode after a second.

        // Do moving values.
        if( mode == 0 ){
            if(buttonJustPressed){
                lineMovingValues.warpToValues();
                buttonJustPressed = false;
            }
            if( SystemClock.uptimeMillis() - movingTimer < movingTimeout ) {
                lineMovingValues.newValues[ 0 ] += mx;
                lineMovingValues.newValues[ 2 ] += my * 3.0f;
                lineMovingValues.clip();
                lineMovingValues.tick( 0.02f * frameDelta, 0.9f, 0.4f );
            }else
                lineMovingValues.tick( speed, 0.97f, 0.7f );
            if ( SystemClock.uptimeMillis() - movingTimer > movingTimeout && lineMovingValues.moveNewValuesIfCloserThan(0.2f)) {
                modeTime = SystemClock.uptimeMillis();
                mode = r.nextInt(3);
            }


        }else if( mode == 1 ) {
            if( SystemClock.uptimeMillis() - cameraTime < movingTimeout ){
                zoomMovingValues.tick( 0.3f * frameDelta, 0.5f, 1.7f);
            }else {
                if (SystemClock.uptimeMillis() - movingTimer < movingTimeout)
                    mode = r.nextInt(2) * 2;
                else {
                    zoomMovingValues.tick(speed, 0.95f, 0.7f);
                    if (SystemClock.uptimeMillis() - movingTimer > movingTimeout && zoomMovingValues.moveNewValuesIfCloserThan(0.2f)) {
                        mode = r.nextInt(2) * 2;
                        modeTime = SystemClock.uptimeMillis();
                    }
                }
            }
        }else if( mode == 2 ){
            if(buttonJustPressed){
                perturbationMovingValues.warpToValues();
                buttonJustPressed = false;
            }
            if( SystemClock.uptimeMillis() - movingTimer < movingTimeout ) {
                perturbationMovingValues.newValues[ 0 ] += mx;
                perturbationMovingValues.newValues[ 1 ] += my;
                perturbationMovingValues.clip();
                perturbationMovingValues.tick( 0.01f * frameDelta, 0.9f, 0.2f );
            }else
                perturbationMovingValues.tick( speed / 3.0f, 0.97f, 0.15f );
            if( SystemClock.uptimeMillis() - movingTimer > movingTimeout && perturbationMovingValues.moveNewValuesIfCloserThan(0.03f)){
                mode = r.nextInt( 3 );
                modeTime = SystemClock.uptimeMillis();
            }

        }
        float a, b, c;

        if( Math.abs( lineMovingValues.values[ 2 ] ) % Math.PI < ( Math.PI / 4 ) ||
                Math.abs( lineMovingValues.values[ 2 ] ) % Math.PI > ( 3 * Math.PI / 4 ) ) {
            b = -0.5f;
            a = (float) Math.tan(lineMovingValues.values[2]) * -b;
            c = ( lineMovingValues.values[1] + a / b * lineMovingValues.values[0] );
        }else{
            a = -0.5f;
            b = (float) ( 1.0f / Math.tan(lineMovingValues.values[2]) ) * -a;
            c = (lineMovingValues.values[0] + b / a * lineMovingValues.values[1]);
        }
        // fudge to prevent numeric instability.
        if( Math.abs(1.0f - a) < 0.02f )
            a = 1.02f;
        if( Math.abs(1.0f - b) < 0.02f )
            b = 1.02f;

        float r = zoomMovingValues.values[ 3 ] * 100.0f;
        float aspect = (float) Math.sqrt((float)height / width);
        float zoom = (float) Math.exp(zoomMovingValues.values[2]);
        float x = zoomMovingValues.values[ 0 ];
        float y = zoomMovingValues.values[ 1 ];
        float px = perturbationMovingValues.values[ 0 ];
        float py = perturbationMovingValues.values[ 1 ];


        // The Matrix
        Matrix.setIdentityM(matrix, 0);
        Matrix.scaleM(matrix, 0, matrix, 0, zoom / aspect, zoom * aspect, 1.0f);
        Matrix.translateM(matrix, 0, matrix, 0, x, y, 0.0f);
        Matrix.rotateM(matrix, 0, r, 0.0f, 0.0f, 1.0f);
        float[] inv = new float[ 16 ];
        Matrix.setIdentityM(inv, 0);
        Matrix.rotateM(inv, 0, -r, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(inv, 0, inv, 0, zoom, zoom, 1.0f);
        float[] vec = new float[ 4 ];
        vec[ 0 ] = ( rtt ? screenDiv : 1.0f ) / ( (float)Math.sqrt( width  * height ) * 1.5f );
        vec[ 1 ] = vec[ 0 ];
        vec[ 2 ] = 0.0f; vec[ 3 ] = 1.0f;
        Matrix.multiplyMV( vec, 0, inv, 0, vec, 0 );

        if( !skip || !rtt ) {
            if( !rtt )
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            else
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHandle);

            GLES20.glClear(0);
            if( rtt )
                GLES20.glViewport(0, 0, (int) (width / screenDiv), (int) (height / screenDiv));
            else
                GLES20.glViewport(0, 0, (int)width, (int)height);


            GLES20.glUseProgram(programHandle);
            // Texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, paletteHandle);
            GLES20.glUniform1i(paletteUniformLocation, 0);

            GLES20.glUniform2f(linePointUniformLocation, 0.0f, 0.0f);
            GLES20.glUniform3f(lineParametersUniformLocation, a, b, c);
            GLES20.glUniform2f(perturbationUniformLocation, px, py);
            GLES20.glUniform1f(dimmerUniformLocation, dimmer );
            GLES20.glUniformMatrix4fv(matrixUniformLocation, 1, false, matrix, 0);
            if(msaa)
                GLES20.glUniform2f(pixelOffsetUniformLocation, vec[ 0 ], vec[ 1 ]);
            GLES20.glVertexAttribPointer(positionAttributeLocation, 2, GLES20.GL_FLOAT, false,
                    2 * 4, screenQuad);
            GLES20.glEnableVertexAttribArray(positionAttributeLocation);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
            skip = skipping;
        }else
          skip = false;

        if( rtt ) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, (int) width, (int) height);


            GLES20.glUseProgram(renderProgramHandle);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glUniform1i(renderBufferUniformLocation, 1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderBufferTextureHandle);
            screenQuad.position(0);
            GLES20.glVertexAttribPointer(renderPositionAttributeLocation, 2, GLES20.GL_FLOAT, false,
                    2 * 4, screenQuad);
            GLES20.glEnableVertexAttribArray(positionAttributeLocation);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        }


    }



    @Override
    public void onSurfaceChanged(GL10 glUnused, int w, int h)
    {
        width = w;
        height = h;
        GLES20.glViewport(0, 0, (int)width, (int)height);
        init();
    }


    private int loadShader( int type, String source ) {
        int shaderHandle = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shaderHandle, source);
        GLES20.glCompileShader(shaderHandle);
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            Log.d("OpenGL log", GLES20.glGetShaderInfoLog(shaderHandle));
            GLES20.glDeleteShader(shaderHandle);
            throw new RuntimeException();
        }

        return shaderHandle;
    }

    private int loadProgram( int fragmentShader, int vertexShader ){
        int programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glLinkProgram(programHandle);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0)
        {
            Log.d("OpenGL log", GLES20.glGetProgramInfoLog(programHandle));
            GLES20.glDeleteProgram(programHandle);
            throw new RuntimeException();
        }
        GLES20.glDeleteShader( fragmentShader );
        GLES20.glDeleteShader( vertexShader );
        return programHandle;
    }

    private static String readTextFileFromRawResource( final Context context, final int resourceId ){
        final InputStream inputStream = context.getResources().openRawResource( resourceId );
        final InputStreamReader inputStreamReader = new InputStreamReader( inputStream );
        final BufferedReader bufferedReader = new BufferedReader( inputStreamReader );

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try{
            while( ( nextLine = bufferedReader.readLine() ) != null ){
                body.append( nextLine );
                body.append( '\n' );
            }
        }
        catch( IOException e ){
            return null;
        }

        return body.toString();
    }
}