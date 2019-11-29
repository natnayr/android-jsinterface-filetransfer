package develop.com.javascript_interface;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Activity mActivity;

    public static final int CAMERA_REQUEST_CODE = 100;
    public static final int GALLERY_REQUEST_CODE = 200;
    public static final float MAX_IMAGE_SIZE = 512;

    private JSInterface mJSInterface;
    private WebView mWebview;
    private String mImageFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;

        mWebview = (WebView) this.findViewById(R.id.webView);
        mWebview.getSettings().setJavaScriptEnabled(true);

        mJSInterface = new JSInterface(this);
        mWebview.addJavascriptInterface(mJSInterface, "JSInterface");
        mWebview.loadUrl("file:///android_res/raw/index.html");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {

            switch (requestCode) {
                case CAMERA_REQUEST_CODE:

                    try {
                        File file = new File(mImageFilePath);
                        final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), Uri.fromFile(file));

                        String encodedImage = encodeImage(bitmap);

                        mWebview.loadUrl("javascript: takePhotoResponse('" + encodedImage + "')");

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(MainActivity.class.getSimpleName(), e.getLocalizedMessage(), e);
                    }
                    break;

                case GALLERY_REQUEST_CODE:

                    try {
                        Uri pickedImage = data.getData();
                        final Bitmap selectedImage = BitmapFactory.decodeStream(getContentResolver().openInputStream(pickedImage));

                        //resize to 500 x 500
                        String encodedImage = encodeImage(selectedImage);

                        mWebview.loadUrl("javascript: takePhotoResponse('" + encodedImage + "')");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(MainActivity.class.getSimpleName(), e.getLocalizedMessage(), e);
                    }
                    break;
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        Bitmap scaledDownBitmap = scaleDown(bitmap);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledDownBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);
        Log.d(MainActivity.class.getSimpleName(), "Base64: [[" + encImage + "]]");
        return encImage;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mImageFilePath = image.getAbsolutePath();
        return image;
    }

    public static Bitmap scaleDown(Bitmap realImage) {
        float ratio = Math.min(
                MAX_IMAGE_SIZE / realImage.getWidth(),
                MAX_IMAGE_SIZE / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, false);
        return newBitmap;
    }


    public class JSInterface {
        Context mContext;

        JSInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface   // must be added for API 17 or higher
        public void takePhoto() {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                return;
            }

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                //Create a file to store the image
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(MainActivity.class.getSimpleName(), "Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                            BuildConfig.APPLICATION_ID + ".fileprovider", photoFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                }

            }
        }

        @JavascriptInterface   // must be added for API 17 or higher
        public void openGallery() {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
            Intent galleryIntent = new Intent(Intent.ACTION_PICK); //Intent.ACTION_GET_CONTENT
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
        }
    }
}
