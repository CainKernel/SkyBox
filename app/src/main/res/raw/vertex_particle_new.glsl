uniform mat4 uMVPMatrix;
uniform float uPointSize;

attribute vec3 a_Position;
attribute vec3 a_Color;

varying vec3 v_Color;

void main() {
    v_Color = a_Color;
    gl_Position = uMVPMatrix * vec4(a_Position, 1.0);
    gl_PointSize = uPointSize;
}
