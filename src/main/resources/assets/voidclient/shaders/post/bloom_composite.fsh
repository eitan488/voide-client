#version 330

uniform sampler2D InSampler;

layout(std140) uniform VoidBloom {
    vec4 Params;    // threshold, knee, intensity, saturation boost
    vec4 Color;     // rgb tint, tint mix
    vec4 Size;      // unused here
};

in vec2 texCoord;
out vec4 fragColor;

// Drawn with additive blending onto the main framebuffer.
void main() {
    fragColor = vec4(texture(InSampler, texCoord).rgb * Params.z, 1.0);
}
