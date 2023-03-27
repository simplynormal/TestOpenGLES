uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;

attribute vec4 a_Position;
attribute vec4 a_Color;

varying vec4 v_Color;

void main() {
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * a_Position;
    gl_PointSize = 10.0;
    v_Color = a_Color;
}