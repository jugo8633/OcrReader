package com.senao.ocrreader;

import java.io.FileNotFoundException;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity
{
	private final int	RESULT_CODE_OPEN_FILE	= 1;
	private Button		btnFile					= null;
	private Button		btnAnalysis				= null;
	private ImageView	imgSrc					= null;
	private TextView	txtResult				= null;
	private Bitmap		bitmap					= null;

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
			try
			{
				if (null != bitmap)
				{
					if (!bitmap.isRecycled())
					{
						bitmap.recycle();
					}
				}
				bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
				imgSrc.setImageBitmap(bitmap);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return;
			}

			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
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
		baseApi.init(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/", "eng");
		baseApi.setImage(bitmap);
		String text = baseApi.getUTF8Text();
		baseApi.clear();
		baseApi.end();
		baseApi = null;
		txtResult.setText(text);
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
