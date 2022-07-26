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
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import org.opencv.android.Utils;

public class MainActivity extends AppCompatActivity {

    Button getImage;
    ImageView result;
    int imageSize = 32;

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


                    Point SEShedCornerDst = new Point(0.0,0.0);
                    Point CloseForsythiaDst = new Point(800.0, 0.0);
                    Point CornerHazelDst = new Point(0.0,800.0);
                    Point FarForsythiaDst = new Point(800.0, 800.0);

                    Point SEShedCornerSrc = new Point(75.0,207.0);
                    Point CloseForsythiaSrc = new Point(663.0,180.0);
                    Point CornerHazelSrc = new Point(90.0, 798.0);
                    Point FarForsythiaSrc = new Point(690.0, 771.0);

                    Point [] srcArray = new Point[4];
                    srcArray[0] = SEShedCornerSrc;
                    srcArray[1] = CloseForsythiaSrc;
                    srcArray[2] = CornerHazelSrc;
                    srcArray[3] = FarForsythiaSrc;

                    Mat OutputMat = new Mat();
                    LinkedList<Point> dstArray = new LinkedList<Point>();

                    dstArray.add(SEShedCornerDst);
                    dstArray.add(CloseForsythiaDst);
                    dstArray.add(CornerHazelDst);
                    dstArray.add(FarForsythiaDst);

                    MatOfPoint2f dst = new MatOfPoint2f();
                    dst.fromList(dstArray);

                    MatOfPoint2f src = new MatOfPoint2f();
                    src.fromArray(srcArray);

                    Mat Homog;



                    // obtain your homography mat (picked your parameters.. you have to play to get the right results)
                    Mat homography = Calib3d.findHomography(src, dst, Calib3d.RANSAC, 3);
// image you want to transform

                    Mat mat = new Mat();
                    Mat outputMat = new Mat();
                    Bitmap bmp32 = selectedImage.copy(Bitmap.Config.ARGB_8888, true);
                    Utils.bitmapToMat(bmp32, mat);

// outputMat will contain the perspectively changed image
                    Imgproc.warpPerspective(mat, outputMat, homography, new Size(800, 800));
                    Bitmap bmp = null;
                    try {
                        //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
                        bmp = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(outputMat, bmp);
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
}