package com.cgfay.skybox.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cgfay.skybox.R;
import com.cgfay.skybox.util.GlUtil;
import com.cgfay.skybox.util.MatrixHelper;
import com.cgfay.skybox.util.ResourceUtil;
import com.cgfay.skybox.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * 天空盒滤镜
 * Created by cain.huang on 2017/8/31.
 */
public class SkyBoxFilter {

    private static final int COORDS_PER_VERTEX = 3;

    // 立方体坐标
    private static final float[] CubeCoords = new float[] {
            -1,  1,  1,     // (0) Top-left near
            1,   1,  1,     // (1) Top-right near
            -1, -1,  1,     // (2) Bottom-left near
            1,  -1,  1,     // (3) Bottom-right near
            -1,  1, -1,     // (4) Top-left far
            1,   1, -1,     // (5) Top-right far
            -1, -1, -1,     // (6) Bottom-left far
            1,  -1, -1      // (7) Bottom-right far
    };

    // 立方体索引
    private static final byte[] CubeIndex = new byte[] {
            // Front
            1, 3, 0,
            0, 3, 2,

            // Back
            4, 6, 5,
            5, 6, 7,

            // Left
            0, 2, 4,
            4, 2, 6,

            // Right
            5, 7, 1,
            1, 7, 3,

            // Top
            5, 1, 4,
            4, 1, 0,

            // Bottom
            6, 2, 7,
            7, 2, 3
    };

    private Context mContext;

    // 视图宽高
    private int mViewWidth;
    private int mViewHeight;

    private FloatBuffer mVertexBuffer;
    private ByteBuffer mIndexBuffer;

    private int mProgramHandle;

    private int muMatrixHandle;
    private int muTextureUnitHandle;
    private int maPositionHandle;

    // Cube纹理
    private int mSkyboxTexture;

    // 变换矩阵
    private float[] mRotationMatrix = new float[16]; // 旋转矩阵
    private float[] mViewMatrix = new float[16];    // 视图矩阵
    private float[] mProjectionMatrix = new float[16]; // 投影矩阵
    private float[] mMVPMatrix = new float[16]; // 总变换矩阵


    public SkyBoxFilter(Context context) {
        mContext = context;
        mVertexBuffer = GlUtil.createFloatBuffer(CubeCoords);
        mIndexBuffer = GlUtil.createByteBuffer(CubeIndex);
        initMatrix();
    }

    /**
     * 初始化Matrix
     */
    private void initMatrix() {
        Matrix.setIdentityM(mRotationMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    /**
     * 创建Program
     */
    public void createProgram() {
        mProgramHandle = GlUtil.createProgram(ResourceUtil
                        .readTextFileFromResource(mContext, R.raw.vertex_skybox),
                ResourceUtil
                        .readTextFileFromResource(mContext, R.raw.fragment_skybox));
        muMatrixHandle = glGetUniformLocation(mProgramHandle, "u_Matrix");
        muTextureUnitHandle = glGetUniformLocation(mProgramHandle, "u_TextureUnit");
        maPositionHandle = glGetAttribLocation(mProgramHandle, "a_Position");
        mSkyboxTexture =  TextureHelper.loadCubeMap(mContext,
                new int[] {
                        R.drawable.left, R.drawable.right,
                        R.drawable.bottom, R.drawable.top,
                        R.drawable.front, R.drawable.back,
                });
    }

    /**
     * 更新视图大小，用于设置视图矩阵和透视矩阵
     * @param width
     * @param height
     */
    public void setViewSize(int width, int height) {
        mViewWidth = width;
        mViewHeight = height;

        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f,
                0f, 1.0f, 0.0f);

        // 设置透视矩阵
        float ratio = (float) width / (float) height;
        MatrixHelper.perspectiveM(mProjectionMatrix, 45, ratio, 1f, 300f);
    }

    /**
     * 绘制天空盒
     */
    public void drawSkyBox() {

        GLES20.glUseProgram(mProgramHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, mSkyboxTexture);
        calculateMatrix();
        GLES20.glUniformMatrix4fv(muMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1i(muTextureUnitHandle, 0);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, mIndexBuffer);
        GLES20.glUseProgram(0);
    }

    /**
     * 计算总变换
     */
    private void calculateMatrix() {
        // 计算综合矩阵
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f,
                0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mViewMatrix, 0, mViewMatrix, 0, mRotationMatrix, 0);
        Matrix.rotateM(mViewMatrix, 0, 90, 1f, 0f, 0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    /**
     * 设置旋转矩阵
     * @param matrix
     */
    public void setRotationMatrix(float[] matrix) {
        mRotationMatrix = matrix;
    }


}
