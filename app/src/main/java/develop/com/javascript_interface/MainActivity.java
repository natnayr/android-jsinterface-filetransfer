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
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private Activity mActivity;

    public static final int CAMERA_REQUEST_CODE = 1;
    public static final int GALLERY_REQUEST_CODE = 2;

    private JSInterface mJSInterface;
    private WebView mWebview;

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

        Bitmap bitmap = null;

        if (resultCode == Activity.RESULT_OK && data != null) {

            switch (requestCode) {
                case CAMERA_REQUEST_CODE:

                    try {
                        final Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        String encodedImage = encodeImage(selectedImage);

                        mWebview.loadUrl("javascript: takePhotoResponse('"+encodedImage+"')");

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
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(selectedImage, 250, 250, false);
                        String encodedImage = encodeImage(resizedBitmap);

                        mWebview.loadUrl("javascript: takePhotoResponse('"+encodedImage+"')");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(MainActivity.class.getSimpleName(), e.getLocalizedMessage(), e);
                    }
                    break;
            }
        }
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
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

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
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
