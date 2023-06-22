uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;

uniform float u_RotationAngle;
uniform float u_Scale;
uniform vec2 u_Translation;

attribute vec4 a_Position;
attribute vec2 a_TexCoord;

varying vec2 v_TexCoord;

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

mat4 inverse(mat4 m) {
    float s0 = m[0][0] * m[1][1] - m[1][0] * m[0][1];
    float s1 = m[0][0] * m[1][2] - m[1][0] * m[0][2];
    float s2 = m[0][0] * m[1][3] - m[1][0] * m[0][3];
    float s3 = m[0][1] * m[1][2] - m[1][1] * m[0][2];
    float s4 = m[0][1] * m[1][3] - m[1][1] * m[0][3];
    float s5 = m[0][2] * m[1][3] - m[1][2] * m[0][3];

    float c5 = m[2][2] * m[3][3] - m[3][2] * m[2][3];
    float c4 = m[2][1] * m[3][3] - m[3][1] * m[2][3];
    float c3 = m[2][1] * m[3][2] - m[3][1] * m[2][2];
    float c2 = m[2][0] * m[3][3] - m[3][0] * m[2][3];
    float c1 = m[2][0] * m[3][2] - m[3][0] * m[2][2];
    float c0 = m[2][0] * m[3][1] - m[3][0] * m[2][1];

    // Should check for 0 determinant
    float invdet = 1.0 / (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);

    mat4 inv;
    inv[0][0] = ( m[1][1] * c5 - m[1][2] * c4 + m[1][3] * c3) * invdet;
    inv[0][1] = (-m[0][1] * c5 + m[0][2] * c4 - m[0][3] * c3) * invdet;
    inv[0][2] = ( m[3][1] * s5 - m[3][2] * s4 + m[3][3] * s3) * invdet;
    inv[0][3] = (-m[2][1] * s5 + m[2][2] * s4 - m[2][3] * s3) * invdet;

    inv[1][0] = (-m[1][0] * c5 + m[1][2] * c2 - m[1][3] * c1) * invdet;
    inv[1][1] = ( m[0][0] * c5 - m[0][2] * c2 + m[0][3] * c1) * invdet;
    inv[1][2] = (-m[3][0] * s5 + m[3][2] * s2 - m[3][3] * s1) * invdet;
    inv[1][3] = ( m[2][0] * s5 - m[2][2] * s2 + m[2][3] * s1) * invdet;

    inv[2][0] = ( m[1][0] * c4 - m[1][1] * c2 + m[1][3] * c0) * invdet;
    inv[2][1] = (-m[0][0] * c4 + m[0][1] * c2 - m[0][3] * c0) * invdet;
    inv[2][2] = ( m[3][0] * s4 - m[3][1] * s2 + m[3][3] * s0) * invdet;
    inv[2][3] = (-m[2][0] * s4 + m[2][1] * s2 - m[2][3] * s0) * invdet;

    inv[3][0] = (-m[1][0] * c3 + m[1][1] * c1 - m[1][2] * c0) * invdet;
    inv[3][1] = ( m[0][0] * c3 - m[0][1] * c1 + m[0][2] * c0) * invdet;
    inv[3][2] = (-m[3][0] * s3 + m[3][1] * s1 - m[3][2] * s0) * invdet;
    inv[3][3] = ( m[2][0] * s3 - m[2][1] * s1 + m[2][2] * s0) * invdet;

    return inv;
}


mat4 genTransformMatrix() {
    mat4 scaleMatrix = genScaleMatrix(u_Scale, u_Scale, u_Scale);
    mat4 rotationMatrix = genRotationMatrix(u_RotationAngle);
    mat4 translationMatrix = genTranslationMatrix(u_Translation.x, u_Translation.y, 0.0);
    mat4 uprightMatrix = genRotationMatrix(-u_RotationAngle);

    mat4 billboardMatrix = inverse(u_ModelViewMatrix);


    return scaleMatrix * rotationMatrix * translationMatrix * uprightMatrix * billboardMatrix;
}

void main() {
    mat4 transformMatrix = genTransformMatrix();
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * transformMatrix * a_Position;
    v_TexCoord = a_TexCoord;
}