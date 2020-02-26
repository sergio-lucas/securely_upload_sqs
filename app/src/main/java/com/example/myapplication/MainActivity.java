package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.task.AsyncTaskResult;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 436;

    private boolean mIsPermissionGranted = true;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.choose_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("video/*");
                if (mIsPermissionGranted) {
                    startActivityForResult(galleryIntent, REQUEST_CODE);
                } else {
                    Toast.makeText(MainActivity.this, "WRITE_EXTERNAL_STORAGE is not granted.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            String imagePath = getPath(selectedImageUri);
            new UploadTask().execute(imagePath);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (isGranted) {
            Log.i(TAG, "Permission: " + permissions[0] + " was " + grantResults[0]);
        } else {
            Log.w(TAG, "User didn't grant WRITE_EXTERNAL_STORAGE permission.");
            mIsPermissionGranted = false;
        }
    }

    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(projection[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private class UploadTask extends AsyncTask<String, Void, AsyncTaskResult<Void>> {
        private static final int NETWORK_TIMEOUT_SEC = 60;
        private static final String SERVER_URL = "upload url";

        @Override
        protected AsyncTaskResult doInBackground(String... params) {
            try {
                // Get pre-signed Amazon url to upload file.
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                        .connectTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .readTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .writeTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .build();

                // Upload file to Amazon.
                String imagePath = params[0];
                Request uploadFileRequest = new Request.Builder()
                        .url(SERVER_URL)
                        .put(RequestBody.create(new File(imagePath), MediaType.parse("video/mp4"))) // !read from uploaded file
                        .build();
                Response uploadResponse = client.newCall(uploadFileRequest).execute();
                if (!uploadResponse.isSuccessful())
                    return new AsyncTaskResult(new Exception("Upload file response code: " + uploadResponse.code()));

                return new AsyncTaskResult(null);

            } catch (Exception e) {
                return new AsyncTaskResult(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult result) {
            super.onPostExecute(result);
            if (result.hasError()) {
                Log.e(TAG, "UploadTask failed", result.getError());
                Toast.makeText(MainActivity.this, "UploadTask finished with error", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "UploadTask finished successfully", Toast.LENGTH_LONG).show();
            }
        }
    }
}
