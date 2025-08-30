// CameraActivity.java
package com.example.mypoject1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.util.Date;
import java.util.Locale;
import android.icu.text.SimpleDateFormat;

public class CameraActivity extends Activity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    private static final int CAMERA_REQUEST_CODE = 200;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // generate unique filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName  = "IMG_" + timestamp + ".jpg";

        // prepare ContentValues
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyApp");
        }

        // insert → get Uri
        imageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri == null) {
            Toast.makeText(this, "Cannot create image file", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // fire camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == CAMERA_REQUEST_CODE && res == RESULT_OK) {
            // scan so it shows up in the device gallery apps immediately
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{ imageUri.getPath() },
                    new String[]{ "image/jpeg" },
                    null
            );

            // return the new Uri
            Intent result = new Intent();
            result.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
            setResult(RESULT_OK, result);

        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
