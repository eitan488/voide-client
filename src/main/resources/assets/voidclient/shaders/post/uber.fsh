#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

// Void Client colour/screen "uber" pass. Every effect is gated by its own flag so a single
// fullscreen pass covers grading, tint, chromatic aberration, sharpening, vignette,
// film grain and CRT-style screen effects.
layout(std140) uniform VoidUber {
    vec4 Grading;       // exposure (stops), contrast, saturation, gamma
    vec4 Grading2;      // temperature, tint, hue shift (turns), vibrance
    vec4 TintColor;     // rgb, strength
    vec4 Vignette;      // intensity, radius, softness, roundness
    vec4 VignetteColor; // rgb, unused
    vec4 Chromatic;     // strength (px), radial (0/1), unused, unused
    vec4 Grain;         // intensity, size, animated (0/1), luminance response
    vec4 Sharpen;       // amount, radius (px), unused, unused
    vec4 Screen;        // scanline intensity, scanline density, curvature, pixelate size (px)
    vec4 Screen2;       // rgb mask intensity, flicker, unused, unused
    vec4 Flags1;        // grading, tint, chromatic, sharpen
    vec4 Flags2;        // vignette, grain, screen, unused
    vec4 OutSize;       // width, height, time seconds, unused
};

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float hash(vec2 p) {
    p = fract(p * vec2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

vec3 hueShift(vec3 c, float turns) {
    float a = turns * 6.2831853;
    vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

vec2 curve(vec2 uv, float amount) {
    vec2 c = uv * 2.0 - 1.0;
    vec2 off = abs(c.yx) / vec2(6.0 / max(amount, 0.0001));
    c = c + c * off * off;
    return c * 0.5 + 0.5;
}

vec3 sampleScene(vec2 uv) {
    if (Flags1.z > 0.5) {
        vec2 px = 1.0 / OutSize.xy;
        vec2 dir = Chromatic.y > 0.5 ? (uv - 0.5) * 2.0 : vec2(1.0, 0.0);
        vec2 off = dir * Chromatic.x * px;
        float r = texture(InSampler, uv + off).r;
        float g = texture(InSampler, uv).g;
        float b = texture(InSampler, uv - off).b;
        return vec3(r, g, b);
    }
    return texture(InSampler, uv).rgb;
}

void main() {
    vec2 uv = texCoord;
    bool screen = Flags2.z > 0.5;
    if (screen && Screen.z > 0.0) {
        uv = curve(uv, Screen.z);
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
        }
    }
    if (screen && Screen.w > 1.0) {
        vec2 cells = OutSize.xy / Screen.w;
        uv = (floor(uv * cells) + 0.5) / cells;
    }

    vec3 color = sampleScene(uv);

    if (Flags1.w > 0.5) {
        vec2 px = Sharpen.y / OutSize.xy;
        vec3 blur = (texture(InSampler, uv + vec2(px.x, 0.0)).rgb + texture(InSampler, uv - vec2(px.x, 0.0)).rgb
                   + texture(InSampler, uv + vec2(0.0, px.y)).rgb + texture(InSampler, uv - vec2(0.0, px.y)).rgb) * 0.25;
        color = clamp(color + (color - blur) * Sharpen.x, 0.0, 1.0);
    }

    if (Flags1.x > 0.5) {
        color *= exp2(Grading.x);
        color.r *= 1.0 + Grading2.x * 0.1;
        color.b *= 1.0 - Grading2.x * 0.1;
        color.g *= 1.0 + Grading2.y * 0.1;
        color = (color - 0.5) * Grading.y + 0.5;
        float l = luma(color);
        float sat = Grading.z;
        float maxc = max(color.r, max(color.g, color.b));
        float minc = min(color.r, min(color.g, color.b));
        float vib = Grading2.w * (1.0 - (maxc - minc));
        color = mix(vec3(l), color, sat + vib);
        if (abs(Grading2.z) > 0.0001) color = hueShift(color, Grading2.z);
        color = pow(max(color, vec3(0.0)), vec3(1.0 / max(Grading.w, 0.01)));
    }

    if (Flags1.y > 0.5) {
        color = mix(color, color * TintColor.rgb * 1.5 + TintColor.rgb * 0.08, TintColor.a);
    }

    if (Flags2.x > 0.5) {
        vec2 d = texCoord - 0.5;
        d.x *= mix(1.0, OutSize.x / OutSize.y, Vignette.w);
        float dist = length(d) * 1.41421356;
        float v = smoothstep(Vignette.y, Vignette.y - max(Vignette.z, 0.001), dist);
        color = mix(VignetteColor.rgb, color, mix(1.0, v, Vignette.x));
    }

    if (Flags2.y > 0.5) {
        vec2 cell = floor(texCoord * OutSize.xy / max(Grain.y, 1.0));
        float t = Grain.z > 0.5 ? floor(OutSize.z * 24.0) : 0.0;
        float n = hash(cell + t * 17.13) - 0.5;
        float response = mix(1.0, 1.0 - luma(color), Grain.w);
        color += n * Grain.x * response;
    }

    if (screen) {
        if (Screen.x > 0.0) {
            float line = sin(uv.y * OutSize.y * 3.14159 / max(Screen.y, 1.0));
            color *= 1.0 - Screen.x * (0.5 - 0.5 * line);
        }
        if (Screen2.x > 0.0) {
            int m = int(mod(gl_FragCoord.x, 3.0));
            vec3 mask = m == 0 ? vec3(1.0, 0.7, 0.7) : m == 1 ? vec3(0.7, 1.0, 0.7) : vec3(0.7, 0.7, 1.0);
            color *= mix(vec3(1.0), mask, Screen2.x);
        }
        if (Screen2.y > 0.0) color *= 1.0 - Screen2.y * 0.08 * (0.5 + 0.5 * sin(OutSize.z * 60.0));
    }

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
