#version 330

uniform sampler2D InSampler;

layout(std140) uniform VoidBlur {
    vec4 Dir;   // direction x, direction y, texel width, texel height
};

in vec2 texCoord;
out vec4 fragColor;

// 9-tap gaussian using linear filtering (5 fetches)
void main() {
    vec2 step = Dir.xy * Dir.zw;
    vec3 c = texture(InSampler, texCoord).rgb * 0.2270270270;
    c += texture(InSampler, texCoord + step * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord - step * 1.3846153846).rgb * 0.3162162162;
    c += texture(InSampler, texCoord + step * 3.2307692308).rgb * 0.0702702703;
    c += texture(InSampler, texCoord - step * 3.2307692308).rgb * 0.0702702703;
    fragColor = vec4(c, 1.0);
}
