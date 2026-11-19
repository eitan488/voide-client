#version 330

uniform sampler2D InSampler;
uniform sampler2D HistorySampler;

layout(std140) uniform VoidMotion {
    vec4 Params;   // history weight (0..0.95), unused...
};

in vec2 texCoord;
out vec4 fragColor;

// Frame accumulation motion blur: blends the new frame with the accumulated history.
void main() {
    vec3 cur = texture(InSampler, texCoord).rgb;
    vec3 hist = texture(HistorySampler, texCoord).rgb;
    fragColor = vec4(mix(cur, hist, Params.x), 1.0);
}
