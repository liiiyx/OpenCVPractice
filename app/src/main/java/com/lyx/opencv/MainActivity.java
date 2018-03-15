package com.lyx.opencv;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    /*static {
        System.loadLibrary("native-lib");
    }*/

    private static final int MEAN_BLUR = 1;
    private static final int CUSTOM_SHARPEN = 2;
    private static final int DILATE = 3;
    private static final int ERODE = 4;
    private static final int DOG_DIFFERENCE = 5;
    private static final int HARRIS_CORNER = 6;
    private static final int HOUGH_LINES = 7;

    private final int SELECT_PHOTO = 1;
    private ImageView ivImage, ivImageProcessed;
    private Mat mOriginMat;
    private Bitmap mOriginBitmap;

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
        // 默认加载opencv_java.so库 
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_load_image:
                ACTION_MODE = MEAN_BLUR;
                break;

            case R.id.action_custom_sharpen:
                ACTION_MODE = CUSTOM_SHARPEN;
                break;

            case R.id.action_dilate:
                ACTION_MODE = DILATE;
                break;

            case R.id.action_erode:
                ACTION_MODE = ERODE;
                break;

            case R.id.action_dog:
                ACTION_MODE = DOG_DIFFERENCE;
                break;

            case R.id.action_harris_corner:
                ACTION_MODE = HARRIS_CORNER;
                break;

            case R.id.action_hough_lines:
                ACTION_MODE = HOUGH_LINES;
                break;

            default:
                break;
        }

        pickPhoto();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        if (data == null) return;

                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        mOriginBitmap = BitmapFactory.decodeStream(imageStream);
                        mOriginMat = new Mat(mOriginBitmap.getHeight(),
                                mOriginBitmap.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(mOriginBitmap, mOriginMat);

                        handleOpenCvAction();
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

    private void handleOpenCvAction() {

        switch (ACTION_MODE) {
            case MEAN_BLUR:
                handleMeanBlur(mOriginMat);
                setIvImageProcessed(mOriginMat, mOriginBitmap);
                break;
            case CUSTOM_SHARPEN:
                handleCustomSharpen(mOriginMat);
                setIvImageProcessed(mOriginMat, mOriginBitmap);
                break;
            case DILATE:
                handleDilate(mOriginMat);
                setIvImageProcessed(mOriginMat, mOriginBitmap);
                break;
            case ERODE:
                handleErode(mOriginMat);
                break;
            case DOG_DIFFERENCE:
                handleDifferenceOfGaussian();
                break;
            case HARRIS_CORNER:
                handleHarrisCorner();
                break;
            case HOUGH_LINES:
//                handleHoughLines();
                handleContours();
                break;
        }
    }

    private void handleMeanBlur(Mat src) {
        Imgproc.blur(src, src,
                new Size(3, 3));
    }

    private void handleCustomSharpen(Mat src) {
        Mat kernel = new Mat(3, 3, CvType.CV_16SC1);
        kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
        Imgproc.filter2D(src, src, src.depth(), kernel);
    }

    private void handleDilate(Mat src) {
        Mat kernelDilate = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(src, src, kernelDilate);
    }

    private void handleErode(Mat src) {
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(5, 5));
        Imgproc.erode(src, src, kernelErode);
    }

    private void handleDifferenceOfGaussian() {

        Mat grayMat = new Mat();
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();

        // 将图像转换成灰色图像
        Imgproc.cvtColor(mOriginMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // 使用两个不同的模糊半径对图像进行模糊处理
        Imgproc.GaussianBlur(mOriginMat, blur1, new Size(15, 15), 5);
        Imgproc.GaussianBlur(mOriginMat, blur2, new Size(25, 25), 5);

        Mat dog = new Mat();
        Core.absdiff(blur1, blur2, dog);

        // 反转二值阈值化, 将边缘点的值设为255
        Core.multiply(dog, new Scalar(100), dog);
        Imgproc.threshold(dog, dog, 50, 255, Imgproc.THRESH_BINARY_INV);

        setIvImageProcessed(dog, mOriginBitmap);
    }

    private void handleHarrisCorner() {

        Mat grayMat = new Mat();
        Mat corners = new Mat();

        Imgproc.cvtColor(mOriginMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // 找出角点
        Mat tempDst = new Mat();
        Imgproc.cornerHarris(grayMat, tempDst, 2, 3, 0.04);

        Mat tempDstNorm = new Mat();
        Core.normalize(tempDst, tempDstNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm, corners);

        Random random = new Random();
        for (int i = 0; i < tempDstNorm.cols(); i++) {
            for (int j = 0; j < tempDstNorm.rows(); j++) {
                double[] value = tempDstNorm.get(j, i);
                if (value[0] > 150) {
                    Imgproc.circle(corners, new Point(i, j), 5,
                            new Scalar(random.nextInt(255)), 2);
                }
            }
        }

        setIvImageProcessed(corners, mOriginBitmap);
    }

    private void handleHoughLines() {

        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat lines = new Mat();

        Imgproc.cvtColor(mOriginMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        // 使用Canny算子检测边缘
        Imgproc.Canny(grayMat, cannyEdges, 10, 100);

        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180,
                50, 20, 20);
        Mat houghLines = new Mat();
        houghLines.create(cannyEdges.size(), CvType.CV_8UC1);

        for (int i = 0; i < lines.cols(); i++) {
            double[] points = lines.get(0, i);
            double x1, y1, x2, y2;

            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point p1 = new Point(x1, y1);
            Point p2 = new Point(x2, y2);

            Imgproc.line(houghLines, p1, p2,
                    new Scalar(255, 0, 0)/*red color*/, 1);
        }

        setIvImageProcessed(houghLines, mOriginBitmap);
    }

    private void handleContours() {

        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();

        List<MatOfPoint> contoursList = new ArrayList<>();

        Imgproc.cvtColor(mOriginMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(grayMat, cannyEdges, 10, 100);

        Imgproc.findContours(cannyEdges, contoursList, hierarchy,
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat contours = new Mat();
        contours.create(cannyEdges.size(), CvType.CV_8UC1);
        Random r = new Random();
        for (int i = 0; i < contoursList.size(); i++) {
            Imgproc.drawContours(contours, contoursList, i,
                    new Scalar(r.nextInt(255), r.nextInt(255),
                            r.nextInt(255)), -1);
        }

        setIvImageProcessed(contours, mOriginBitmap);
    }

    private void setIvImageProcessed(Mat processedMat, Bitmap selectedImage) {

        Bitmap processedImage = Bitmap.createBitmap(
                processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(processedMat, processedImage);
        ivImage.setImageBitmap(selectedImage);
        ivImageProcessed.setImageBitmap(processedImage);
    }
}
