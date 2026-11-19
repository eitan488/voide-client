#version 330

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

// Depth effects: linear depth fog tint + depth-of-field disk blur. Runs before the hand
// is drawn so the hand stays sharp and unfogged.
layout(std140) uniform VoidDepth {
    vec4 Fog;        // start, end, strength, enabled
    vec4 FogColor;   // rgb, unused
    vec4 Focus;      // focus distance, focus range, max blur (px), enabled
    vec4 Planes;     // near, far, width, height
};

in vec2 texCoord;
out vec4 fragColor;

float linearDepth(float d) {
    float z = d * 2.0 - 1.0;
    float n = Planes.x, f = Planes.y;
    return (2.0 * n * f) / (f + n - z * (f - n));
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    float dist = depth >= 1.0 ? Planes.y : linearDepth(depth);
    vec3 color = texture(InSampler, texCoord).rgb;

    if (Focus.w > 0.5) {
        float coc = clamp((abs(dist - Focus.x) - Focus.y) / max(Focus.y, 0.5), 0.0, 1.0) * Focus.z;
        if (coc > 0.5) {
            vec2 px = 1.0 / Planes.zw;
            vec3 acc = color;
            float total = 1.0;
            const int TAPS = 16;
            for (int i = 0; i < TAPS; i++) {
                float a = float(i) * 2.39996323;
                float r = sqrt(float(i + 1) / float(TAPS)) * coc;
                vec2 off = vec2(cos(a), sin(a)) * r * px;
                acc += texture(InSampler, texCoord + off).rgb;
                total += 1.0;
            }
            color = acc / total;
        }
    }

    if (Fog.w > 0.5 && depth < 1.0) {
        float f = clamp((dist - Fog.x) / max(Fog.y - Fog.x, 0.001), 0.0, 1.0) * Fog.z;
        color = mix(color, FogColor.rgb, f);
    }

    fragColor = vec4(color, 1.0);
}
