package com.senao.ocrreader;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private final int		RESULT_CODE_OPEN_FILE	= 1;
	private Button			btnFile					= null;
	private Button			btnAnalysis				= null;
	private ImageView		imgSrc					= null;
	private TextView		txtResult				= null;
	private Bitmap			bitmap					= null;
	private final String	LANGUAGE				= "eng";	//English:eng , 繁體:chi_tra , 簡體:chi_sim 

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnFile = (Button) this.findViewById(R.id.buttonFilePath);
		btnAnalysis = (Button) this.findViewById(R.id.buttonAnalysis);
		imgSrc = (ImageView) this.findViewById(R.id.imageViewOCR);
		txtResult = (TextView) this.findViewById(R.id.textViewResult);

		btnFile.setOnClickListener(buttonClickListener);
		btnAnalysis.setOnClickListener(buttonClickListener);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK && RESULT_CODE_OPEN_FILE == requestCode)
		{
			txtResult.setText(null);
			Uri uri = data.getData();
			ContentResolver contentResolver = this.getContentResolver();
			if (null != bitmap)
			{
				if (!bitmap.isRecycled())
				{
					bitmap.recycle();
				}
			}

			try
			{
				//取得圖片Bitmap
				bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri);
				//				bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			bitmap = convertToBlackWhite(bitmap);
			bitmap = colorFilter(bitmap);
			imgSrc.setImageBitmap(bitmap);
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void openFile()
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, RESULT_CODE_OPEN_FILE);
	}

	private void analysis()
	{
		if (null == bitmap)
		{
			return;
		}

		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.init(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/", LANGUAGE);
		baseApi.setVariable("tessedit_char_whitelist", "0123456789");
		baseApi.setImage(bitmap);
		String text = baseApi.getUTF8Text();
		baseApi.clear();
		baseApi.end();
		baseApi = null;
		txtResult.setText(text);
	}

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
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

	public static Bitmap convertToBlackWhite(Bitmap bmp)
	{
		int width = bmp.getWidth(); // 获取位图的宽
		int height = bmp.getHeight(); // 获取位图的高
		int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		int alpha = 0xFF << 24;
		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
			{
				int grey = pixels[width * i + j];
				int red = ((grey & 0x00FF0000) >> 16);
				int green = ((grey & 0x0000FF00) >> 8);
				int blue = (grey & 0x000000FF);
				grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
				grey = alpha | (grey << 16) | (grey << 8) | grey;
				pixels[width * i + j] = grey;
			}
		}
		Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
		return newBmp;
	}

	public static Bitmap toGrayscale(Bitmap bmpOriginal)
	{
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();

		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	private Bitmap colorFilter(Bitmap sourceBitmap)
	{
		int brightness = 80;

		float[] colorTransform = { // color matrix
		0.33f, 0.33f, 0.33f, 0, brightness, //red
				0.33f, 0.33f, 0.33f, 0, brightness, //green
				0.33f, 0.33f, 0.33f, 0, brightness, //blue
				0, 0, 0, 1, 0 //alpha    
		};

		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0f); //Remove Colour 
		colorMatrix.set(colorTransform);

		ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
		Paint paint = new Paint();
		paint.setColorFilter(colorFilter);

		Display display = getWindowManager().getDefaultDisplay();

		Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap);

		imgSrc.setImageBitmap(resultBitmap);

		Canvas canvas = new Canvas(resultBitmap);
		canvas.drawBitmap(resultBitmap, 0, 0, paint);

		return resultBitmap;
	}

	private OnClickListener	buttonClickListener	= new OnClickListener()
												{

													@Override
													public void onClick(View v)
													{
														int nId = v.getId();
														switch (nId)
														{
														case R.id.buttonFilePath:
															Logs.showTrace("open file");
															openFile();
															break;
														case R.id.buttonAnalysis:
															Logs.showTrace("ocr analysis");
															analysis();
															break;
														}
													}
												};
}
