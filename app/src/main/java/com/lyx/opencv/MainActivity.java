package com.lyx.opencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    /*static {
        System.loadLibrary("native-lib");
    }*/

    private static final int MEAN_BLUR = 1;

    private final int SELECT_PHOTO = 1;
    private ImageView ivImage, ivImageProcessed;
    Mat src;
    static int ACTION_MODE = 1;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    // do your job
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        ivImage = findViewById(R.id.iv_image);
        ivImageProcessed = findViewById(R.id.iv_imageProcessed);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {// 默认加载opencv_java.so库 
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    public native String stringFromJNI();
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_load_image:
                pickPhoto();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        src = new Mat(selectedImage.getHeight(),
                                selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src);

                        switch (ACTION_MODE) {
                            case MEAN_BLUR:
                                handleMeanBlur(src);
                                setIvImageProcessed(src, selectedImage);
                                break;
                        }

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void pickPhoto() {
        Intent picPickerIntent = new Intent(Intent.ACTION_PICK);
        picPickerIntent.setType("image/*");
        startActivityForResult(picPickerIntent, SELECT_PHOTO);
    }

    private void handleMeanBlur(Mat src) {
        Imgproc.blur(src, src, new Size(3, 3));
    }

    private void handleGaussianBlur(Mat src) {
        Imgproc.GaussianBlur(src, src, new Size(3, 3), 0);
    }

    private void handleMedianBlur(Mat src) {
        Imgproc.medianBlur(src, src, 3);
    }

    private void setIvImageProcessed(Mat src, Bitmap selectedImage) {
        Bitmap processedImage = Bitmap.createBitmap(
                src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, processedImage);
        ivImage.setImageBitmap(selectedImage);
        ivImageProcessed.setImageBitmap(processedImage);
    }
}
