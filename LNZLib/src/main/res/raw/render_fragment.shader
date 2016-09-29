precision lowp float;

uniform sampler2D renderBuffer;

varying vec2 v_Pos;

void main(){
  gl_FragColor = texture2D(renderBuffer, v_Pos);
}


