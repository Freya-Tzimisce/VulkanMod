#version 450

#include "projection.glsl"

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;

layout(binding = 0) uniform UniformBufferObject {
    mat4 ModelViewMat;
    mat4 ProjMat;
    int FogShape;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(location = 0) out vec4 shimmer;
layout(location = 1) out vec2 texCoordBlock;
layout(location = 2) out vec4 texProjSky;
layout(location = 3) out vec2 texCoordGlint;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    texProjSky = projection_from_position(gl_Position);
    texCoordBlock = UV0;
    texCoordGlint = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;

    shimmer = Color;
}