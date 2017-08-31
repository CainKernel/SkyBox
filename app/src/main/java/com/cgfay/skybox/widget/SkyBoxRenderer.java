
package com.cgfay.skybox.widget;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.cgfay.skybox.camera.ICamera;
import com.cgfay.skybox.filter.CameraFilter;
import com.cgfay.skybox.filter.ParticleFilter;
import com.cgfay.skybox.filter.SkyBoxFilter;
import com.cgfay.skybox.util.GlUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SkyBoxRenderer implements GLSurfaceView.Renderer {
    private final Activity mContext;

    private int mViewWidth;
    private int mViewHeight;

    // 天空盒滤镜
    private SkyBoxFilter mSkyBoxFilter;
    // 粒子系统滤镜
    private ParticleFilter mParticleFilter;

    private ICamera mICamera;
    private Camera mCamera;

    private int mTextureID;
    private SurfaceTexture mSurfaceTexture;

    private CameraFilter mCameraFilter;

    public SkyBoxRenderer(Activity context) {
        mContext = context;
        mICamera = new ICamera();
    }

    /**
     * 设置旋转矩阵
     * @param matrix
     */
    public void setRotationMatrix(float[] matrix) {
        if (mSkyBoxFilter != null) {
            mSkyBoxFilter.setRotationMatrix(matrix);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // 创建天空盒
        mSkyBoxFilter = new SkyBoxFilter(mContext);
        mSkyBoxFilter.createProgram();

        // 创建粒子系统
        mParticleFilter = new ParticleFilter(mContext);
        mParticleFilter.createProgram();

        // 相机
        mTextureID = GlUtil.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener((SurfaceTexture.OnFrameAvailableListener) mContext);
        mCamera = mICamera.openCamera(false, mContext);

        mCameraFilter = new CameraFilter(mTextureID);
        mCameraFilter.createProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {                
        GLES20.glViewport(0, 0, width, height);
        mViewWidth = width;
        mViewHeight = height;
        // 调整视图大小
        if (mSkyBoxFilter != null) {
            mSkyBoxFilter.setViewSize(width, height);
        }
        if (mParticleFilter != null) {
            mParticleFilter.setViewSize(width, height);
        }
        // 开始预览
        mICamera.startPreview(mSurfaceTexture);
    }

    @Override    
    public void onDrawFrame(GL10 glUnused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // 绘制天空盒
        drawSkybox();
        updateTexImage();
        drawCameraFrame();
        // 绘制粒子系统
        drawParticles();
    }


    /**
     * 绘制天空盒
     */
    private void drawSkybox() {
        if (mSkyBoxFilter != null) {
            mSkyBoxFilter.drawSkyBox();
        }
    }

    /**
     * 更新SurfaceTexture的内容
     */
    private void updateTexImage() {
        // 更新SurfaceTexture 的内容
        float[] matrix = new float[16];
        mSurfaceTexture.getTransformMatrix(matrix);
        mSurfaceTexture.updateTexImage();
        mCameraFilter.setTextureMatrix(matrix);
    }


    /**
     * 绘制相机帧
     */
    private void drawCameraFrame() {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        mCameraFilter.drawFrame();
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    /**
     * 绘制粒子特效
     */
    private void drawParticles() {
        // 混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
        if (mParticleFilter != null) {
            mParticleFilter.drawParticle();
        }
        GLES20.glDisable(GLES20.GL_BLEND);

    }

    /**
     * 设置点击的位置
     * @param x
     * @param y
     */
    public void setDirection(float x, float y) {
        if (mParticleFilter != null) {
            mParticleFilter.setDirection(x, y, 0);
        }
    }


    /**
     * 返回宽度
     * @return
     */
    public int getWidth() {
        return mViewWidth;
    }

    /**
     * 返回高度
     * @return
     */
    public int getHeight() {
        return mViewHeight;
    }
}