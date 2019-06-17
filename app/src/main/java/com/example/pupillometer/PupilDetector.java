package com.example.pupillometer;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PupilDetector {

    // Tag string for Logs
    private final String _TAG = "PupilDetector";


    // pupil sizes and differences
    private double mPupilSizeLeft, mPupilSizeRight, mPupilDiff;

    // reference value (26.5px = 1mm)
    private static float mMillimeter = 26.5f;

    // algorithm paramters for each eye color
    private static int[] blueGreenGray = {35,52,10,255,255,52,2,2,2,2};
    private static int[] lightBrown = {36,0,0,255,255,65,5,5,7,7};
    private static int[] darkBrown = {36,112,0,255,255,47,12,12,18,18};
    private static int[] brown = {20,70,0,255,255,60,3,3,6,6};

    // path of processed image file
    private String processedImgPath;

    // gobal bitmap
    private Bitmap mImgBitmap;
    // global image mat
    private Mat mImgMat;
    // eye cascade classifier
    private CascadeClassifier mEyeClassifier;

    // application context
    private Context context;

    // loader callback for openCV initialization
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(_TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /**
     * Constructor of PupilDetector class
     * @param context
     */
    public PupilDetector(Context context){
        this.context = context;
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, context, loaderCallback);
    }


    /**
     * Performs pupil measurement based on eye color and saves processed image
     * @return Measurement
     */
    public Measurement doMeasurement(String filepath, int eyeColor) {

        // load cascade file from application resources
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        mEyeClassifier = loadClassifier(R.raw.haarcascade_eye, "haarcascade_eyes.xml", cascadeDir);
        cascadeDir.delete();

        mImgMat = transformImgToMat(filepath);

        if (mImgMat != null) {

            int[] algoParams = {};
            if(eyeColor == 0) algoParams = blueGreenGray;
            if(eyeColor == 1) algoParams = brown;
            if(eyeColor == 2) algoParams = darkBrown;
            if(eyeColor == 3) algoParams = lightBrown;

            mImgBitmap = detectPupils(mImgMat, mEyeClassifier, algoParams);
            saveImage(mImgBitmap);

            int id = (int) System.currentTimeMillis();
            Date date = new Date();

            return new Measurement(id, mPupilSizeLeft, mPupilSizeRight, mPupilDiff, date.toString(), processedImgPath);
        } else {
            return null;
        }
    }

    /**
     * Transforms image from image path to a Mat object
     * @param imgPath
     * @return
     */
    private Mat transformImgToMat(String imgPath) {
        //To speed up loading of image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1; // 1: returns original image size

        // we create a new Bitmap object temp to store the image
        Bitmap temp = BitmapFactory.decodeFile(imgPath, options);

        //Get orientation information
        int orientation = 0;
        try {
            // we first extract the orientation information from the image using the ExifInterface class
            ExifInterface imgParams = new ExifInterface(imgPath);
            orientation = imgParams.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Rotating the image to get the correct orientation
        Matrix rotate90 = new Matrix();
        rotate90.postRotate(orientation);

        Bitmap originalBitmap = rotateBitmap(temp, orientation);

        if (originalBitmap != null) {
            Toast.makeText(context, "Image added.", Toast.LENGTH_SHORT).show();

            //Convert Bitmap to Mat
            Mat tmpMat;
            mImgBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            tmpMat = new Mat(mImgBitmap.getHeight(), mImgBitmap.getWidth(), CvType.CV_8U);
            Utils.bitmapToMat(mImgBitmap, tmpMat);

            return tmpMat;
        } else {
            Log.i(_TAG, "mImgBitmap is empty");
            return null;
        }
    }

    /**
     * Rotates bitmap based on it's orientation
     * @param bitmap
     * @param orientation
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Loads cascade classifier from a XML-File
     * @param rawResId
     * @param filename
     * @param cascadeDir
     * @return
     */
    private CascadeClassifier loadClassifier(int rawResId, String filename, File cascadeDir) {
        CascadeClassifier classifier = null;
        try {
            InputStream is = context.getResources().openRawResource(rawResId);
            File cascadeFile = new File(cascadeDir, filename);
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (classifier.empty()) {
                Log.e(_TAG, "Failed to load cascade classifier");
                classifier = null;
            } else
                Log.i(_TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(_TAG, "Failed to load cascade. Exception thrown: " + e);
        }

        return classifier;
    }

    /**
     * Detects eyes and pupils using Cascade Classification, Canny, Erosion, Dilation, Guassian Blur and Hough Circle Transformation
     * @param imgMat
     * @param classifier
     * @param params
     * @return
     */
    private Bitmap detectPupils(Mat imgMat, CascadeClassifier classifier, int[] params) {
        Bitmap tmpBitmap = mImgBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Mat mGray = new Mat();

        // Convert image to gray scale
        Imgproc.cvtColor(imgMat, mGray, Imgproc.COLOR_RGB2GRAY);

        Rect area = new Rect(new Point(20, 20), new Point(mGray.width() - 20, mGray.height() - 20));

        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();

        // DETECT EYES
        classifier.detectMultiScale(mROI, eyes, 1.04, 6, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                | Objdetect.CASCADE_SCALE_IMAGE, new Size(700, 700), new Size());

        // Eyes coordinates & draw rectangle on coordinates
        Rect[] eyesArray = eyes.toArray();
        for (int eye = 0; eye < eyesArray.length; eye++) {
            Rect e = eyesArray[eye];

            boolean isLeftEye = false;
            boolean isRightEye = false;

            int imgMiddle = imgMat.width()/2;
            if(e.tl().x > imgMiddle) isLeftEye = true;
            else isRightEye = true;

            Rect eye_only_rectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.3), (int) (e.width + e.width * 0.1), (int) (e.height * 0.6));

            Imgproc.rectangle(imgMat, eye_only_rectangle.tl(), eye_only_rectangle.br(), new Scalar(255, 255, 0, 255), 12);

            String eye_txt = "";
            if(isLeftEye) eye_txt = "L";
            if(isRightEye) eye_txt = "R";
            Imgproc.putText(imgMat, eye_txt,
                    new Point((int)(eye_only_rectangle.tl().x + ((e.width + e.width * 0.1)/2) - 50), (int) (eye_only_rectangle.tl().y - 30)),
                    Core.FONT_ITALIC, 5.5, new Scalar(255, 255, 0, 255), 13);

            // Detect pupils in the defined area
            Mat img = imgMat.submat(eye_only_rectangle);
            Mat img_hsv = new Mat();

            Mat circles = new Mat();

            Imgproc.cvtColor(img, img_hsv, Imgproc.COLOR_RGB2HSV);

            Core.inRange(img_hsv, new Scalar(params[0], params[1], params[2]),
                    new Scalar(params[3], params[4], params[5]), img_hsv);

            Imgproc.erode(img_hsv, img_hsv, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(params[6], params[7])));

            Imgproc.dilate(img_hsv, img_hsv, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(params[8], params[9])));

            Imgproc.Canny(img_hsv, img_hsv, 0, 255);

            Imgproc.GaussianBlur(img_hsv, img_hsv, new Size(9, 9), 2, 2);

            Imgproc.HoughCircles(img_hsv, circles, Imgproc.CV_HOUGH_GRADIENT, 1,
                    img_hsv.rows(), 180, 10, 10, 110);

            double tmpPupilRadius = 0;
            if (circles.cols() > 0) {
                for (int pupil = 0; pupil < circles.cols(); pupil++) {
                    double pupilParams[] = circles.get(0, pupil);

                    if (pupilParams == null)
                        break;

                    Point pupilPos = new Point(Math.round(pupilParams[0]), Math.round(pupilParams[1]));
                    int radius = (int) Math.round(pupilParams[2]);

                    // draw circle around pupil
                    Imgproc.circle(img, pupilPos, radius, new Scalar(0, 255, 0), 3);

                    // draw point at pupilc center
                    Imgproc.circle(img, pupilPos, 1, new Scalar(255, 255, 255), 9);

                    // stores the radius of the first circle
                    if (pupil == 0) tmpPupilRadius = pupilParams[2];
                }
            } else {
                Log.i(_TAG, "No pupils detected...");
            }

            if (isLeftEye)
                mPupilSizeLeft = tmpPupilRadius * 2 / mMillimeter;
            if (isRightEye)
                mPupilSizeRight = tmpPupilRadius * 2 / mMillimeter;

        }
        mPupilDiff = mPupilSizeLeft - mPupilSizeRight;

        Utils.matToBitmap(imgMat, tmpBitmap);
        return tmpBitmap;
    }

    /**
     * Saves bitmap as PNG-File
     *
     * @param bitmap
     */
    private void saveImage(Bitmap bitmap) {
        File imageFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // create own folder for app
        File imgFolder = new File(imageFolder, "Pupilometer/PROCESSED");

        // check if folder exists
        if (!imgFolder.exists()) {
            imgFolder.mkdirs();
        }

        Long tsLong = System.currentTimeMillis();
        String timestamp = Long.toString(tsLong);
        String filename = timestamp.substring(0,4) + timestamp.substring(timestamp.length() - 4);

        File imageFile = new File(imgFolder, "IMG_" + filename + ".jpg");
        if (imageFile.exists()) imageFile.delete();
        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            processedImgPath = imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
