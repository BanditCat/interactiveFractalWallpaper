precision $ float;

uniform sampler2D palette;
uniform vec2 perturbation;
uniform vec3 lineParameters;
uniform vec2 linePoint;
uniform float dimmer;
uniform vec2 pixelOffset;

varying vec2 v_Pos;


vec4 getColor( vec2 mcx, vec2 p ){
  float min = 100.0;
  vec4 ans;
  float tl = 0.0;
  vec3 cx = vec3( mcx + p, 1.0 );
  vec2 cy;

  for( int i = 0; i < %; ++i ){
    float t = cx.x;
    cx.x = cx.x * cx.x - cx.y * cx.y;
    cx.y = 2.0 * t * cx.y;

    cx.xy += mcx.xy;

    float dist = abs( dot( cx, lineParameters ) ) / length( lineParameters.xy );
    if( dist < min ){
      min = dist;
      cy = cx.xy;
    }
  }

  float dst = distance( linePoint, cy );
  min = (1.0 - min) / max( dst, 1.0 );
  vec2 uv = vec2( 1.0 - log( dst ) / 5.0, 1.0 );
  ans = texture2D(palette, uv) * min * dimmer;
  ans.a = 1.0;
  ans = clamp( ans, 0.0, 1.0 );
  return ans;
}
void main(){
  vec2 center = v_Pos;
  vec2 po2 = pixelOffset.yx;
  po2.x = -po2.x;

  vec4 fc = getColor( center + pixelOffset, perturbation ) / vec4( 4.0, 4.0, 4.0, 1.0 );
  fc += getColor( center - pixelOffset, perturbation ) / vec4( 4.0, 4.0, 4.0, 1.0 );
  fc += getColor( center + po2, perturbation ) / vec4( 4.0, 4.0, 4.0, 1.0 );
  fc += getColor( center - po2, perturbation ) / vec4( 4.0, 4.0, 4.0, 1.0 );
  gl_FragColor = fc;
}
