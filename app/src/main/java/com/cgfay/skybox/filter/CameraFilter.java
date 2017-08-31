package com.cgfay.skybox.filter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cgfay.skybox.util.GlUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CameraFilter {

	private final String VertexShader =
			"attribute vec4 vPosition;									\n" +
			"attribute vec2 inputTextureCoordinate;						\n" +
			"varying vec2 textureCoordinate;							\n" +
			"void main() {												\n" +
			"    gl_Position = vPosition;								\n" +
			"    gl_PointSize = 10.0;									\n" +
			"    textureCoordinate = inputTextureCoordinate;			\n" +
			"}";

	private final String FragmentShader =
			"#extension GL_OES_EGL_image_external : require				\n" +
			"precision mediump float;									\n" +
			"varying vec2 textureCoordinate;							\n" +
			"uniform samplerExternalOES s_texture;						\n" +
			"void main() {												\n" +
			"  gl_FragColor = texture2D(s_texture, textureCoordinate);	\n" +
			"}";

	private FloatBuffer mVertexBuffer, mTextureBuffer;
	private ShortBuffer mDrawListBuffer;

	// GLSL句柄
	private int mProgramHandle;
	private int mPositionHandle;
	private int mTextureCoordHandle;

	// 顶点绘制顺序
	private short mDrawOrder[] = { 0, 1, 2, 0, 2, 3 };
	// 顶点坐标数
	private static final int COORDS_PER_VERTEX = 2;
	// 顶点步幅
	private final int mVertexStride = COORDS_PER_VERTEX * 4;
	// 直角坐标系
	private static float SquareCoords[] = {
			-1.0f, 1.0f,	// left, top
			-1.0f, -1.0f,	// left, bottom
			1.0f, -1.0f,	// right, bottom
			1.0f, 1.0f,		// right, top
	};

	// 结构顶点（8个数字表示了4个点x,y的位置.大小在0-1之间）
	private static float TextureVertices[] = {
			1.0f, 1.0f,	// right, top
			0.0f, 1.0f,	// left, top
			0.0f, 0.0f,	// left，bottom
			1.0f, 0.0f,	// right，bottom
	};

	private int mTextureID;
	private int[] mFramebuffers;
	private int[] mFramebufferTextures;
	private int mFrameWidth = -1;
	private int mFrameHeight = -1;

	// SurfaceTexture 的Matrix
	private float[] mMatrix = TextureVertices;

	public CameraFilter(int textureID) {
		mTextureID = textureID;
		mVertexBuffer = GlUtil.createFloatBuffer(SquareCoords);
		mDrawListBuffer = GlUtil.createShortBuffer(mDrawOrder);
		mTextureBuffer = GlUtil.createFloatBuffer(TextureVertices);

	}

	/**
	 * 创建并绑定Program
	 */
	public void createProgram() {
		mProgramHandle = GlUtil.createProgram(VertexShader, FragmentShader);
		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "vPosition");
		mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle,
				"inputTextureCoordinate");
	}

	/**
	 * 绘制帧
	 */
	public void drawFrame() {
		// 1、使用Program
		GLES20.glUseProgram(mProgramHandle);
		// 2、绑定Texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
		// 3、使能属性并绑定Buffer
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, mVertexStride, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

		GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, mVertexStride, mTextureBuffer);

		// 4、绘制
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mDrawOrder.length,
				GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
		// 5、禁用句柄
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
		// 6、释放program
		GLES20.glUseProgram(0);
	}

	/**
	 * 将相机图像绘制到Framebuffer
	 * @param matrix SurfaceTexture 的Matrix
	 * @return
	 */
	public int drawToFramebuffer(float[] matrix) {
		// 1、使用Program
		GLES20.glUseProgram(mProgramHandle);
		// 2、绑定Framebuffer 和 Texture
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffers[0]);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
		// 3、使能属性并绑定Buffer
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, mVertexStride, mVertexBuffer);
		GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
		GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, mVertexStride, mTextureBuffer);
		// 4、绘制
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mDrawOrder.length,
				GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
		// 5、禁用句柄
		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
		// 6、解除Framebuffer的绑定
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glUseProgram(0);
		return mFramebufferTextures[0];
	}

	/**
	 * 图像旋转
	 */
	private float[] transformTextureCoordinates(float[] coords, float[] matrix) {
		float[] result = new float[coords.length];
		float[] vt = new float[4];

		for (int i = 0; i < coords.length; i += 2) {
			float[] v = { coords[i], coords[i + 1], 0, 1 };
			Matrix.multiplyMV(vt, 0, matrix, 0, v, 0);
//			result[i] = vt[0];// x轴镜像
			result[i] = coords[i];
//			result[i + 1] = vt[1]; // y轴镜像
			result[i + 1] = coords[i + 1];
		}
		return result;
	}


	/**
	 * 设置SurtfaceTexture 的 matrix
	 * @param matrix
	 */
	public void setTextureMatrix(float[] matrix) {
		mMatrix = transformTextureCoordinates(TextureVertices, matrix);
		mTextureBuffer = GlUtil.createFloatBuffer(mMatrix);
	}

	/**
	 * 初始化Framebuffer
	 * @param width
	 * @param height
	 */
	public void initCameraFramebuffer(int width, int height) {
		if (mFramebuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
			destroyFramebuffer();
		}
		if (mFramebuffers == null) {
			mFrameWidth = width;
			mFrameHeight = height;
			mFramebuffers = new int[1];
			mFramebufferTextures = new int[1];
			GlUtil.createSampler2DFrameBuff(mFramebuffers, mFramebufferTextures, width, height);
		}
	}

	/**
	 * 销毁Framebuffer
	 */
	public void destroyFramebuffer() {
		if (mFramebufferTextures != null) {
			GLES20.glDeleteTextures(1, mFramebufferTextures, 0);
			mFramebufferTextures = null;
		}

		if (mFramebuffers != null) {
			GLES20.glDeleteFramebuffers(1, mFramebuffers, 0);
			mFramebuffers = null;
		}
	}

}
