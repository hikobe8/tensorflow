package org.tensorflow.demo.image;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.demo.Classifier;
import org.tensorflow.demo.R;
import org.tensorflow.demo.TensorFlowObjectDetectionAPIModel;

import java.io.IOException;
import java.util.List;

public class RecognizeImageActivity extends Activity {

    private static final String TAG = RecognizeImageActivity.class.getSimpleName();
    private Classifier detector;

    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final int TF_OD_API_INPUT_SIZE = 300;

    private ImageView mImageView;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize_image);
        mImageView = (ImageView) findViewById(R.id.iv);
        mTextView = (TextView) findViewById(R.id.tv_result);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
            int cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            Log.e(TAG, "RecognizeImageActivity");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    public void choosePhoto(View v){
        Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
        // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型" 所有类型则写 "image/*"
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg");
        startActivityForResult(intentToPickPic, 1);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // 获取图片
            try {
                //该uri是上一个Activity返回的
                Uri imageUri = data.getData();
                if(imageUri!=null) {
                    Bitmap bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    Log.i("bit", String.valueOf(bit));
                    mImageView.setImageBitmap(bit);
                    mTextView.setText("Processing...");
                    new ProcessTask().execute(bit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ProcessTask extends AsyncTask<Bitmap, Void, List<Classifier.Recognition>> {

        @Override
        protected List<Classifier.Recognition> doInBackground(Bitmap... bitmaps) {
            Bitmap source = bitmaps[0];
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, 300, 300, false);
            List<Classifier.Recognition> recognitions = detector.recognizeImage(scaledBitmap);
            return recognitions;
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected void onPostExecute(List<Classifier.Recognition> list) {
            super.onPostExecute(list);
            StringBuilder stringBuilder = new StringBuilder();
            for (Classifier.Recognition recognition : list) {
                Log.e("test", recognition.getTitle() + " value = " + recognition.getConfidence());
                stringBuilder
                        .append("Name : ")
                        .append(recognition.getTitle())
                        .append(", Confidence : ")
                        .append(String.format("(%.1f%%) ", recognition.getConfidence() * 100.0f))
                        .append("\n");
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            mTextView.setText(stringBuilder.toString());
        }
    }

}
