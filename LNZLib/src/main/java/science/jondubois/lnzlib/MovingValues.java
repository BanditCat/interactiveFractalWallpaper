package science.jondubois.lnzlib;


import java.util.Random;

public class MovingValues {

    public float[] values;
    public float[] velocity;
    public float[] newValues;
    private Random r = new Random();

    private float[] maxBounds;
    private float[] minBounds;




    public MovingValues( float[] minBounds, float[] maxBounds ){
        values = new float[ minBounds.length ];
        newValues = new float[ minBounds.length ];
        velocity = new float[ minBounds.length ];
        this.maxBounds = maxBounds;
        this.minBounds = minBounds;
        for (int i = 0; i < minBounds.length; i++) {
            velocity[ i ] = 0.0f;
            values[ i ] = r.nextFloat() * ( maxBounds[ i ] - minBounds[ i ] ) + minBounds[ i ];
            newValues[ i ] = r.nextFloat() * ( maxBounds[ i ] - minBounds[ i ] ) + minBounds[ i ];
        }
    }
    public float distance( ){
        float dist = 0;
        for (int i = 0; i < values.length; i++) {
            float ldist = values[ i ] - newValues[ i ];
            dist += ldist * ldist;
        }
        return (float) Math.sqrt(dist);
    }


    public void tick( float delta, float restitution, float slowRadius ){
        float dlt;
        float dist = distance();
        if( dist < 0.0001f )
            dist = 0.0001f;
        if( dist < slowRadius )
            dlt = dist / slowRadius;
        else
            dlt = 1.0f;
        for (int i = 0; i < values.length; i++) {
            if( values[ i ] > maxBounds[ i ] )
                values[ i ] = maxBounds[ i ];
            if( values[ i ] < minBounds[ i ] )
                values[ i ] = minBounds[ i ];
            float accel = ( (newValues[ i ] - values[ i ])  ) / dist;
            velocity[ i ] += accel;
            velocity[ i ] *= restitution;
            values[ i ] += velocity[ i ] * delta * dlt;

        }
    }

    public boolean moveNewValuesIfCloserThan( float distance ){
        if( distance() < distance ) {
            for (int i = 0; i < minBounds.length; i++) {
                newValues[i] = r.nextFloat() * (maxBounds[i] - minBounds[i]) + minBounds[i];
            }
            return true;
        }
        return false;
    }

    public void warpToValues(){
        for (int i = 0; i < values.length; i++) {
            newValues[ i ] = values[ i ];
        }
    }

    public void clip(){
        for (int i = 0; i < newValues.length; i++) {
            if( newValues[ i ] > maxBounds[ i ] )
                newValues[ i ] = maxBounds[ i ];
            if( newValues[ i ] < minBounds[ i ] )
                newValues[ i ] = minBounds[ i ];
        }
    }
}
