package com.senao.ocrreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.opengl.GLES10;
import android.view.View;
import android.view.View.MeasureSpec;

public class BitmapHandler
{

	private static int	msMaxTexture	= getMaxTexDim();

	public BitmapHandler()
	{
		super();
	}

	private static int getMaxTexDim()
	{
		int[] maxTextureSize = new int[1];
		GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
		return maxTextureSize[0];
	}

	/**
	  * 以最省內存的方式讀取本地資源的圖片
	  * @param context
	  * @param resId
	  * @return
	  */
	public static Bitmap readBitmap(Context context, int resId)
	{
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
		opt.inPurgeable = true;
		opt.inInputShareable = true;
		//獲取資源圖片
		InputStream is = context.getResources().openRawResource(resId);
		return BitmapFactory.decodeStream(is, null, opt);
	}

	public static Bitmap readBitmap(String strFilePath, int reqWidth, int reqHeight, boolean bResize)
	{
		if (null == strFilePath)
		{
			return null;
		}

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(strFilePath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		try
		{

			if (0 >= msMaxTexture)
			{
				msMaxTexture = getMaxTexDim();
				if (0 >= msMaxTexture)
				{
					msMaxTexture = 4096;
				}
			}
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			options.inPurgeable = true;
			BitmapFactory.decodeFile(strFilePath, options);
			options.inJustDecodeBounds = false;
			float fWidth = reqWidth;
			float fHeight = reqHeight;
			float fScale = 1.0f;
			if (reqWidth > reqHeight && msMaxTexture < reqWidth)
			{
				fWidth = msMaxTexture;
				fScale = reqWidth / msMaxTexture;
				fHeight = reqHeight / fScale;
			}

			if (reqWidth < reqHeight && msMaxTexture < reqHeight)
			{
				fHeight = msMaxTexture;
				fScale = reqHeight / msMaxTexture;
				fWidth = reqWidth / fScale;
			}

			if (reqWidth == reqHeight && msMaxTexture < reqWidth)
			{
				fWidth = msMaxTexture;
				fHeight = msMaxTexture;
			}

			int nWidth = (int) Math.floor(fWidth);
			int nHeight = (int) Math.floor(fHeight);
			options.inSampleSize = calculateInSampleSize(options, nWidth, nHeight);
			Bitmap originalBitmap = BitmapFactory.decodeStream(fis, null, options);
			//Bitmap originalBitmap = BitmapFactory.decodeFile(strFilePath, options);
			fis.close();
			fis = null;
			if (!bResize)
			{
				return originalBitmap;
			}

			options.inDither = false;
			options.inPurgeable = true;
			options.inInputShareable = true;
			options.inSampleSize = 1;
			options.inTempStorage = new byte[16 * 1024];
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, nWidth, nHeight, true);//getResizedBitmap(originalBitmap, nWidth, nHeight);
			if (originalBitmap != resizedBitmap)
			{
				originalBitmap.recycle();
			}

			return resizedBitmap;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;

	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{
			final int heightRatio = (int) Math.floor((float) height / (float) reqHeight);
			final int widthRatio = (int) Math.floor((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight)
	{
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}

	/** 
	 * 合併兩張bitmap為一張 
	 * @param background 
	 * @param foreground 
	 * @return Bitmap 
	 */
	public static Bitmap combineBitmap(Bitmap background, Bitmap foreground, float left, float top)
	{
		if (background == null)
		{
			return null;
		}
		Bitmap newmap = Bitmap.createBitmap(background.getWidth(), background.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(newmap);
		canvas.drawBitmap(background, 0, 0, null);
		canvas.drawBitmap(foreground, left, top, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		return newmap;
	}

	public static Bitmap getScreenshotsForCurrentWindow(Activity activity)
	{
		View cv = activity.getWindow().getDecorView();
		Bitmap bmp = Bitmap.createBitmap(cv.getWidth(), cv.getHeight(), Bitmap.Config.ARGB_4444);
		cv.draw(new Canvas(bmp));
		return bmp;
	}

	public static Bitmap cutBitmap(Bitmap mBitmap, Rect r, Bitmap.Config config)
	{
		int width = r.width();
		int height = r.height();

		Bitmap croppedImage = Bitmap.createBitmap(width, height, config);

		Canvas cvs = new Canvas(croppedImage);
		Rect dr = new Rect(0, 0, width, height);
		cvs.drawBitmap(mBitmap, r, dr, null);
		return croppedImage;
	}

	public static int getBitmapWidth(String strFilePath)
	{
		if (null == strFilePath)
		{
			return 0;
		}

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(strFilePath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPurgeable = true;
		BitmapFactory.decodeStream(fis, null, bitmapOptions);
		return bitmapOptions.outWidth;
	}

	public static int getBitmapHeight(String strFilePath)
	{
		if (null == strFilePath)
		{
			return 0;
		}

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(strFilePath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPurgeable = true;
		BitmapFactory.decodeStream(fis, null, bitmapOptions);

		return bitmapOptions.outHeight;

	}

	public static Bitmap readBitmapThumbnail(Context context, String strFilePath, int nSampleSize)
	{
		if (null == strFilePath)
		{
			return null;
		}

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(new File(strFilePath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inJustDecodeBounds = true;
		bitmapOptions.inPurgeable = true;
		BitmapFactory.decodeStream(fis, null, bitmapOptions);
		//		int imageWidth = bitmapOptions.outWidth;
		//		int imageHeight = bitmapOptions.outHeight;

		bitmapOptions.inJustDecodeBounds = false;
		bitmapOptions.inSampleSize = nSampleSize;
		Bitmap thumbnailBitmap = BitmapFactory.decodeStream(fis, null, bitmapOptions);

		try
		{
			fis.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return thumbnailBitmap;
	}

	/** 
	 * 圖片旋轉
	 *  
	 * @param bmp 
	 *            
	 * @param degree 圖片旋轉的角度，負值為逆時針旋轉，正值為順時針旋轉 
	 *          
	 * @return 
	 */
	public static Bitmap rotateBitmap(Bitmap bmp, float degree)
	{
		Matrix matrix = new Matrix();
		matrix.postRotate(degree);
		return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
	}

	/** 
	 * 圖片縮放 
	 *  
	 * @param bm 
	 * @param scale 
	 *            值小於則為縮小，否則為放大 
	 * @return 
	 */
	public static Bitmap resizeBitmap(Bitmap bm, float scale)
	{
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
	}

	/** 
	 * 圖片縮放 
	 *  
	 * @param bm 
	 * @param w 
	 *            縮小或放大成的寬 
	 * @param h 
	 *            縮小或放大成的高 
	 * @return 
	 */
	public static Bitmap resizeBitmap(Bitmap bm, int w, int h)
	{
		Bitmap BitmapOrg = bm;

		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();

		float scaleWidth = ((float) w) / width;
		float scaleHeight = ((float) h) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		return Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
	}

	/** 
	 * 圖片反轉 
	 *  
	 * @param bm 
	 * @param flag 
	 *            0為水平反轉，1為垂直反轉 
	 * @return 
	 */
	public static Bitmap reverseBitmap(Bitmap bmp, int flag)
	{
		float[] floats = null;
		switch (flag)
		{
		case 0: // 水平反转  
			floats = new float[] { -1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f };
			break;
		case 1: // 垂直反转  
			floats = new float[] { 1f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 1f };
			break;
		}

		if (floats != null)
		{
			Matrix matrix = new Matrix();
			matrix.setValues(floats);
			return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
		}

		return null;
	}

	public static void releaseBitmap(Bitmap bitmap)
	{
		if (null != bitmap)
		{
			if (!bitmap.isRecycled())
			{
				bitmap.recycle();
			}
			bitmap = null;
		}
	}
}
