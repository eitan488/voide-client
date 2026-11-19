#version 330

uniform sampler2D InSampler;

layout(std140) uniform VoidBloom {
    vec4 Params;    // threshold, knee, intensity, saturation boost
    vec4 Color;     // rgb tint, tint mix
    vec4 Size;      // src width, src height, dst width, dst height
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // 4-tap box downsample to reduce shimmering
    vec2 px = 1.0 / Size.xy;
    vec3 c = (texture(InSampler, texCoord + vec2(-px.x, -px.y)).rgb + texture(InSampler, texCoord + vec2(px.x, -px.y)).rgb
            + texture(InSampler, texCoord + vec2(-px.x, px.y)).rgb + texture(InSampler, texCoord + vec2(px.x, px.y)).rgb) * 0.25;
    float br = max(c.r, max(c.g, c.b));
    float knee = Params.x * Params.y + 0.00001;
    float soft = clamp(br - Params.x + knee, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee);
    float contrib = max(soft, br - Params.x) / max(br, 0.00001);
    vec3 outc = c * contrib;
    float l = dot(outc, vec3(0.2126, 0.7152, 0.0722));
    outc = mix(vec3(l), outc, 1.0 + Params.w);
    outc = mix(outc, l * Color.rgb * 1.5, Color.a);
    fragColor = vec4(max(outc, 0.0), 1.0);
}
