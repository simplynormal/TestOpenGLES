precision mediump float;

uniform vec4 u_Color;

void main() {
    if (gl_FragCoord.x < 540.0) {
        gl_FragColor = u_Color;
    } else {
        gl_FragColor = u_Color * 0.5;
    }
}