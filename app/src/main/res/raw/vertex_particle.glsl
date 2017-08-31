uniform mat4 uMVPMatrix;
uniform float uTime;
uniform int a_OpenMouth;

attribute vec3 a_Position;  
attribute vec3 a_Color;
attribute vec3 a_DirectionVector;
attribute float a_ParticleStartTime;


varying vec3 v_Color;
varying float v_ElapsedTime;

void main()
{
    // 颜色
    v_Color = a_Color;
    // 时间经过的
    v_ElapsedTime = uTime - a_ParticleStartTime;
    // 重力加速度
    float gravityFactor = v_ElapsedTime * v_ElapsedTime / 8.0;
    // 当前位置
    vec3 currentPosition = a_Position + (a_DirectionVector * v_ElapsedTime);
    // 如果张开嘴巴，则将当前位置运动到嘴巴中心点
    if (a_OpenMouth == 1) {
        currentPosition = a_Position + ((a_DirectionVector - a_Position) * v_ElapsedTime);
    } else {
        currentPosition.y -= gravityFactor;
    }
    gl_Position = uMVPMatrix * vec4(currentPosition, 1.0);
    gl_PointSize = 25.0;
}