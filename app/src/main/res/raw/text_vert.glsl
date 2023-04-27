uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;
uniform mat4 u_TransformMatrix;

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

void main() {
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * u_TransformMatrix * a_Position;
    v_TexCoord = a_TexCoord;
    gl_PointSize = 10.0;
}