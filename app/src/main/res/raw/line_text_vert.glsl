precision mediump float;

uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;

uniform float u_RotationAngle;
uniform float u_Scale;
uniform vec2 u_Translation;

uniform vec2 u_TextCenter;

attribute vec4 a_Position;
attribute float a_FontSize;
attribute float a_Angle;
attribute vec2 a_Offset;
attribute float a_AltAngle;
attribute vec2 a_AltOffset;
attribute float a_FirstAngle;
attribute vec4 a_Color;

varying vec4 v_Color;

mat4 genScaleMatrix(float x, float y, float z) {
    return mat4(
    vec4(x, 0, 0, 0),
    vec4(0, y, 0, 0),
    vec4(0, 0, z, 0),
    vec4(0, 0, 0, 1)
    );
}

mat4 genRotationMatrix(float angle) {
    float angleInRadians = radians(angle);
    float c = cos(angleInRadians);
    float s = sin(angleInRadians);
    return mat4(
    vec4(c, s, 0, 0),
    vec4(-s, c, 0, 0),
    vec4(0, 0, 1, 0),
    vec4(0, 0, 0, 1)
    );
}

mat4 genTranslationMatrix(float x, float y, float z) {
    return mat4(
    vec4(1, 0, 0, 0),
    vec4(0, 1, 0, 0),
    vec4(0, 0, 1, 0),
    vec4(x, y, z, 1)
    );
}

mat4 genTransformMatrix() {
    mat4 scaleMatrix = genScaleMatrix(u_Scale, u_Scale, u_Scale);
    mat4 rotationMatrix = genRotationMatrix(u_RotationAngle);
    mat4 translationMatrix = genTranslationMatrix(u_Translation.x, u_Translation.y, 0.0);

    return scaleMatrix * rotationMatrix * translationMatrix;
}

mat4 genFontTransformMatrix(vec2 offset, float angle) {
    float DEFAULT_FONT_SIZE = 10.0;
    float amount = a_FontSize / DEFAULT_FONT_SIZE;
    mat4 magnifyMatrix = genTranslationMatrix(u_TextCenter.x, u_TextCenter.y, 0.0) * genScaleMatrix(amount, amount, amount) * genTranslationMatrix(-u_TextCenter.x, -u_TextCenter.y, 0.0);
    mat4 offsetMatrix = genTranslationMatrix(offset.x, offset.y, 0.0);
    mat4 rotationMatrix = genRotationMatrix(angle);

    return offsetMatrix * rotationMatrix * magnifyMatrix;
}

void main() {
    mat4 transformMatrix = genTransformMatrix();
    mat4 fontTransformMatrix;
    float totalAngle = a_FirstAngle + u_RotationAngle;
    if (totalAngle > 90.0 && totalAngle < 270.0) {
        fontTransformMatrix = genFontTransformMatrix(a_AltOffset, a_AltAngle);
    } else {
        fontTransformMatrix = genFontTransformMatrix(a_Offset, a_Angle);
    }

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * transformMatrix * fontTransformMatrix * a_Position;
    gl_PointSize = 10.0;
    v_Color = a_Color;
}
