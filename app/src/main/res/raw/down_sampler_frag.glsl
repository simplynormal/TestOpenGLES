precision mediump float;

uniform sampler2D u_TextureUnit;

uniform vec2 u_TexelStep;
uniform int u_ShowEdges;
uniform int u_FxaaOn;

uniform float u_LumaThreshold;
uniform float u_MulReduce;
uniform float u_MinReduce;
uniform float u_MaxSpan;

varying vec2 v_TexCoord;

void main() {
    vec3 rgbM = texture2D(u_TextureUnit, v_TexCoord).rgb;

    if (u_FxaaOn == 0)
    {
        gl_FragColor = vec4(rgbM, 1.0);
        return;
    }

    vec3 rgbNW = texture2D(u_TextureUnit, v_TexCoord + vec2(-u_TexelStep.x, u_TexelStep.y)).rgb;
    vec3 rgbNE = texture2D(u_TextureUnit, v_TexCoord + vec2(u_TexelStep.x, u_TexelStep.y)).rgb;
    vec3 rgbSW = texture2D(u_TextureUnit, v_TexCoord + vec2(-u_TexelStep.x, -u_TexelStep.y)).rgb;
    vec3 rgbSE = texture2D(u_TextureUnit, v_TexCoord + vec2(u_TexelStep.x, -u_TexelStep.y)).rgb;

    const vec3 toLuma = vec3(0.299, 0.587, 0.114);

    float lumaNW = dot(rgbNW, toLuma);
    float lumaNE = dot(rgbNE, toLuma);
    float lumaSW = dot(rgbSW, toLuma);
    float lumaSE = dot(rgbSE, toLuma);
    float lumaM = dot(rgbM, toLuma);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    if (lumaMax - lumaMin <= lumaMax * u_LumaThreshold)
    {
        gl_FragColor = vec4(rgbM, 1.0);
        return;
    }

    vec2 samplingDirection;
    samplingDirection.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    samplingDirection.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

    float samplingDirectionReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * 0.25 * u_MulReduce, u_MinReduce);

    float minSamplingDirectionFactor = 1.0 / (min(abs(samplingDirection.x), abs(samplingDirection.y)) + samplingDirectionReduce);

    samplingDirection = clamp(samplingDirection * minSamplingDirectionFactor, vec2(-u_MaxSpan), vec2(u_MaxSpan)) * u_TexelStep;

    vec3 rgbSampleNeg = texture2D(u_TextureUnit, v_TexCoord + samplingDirection * (1.0 / 3.0 - 0.5)).rgb;
    vec3 rgbSamplePos = texture2D(u_TextureUnit, v_TexCoord + samplingDirection * (2.0 / 3.0 - 0.5)).rgb;

    vec3 rgbTwoTab = (rgbSamplePos + rgbSampleNeg) * 0.5;

    vec3 rgbSampleNegOuter = texture2D(u_TextureUnit, v_TexCoord + samplingDirection * (0.0 / 3.0 - 0.5)).rgb;
    vec3 rgbSamplePosOuter = texture2D(u_TextureUnit, v_TexCoord + samplingDirection * (3.0 / 3.0 - 0.5)).rgb;
    vec3 rgbFourTab = (rgbSamplePosOuter + rgbSampleNegOuter) * 0.25 + rgbTwoTab * 0.5;

    float lumaFourTab = dot(rgbFourTab, toLuma);

    if (lumaFourTab < lumaMin || lumaFourTab > lumaMax)
    {
        gl_FragColor = vec4(rgbTwoTab, 1.0);
    }
    else
    {
        gl_FragColor = vec4(rgbFourTab, 1.0);
    }

    if (u_ShowEdges != 0)
    {
        gl_FragColor.r = 1.0;
    }
}
