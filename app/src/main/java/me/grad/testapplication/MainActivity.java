package me.grad.testapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.utils.Converters;

public class MainActivity extends AppCompatActivity {

    Button getImage;
    ImageView result;
    int imageSize = 32;
    Bitmap bmp32;
    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getImage = findViewById(R.id.get_img);
        result = findViewById(R.id.result);

        getImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);


                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                result.setImageBitmap(image);

            }else{
                try {
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                    Mat mat = new Mat();
                    bmp32 = selectedImage.copy(Bitmap.Config.ARGB_8888, true);

                    Utils.bitmapToMat(selectedImage, mat);
                    Mat outputMat2 = imageProc(mat);

                    Bitmap bmp = null;
                    try {
                        bmp = Bitmap.createBitmap(outputMat2.cols(), outputMat2.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(outputMat2, bmp);
                    }
                    catch (CvException e){
                        Log.d("Exception",e.getMessage());}

                    result.setImageBitmap(bmp);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Mat imageProc (Mat imgSource){
        Mat sourceImage = imgSource;

        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);

        //convert the image to black and white does (8 bit)
        Imgproc.Canny(imgSource, imgSource, 50, 50);

        //apply gaussian blur to smoothen lines of dots
        Imgproc.GaussianBlur(imgSource, imgSource, new  org.opencv.core.Size(5, 5), 5);

        //find the contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        int maxAreaIdx = -1;
        Log.d("size",Integer.toString(contours.size()));
        MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint largest_contour = contours.get(0);
        //largest_contour.ge
        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
        //Imgproc.drawContours(imgSource,contours, -1, new Scalar(0, 255, 0), 1);

            for (int idx = 0; idx < contours.size(); idx++) {
                temp_contour = contours.get(idx);
                double contourarea = Imgproc.contourArea(temp_contour);
                //compare this contour to the previous largest contour found
                if (contourarea > maxArea) {
                    //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
                int contourSize = (int)temp_contour.total();
                MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize*0.05, true);
                if (approxCurve_temp.total() == 4) {
                    maxArea = contourarea;
                    maxAreaIdx = idx;
                    approxCurve=approxCurve_temp;
                    largest_contour = temp_contour;
                }
            }
        }

        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);

        double[] temp_double;
        temp_double = approxCurve.get(0,0);
        Point p1 = new Point(temp_double[0], temp_double[1]);

        temp_double = approxCurve.get(1,0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        temp_double = approxCurve.get(2,0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        temp_double = approxCurve.get(3,0);
        Point p4 = new Point(temp_double[0], temp_double[1]);
        List<Point> source = new ArrayList<Point>();
        source.add(p1);
        source.add(p2);
        source.add(p3);
        source.add(p4);
        Mat startM = Converters.vector_Point2f_to_Mat(source);
        Mat result=warp(sourceImage,startM);
        return result;
    }

    public Mat warp(Mat inputMat,Mat startM) {
        int resultWidth = 1000;
        int resultHeight = 1000;

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);



        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(0, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, 0);
        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);


        Mat mat = new Mat();
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.warpPerspective(mat,
                outputMat,
                perspectiveTransform,
                new Size(resultWidth, resultHeight),
                Imgproc.INTER_CUBIC);

        return outputMat;
    }
}