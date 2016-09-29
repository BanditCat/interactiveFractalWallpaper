attribute vec4 a_Position;

varying vec2 v_Pos;

void main(){
  vec4 ans = a_Position;
  v_Pos = ( a_Position.xy * vec2( 0.5, 0.5 ) + vec2( 0.5, 0.5 ) );
  gl_Position = ans;
}