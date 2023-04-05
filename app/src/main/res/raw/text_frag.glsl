precision mediump float;
uniform sampler2D u_TextureUnit;
uniform vec4 u_TextColor;

varying vec2 v_TexCoord;

void main() {
    gl_FragColor = u_TextColor * texture2D(u_TextureUnit, v_TexCoord);
}