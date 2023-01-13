precision mediump float;

uniform vec4 u_Color;

void main() {
    if (gl_PointCoord.x < 0.25) {
        gl_FragColor = u_Color;
    } else {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    }
}