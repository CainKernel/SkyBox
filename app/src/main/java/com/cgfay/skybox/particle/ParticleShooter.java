package com.cgfay.skybox.particle;

import java.util.Random;

/**
 * 粒子射击系统，用于控制粒子的运动
 * Created by cain.huang on 2017/8/31.
 */
public class ParticleShooter {
    // 最大射击粒子数
    private static final int MAX_PARTICLE = 100;

    private static final float mSlowDown = 2f;


    private static final float mMouthOffset = 0.05f;

    //存储12中不同的颜色.对每一个颜色从0到11
    private float mColors[][] = {
            {1.0f, 0.5f, 0.5f},
            {1.0f, 0.75f, 0.5f},
            {1.0f, 1.0f, 0.5f},
            {0.75f, 1.0f, 0.5f},
            {0.5f, 1.0f, 0.5f},
            {0.5f, 1.0f, 0.75f},
            {0.5f, 1.0f, 1.0f},
            {0.5f, 0.75f, 1.0f},
            {0.5f, 0.5f, 1.0f},
            {0.75f, 0.5f, 1.0f},
            {1.0f, 0.5f, 1.0f},
            {1.0f, 0.5f, 0.75f}
    };

    // 存放粒子数组
    private Particle mParticles[] = new Particle[MAX_PARTICLE];

    // 随机过程
    private Random mRandom = new Random();

    private int mColorIndex = 0; // 颜色位置（随机）

    // 点击时的位置
    private float[] mDirection = new float[4];

    // 是否吃东西的动作
    private boolean isEating = false;






    public ParticleShooter() {

    }


    /**
     * 计算顶点
     */
    public void calculateParticlePoints() {
        for (int index = 0; index < MAX_PARTICLE; index++) {
            if (mParticles[index].active) {

                // 计算变化的位移
                float deltaX = mParticles[index].xi / (mSlowDown * 1000);
                float deltaY = mParticles[index].yi / (mSlowDown * 1000);
                float deltaZ = mParticles[index].zi / (mSlowDown * 1000);

                //更新粒子的位置
                mParticles[index].x += deltaX;
                // 更新Y坐标的位置
                mParticles[index].y += deltaY;
                // 更新Z坐标的位置
                mParticles[index].z += deltaZ;

                // 如果是吃了豆子的动作，需要判断豆子是否处于当前范围内，将粒子的生命重置为-1
                if (isEating && isNearMouth(mParticles[index])) {
                    mParticles[index].live = -1;
                }

                // 更新X轴方向速度大小
                mParticles[index].xi += mParticles[index].xg;
                // 更新Y轴方向速度大小
                mParticles[index].yi += mParticles[index].yg;
                // 更新Z轴方向速度大小
                mParticles[index].zi += mParticles[index].zg;
                // 减少粒子的生命值
                mParticles[index].live -= mParticles[index].decrease;

                // 如果粒子生命小于0，重新创建粒子
                if (mParticles[index].live < 0.0f) {

                    mParticles[index] = new Particle();
                    mParticles[index].active = true;
                    mParticles[index].live = 1.0f;
                    mParticles[index].decrease = (float)(rand1000() % 100) / 1000.0f + 0.005f;

                    // 重新设定粒子的x,y和z位置为随机
                    mParticles[index].x = (mRandom.nextFloat() * 2.0f - 1.0f);
                    mParticles[index].y = (mRandom.nextFloat() * 2.0f - 1.0f);
                    mParticles[index].z = 0;
                    // 如果是吃了豆子的动作，则判断新生成的豆子是否处于某个范围内，需要将豆子移动到范围之外。

                    //在粒子从新设置之后,将给它新的移动速度/方向
                    mParticles[index].xi = (float)((rand1000() % 50) - 33.0f)* 12.0f;//x方向
                    mParticles[index].yi = (float)((rand1000() % 50) - 33.0f) * 12.0f;//y方向

                    //最后我们分配粒子一种新的颜色
                    mParticles[index].r = mColors[mColorIndex][0];
                    mParticles[index].g = mColors[mColorIndex][1];
                    mParticles[index].b = mColors[mColorIndex][2];
                }
            }
        }
    }

    /**
     * 判断某个粒子是否处与嘴巴附近
     * @param particle
     * @return
     */
    private boolean isNearMouth(Particle particle) {
        if (Math.abs(particle.x - mDirection[0]) < mMouthOffset
                || Math.abs(particle.y - mDirection[1]) < mMouthOffset) {
            return true;
        }

        return false;
    }


    /**
     * 产生1000以内的随机数
     * @return
     */
    private int rand1000() {
        return mRandom.nextInt(1000);
    }

    /**
     * 产生100以内的随机数
     * @return
     */
    private int rand100() {
        return mRandom.nextInt(100);
    }

    /**
     * 设置点击的位置
     * @param x
     * @param y
     * @param z
     */
    public void setDirection(float x, float y, float z) {
        mDirection[0] = x;
        mDirection[1] = y;
        mDirection[2] = z;
    }

    /**
     * 是否处于吃东西的动作
     * @param eating
     */
    public void setEating(boolean eating) {
        isEating = eating;
    }


}