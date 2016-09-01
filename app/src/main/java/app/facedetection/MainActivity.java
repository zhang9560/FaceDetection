package app.facedetection;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    static {
        OpenCVLoader.init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        mFaceDetector = initDetector("haarcascade_frontalface_alt.xml");
        mEyeDetector = initDetector("haarcascade_eye.xml");
    }

    private void initViews() {
        mImageView = (ImageView)findViewById(R.id.image);

        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open("test.jpg"));
            mImageView.setImageBitmap(mBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CascadeClassifier initDetector(String cascade) {
        try {
            File fileDir = getFilesDir();
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            File cascadeFile = new File(fileDir.getAbsoluteFile() + File.separator + cascade);
            if (!cascadeFile.exists()) {
                InputStream is = getAssets().open(cascade);
                FileOutputStream os = new FileOutputStream(cascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                os.close();
                is.close();
            }

            return new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void detect(View view) {
        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Void... voids) {
                ByteBuffer buf = ByteBuffer.allocate(mBitmap.getWidth() * mBitmap.getHeight() * 4);
                mBitmap.copyPixelsToBuffer(buf);

                Mat rgbMat = new Mat(mBitmap.getHeight(), mBitmap.getWidth(), CvType.CV_8UC4);
                rgbMat.put(0, 0, buf.array());
                Mat greyMat = new Mat(mBitmap.getHeight(), mBitmap.getWidth(), CvType.CV_8UC1);
                Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_BGRA2GRAY);
                MatOfRect faces = new MatOfRect();
                mFaceDetector.detectMultiScale(greyMat, faces);

                Bitmap mutableBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(mutableBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);

                Rect[] facesArray = faces.toArray();
                for (int i = 0; i < facesArray.length; i++) {
                    Log.i(TAG, facesArray[i].toString());
                    canvas.drawRect((float)facesArray[i].tl().x, (float)facesArray[i].tl().y,
                            (float)facesArray[i].br().x, (float)facesArray[i].br().y, paint);

                    MatOfRect eyes = new MatOfRect();
                    Mat eyeMat = greyMat.submat(facesArray[i]);
                    mEyeDetector.detectMultiScale(eyeMat, eyes);

                    Rect[] eyesArray = eyes.toArray();
                    for (int j = 0; j < eyesArray.length; j++) {
                        Log.i(TAG, eyesArray[j].toString());
                        canvas.drawRect((float)(facesArray[i].tl().x + eyesArray[j].tl().x),
                                (float)(facesArray[i].tl().y + eyesArray[j].tl().y),
                                (float)(facesArray[i].tl().x + eyesArray[j].br().x),
                                (float)(facesArray[i].tl().y + eyesArray[j].br().y),
                                paint);
                    }

                    eyeMat.release();
                }

                rgbMat.release();
                greyMat.release();
                return mutableBitmap;
            }

            @Override
            protected void onPreExecute() {
                mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setMessage("正在检测");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                mImageView.setImageBitmap(result);
                mProgressDialog.dismiss();
            }

            private ProgressDialog mProgressDialog;
        }.execute();
    }

    public void clear(View view) {
        mImageView.setImageBitmap(mBitmap);
    }

    private CascadeClassifier mFaceDetector;
    private CascadeClassifier mEyeDetector;
    private Bitmap mBitmap;
    private ImageView mImageView;
}
