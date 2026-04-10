#version 450

layout(binding = 0) uniform sampler2D Sampler0;
layout(binding = 1) uniform sampler2D Sampler1;
layout(binding = 2) uniform sampler2D Sampler2;

layout(location = 0) in vec4 shimmer;
layout(location = 1) in vec2 texCoordBlock;
layout(location = 2) in vec4 texProjSky;
layout(location = 3) in vec2 texCoordGlint;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoordBlock);
    vec4 skyColor = textureProj(Sampler1, texProjSky);
    vec4 glintColor = texture(Sampler2, texCoordGlint) * shimmer;

    fragColor = mix(skyColor, textureColor, glintColor * textureColor.a);
}