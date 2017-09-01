package com.cgfay.skybox.filter;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cgfay.skybox.R;
import com.cgfay.skybox.util.Geometry;
import com.cgfay.skybox.util.GlUtil;
import com.cgfay.skybox.util.MatrixHelper;
import com.cgfay.skybox.util.ResourceUtil;
import com.cgfay.skybox.util.TextureHelper;

import java.nio.FloatBuffer;
import java.util.Random;

import static com.cgfay.skybox.camera.Constants.BYTES_PER_FLOAT;

/**
 * Created by cain.huang on 2017/9/1.
 */

public class ParticleFilterNew {
    // 最大粒子数
    private static final int MaxParticleCount = 1000;

    private Context mContext;
    private int mViewWidth;
    private int mViewHeight;

    // 创建粒子系统开始时间
    private long mGlobalStartTime;

    // GLSL句柄
    private int mProgramHandle;
    private int mMVPMatrixHandle;
    private int aPositionLocation;
    private int aColorLocation;
    private int aDirectionVectorLocation;
    private int aParticleStartTimeLocation;
    private int mPointsizeLocation;

    private int mParticleTexture;

    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];


    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int VECTOR_COMPONENT_COUNT = 3;
    private static final int PARTICLE_START_TIME_COMPONENT_COUNT = 1;

    private static final int TOTAL_COMPONENT_COUNT =
            POSITION_COMPONENT_COUNT
                    + COLOR_COMPONENT_COUNT
                    + VECTOR_COMPONENT_COUNT
                    + PARTICLE_START_TIME_COMPONENT_COUNT;

    private static final int STRIDE = TOTAL_COMPONENT_COUNT * BYTES_PER_FLOAT;

    private float[] mParticles;
    private FloatBuffer mVertexBuffer;
    private int mNextParticle;



    // ParticleShooter内容
    private Geometry.Point mPosition; // 起始位置
    private int mColor; // 颜色

    private float mAngleVariance;
    private float mSpeedVariance;

    private Random mRandom = new Random();

    // 旋转矩阵
    private float[] mRotationMatrix = new float[16];
    // 目的向量
    private float[] mDirectionVector = new float[4];
    // 最终的结果
    private float[] mResultVector = new float[4];


    public ParticleFilterNew(Context context) {
        mContext = context;
        mGlobalStartTime = System.nanoTime();

        mPosition = new Geometry.Point(0f, 0f, 0f);
        mDirectionVector[0] = 0.0f;
        mDirectionVector[1] = 0.5f;
        mDirectionVector[2] = 0.0f;
        mAngleVariance = 5f;
        mSpeedVariance = 1.0f;
        mColor = Color.rgb(255, 255, 255);

        // 创建粒子
        mParticles = new float[MaxParticleCount * TOTAL_COMPONENT_COUNT];
        mVertexBuffer = GlUtil.createFloatBuffer(mParticles);
    }

    /**
     * 创建Program
     */
    public void createProgram() {
        String vertexShader = ResourceUtil.readTextFileFromResource(mContext,
                R.raw.vertex_particle_new);
        String fragmentShader = ResourceUtil.readTextFileFromResource(mContext,
                R.raw.fragment_particle_new);
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        mPointsizeLocation = GLES20.glGetUniformLocation(mProgramHandle, "uPointSize");

        aPositionLocation = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        aColorLocation = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
        aDirectionVectorLocation = GLES20.glGetAttribLocation(mProgramHandle, "a_DirectionVector");
        aParticleStartTimeLocation = GLES20.glGetAttribLocation(mProgramHandle, "a_ParticleStartTime");

        // 创建粒子的Texture
        mParticleTexture = TextureHelper.loadTexture(mContext, R.drawable.particle_texture);
    }

    /**
     * 更新视图大小
     * @param width
     * @param height
     */
    public void setViewSize(int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        // 设置透视矩阵
        float ratio = (float) width / (float) height;
        MatrixHelper.perspectiveM(mProjectionMatrix, 45, ratio, 1f, 300f);
    }

    /**
     * 绘制粒子
     */
    public void drawParticle() {
        GLES20.glUseProgram(mProgramHandle);
        float currentTime = (System.nanoTime() - mGlobalStartTime) / 1000000000f;
        calculate(currentTime, 1);
        calculateMatirx();
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mParticleTexture);
        bindAttributeData();
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        GLES20.glUseProgram(0);
    }

    private void bindAttributeData() {
        int dataOffset = 0;
        // position
        mVertexBuffer.position(dataOffset);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexBuffer);
        mVertexBuffer.position(0);
        dataOffset += POSITION_COMPONENT_COUNT;

        // color
        mVertexBuffer.position(dataOffset);
        GLES20.glEnableVertexAttribArray(aColorLocation);
        GLES20.glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexBuffer);
        mVertexBuffer.position(0);
        dataOffset += COLOR_COMPONENT_COUNT;

        // direction
        mVertexBuffer.position(dataOffset);
        GLES20.glEnableVertexAttribArray(aDirectionVectorLocation);
        GLES20.glVertexAttribPointer(aDirectionVectorLocation, VECTOR_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexBuffer);
        mVertexBuffer.position(0);
        dataOffset += VECTOR_COMPONENT_COUNT;

        // startTime
        mVertexBuffer.position(dataOffset);
        GLES20.glEnableVertexAttribArray(aParticleStartTimeLocation);
        GLES20.glVertexAttribPointer(aParticleStartTimeLocation,
                PARTICLE_START_TIME_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, STRIDE, mVertexBuffer);
        mVertexBuffer.position(0);
    }

    /**
     * 计算粒子
     * @param time
     * @param count
     */
    private void calculate(float time, int count) {
        for (int i = 0; i < count; i++) {
            // 随机产生(-1.0 ~ 1.0)之间的起始位置
            float startX = mRandom.nextFloat() * 2 - 1.0f;
            float startY = mRandom.nextFloat() * 2 - 1.0f;
            mPosition = new Geometry.Point(startX, startY, 0);

            // 设置欧拉角
            Matrix.setRotateEulerM(mRotationMatrix, 0,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance);

            // 向量矩阵乘法
            Matrix.multiplyMV(mResultVector, 0, mRotationMatrix, 0, mDirectionVector, 0);

            // 调整速度
            float speedAdjustment = 1f + mRandom.nextFloat() * mSpeedVariance;

            // 计算最终的方向向量
            Geometry.Vector direction = new Geometry.Vector(
                    mResultVector[0] * speedAdjustment,
                    mResultVector[1] * speedAdjustment,
                    mResultVector[2] * speedAdjustment);

            addParticle(mPosition, mColor, direction, time);
        }
    }

    /**
     * 添加粒子
     * @param position
     * @param color
     * @param direction
     * @param startTime
     */
    private void addParticle(Geometry.Point position, int color,
                             Geometry.Vector direction, float startTime) {
        final int particleOffset = mNextParticle * TOTAL_COMPONENT_COUNT;
        int currentOffset = particleOffset;
        mNextParticle++;

        if (mNextParticle == MaxParticleCount) {
            mNextParticle = 0;
        }

        // 原始位置
        mParticles[currentOffset++] = position.x;
        mParticles[currentOffset++] = position.y;
        mParticles[currentOffset++] = position.z;

        // 颜色
        mParticles[currentOffset++] = Color.red(color) / 255f;
        mParticles[currentOffset++] = Color.green(color) / 255f;
        mParticles[currentOffset++] = Color.blue(color) / 255f;

        // 目的位置
        mParticles[currentOffset++] = direction.x;
        mParticles[currentOffset++] = direction.y;
        mParticles[currentOffset++] = direction.z;

        mParticles[currentOffset++] = startTime;

        mVertexBuffer.position(particleOffset);
        mVertexBuffer.put(mParticles, particleOffset, TOTAL_COMPONENT_COUNT);
        mVertexBuffer.position(0);
    }

    /**
     * 计算总变换
     */
    private void calculateMatirx() {
        // 计算综合矩阵
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.translateM(mViewMatrix, 0, 0f, 0f, -5f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    /**
     * 设置点击的位置
     * @param x
     * @param y
     * @param z
     */
    public void setDirection(float x, float y, float z) {
        mDirectionVector[0] = x;
        mDirectionVector[1] = y;
        mDirectionVector[2] = z;
    }

}