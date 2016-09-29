uniform mat4 mat;
attribute vec4 a_Position;

varying vec2 v_Pos;

void main(){
  vec4 ans = a_Position;
  vec4 vp = ans * mat;

  v_Pos = vp.xy;
  v_Pos.x += mat[ 3 ][ 0 ];
  v_Pos.y += mat[ 3 ][ 1 ];
  gl_Position = ans;
}