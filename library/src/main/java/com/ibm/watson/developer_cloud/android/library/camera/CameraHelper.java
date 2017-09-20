/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.developer_cloud.android.library.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

/**
 * The Class CameraHelper.
 */
public final class CameraHelper {

  /**
   * The Constant REQUEST_IMAGE_CAPTURE.
   */
  public static final int REQUEST_IMAGE_CAPTURE = 1000;
  /**
   * The Constant REQUEST_PERMISSION.
   */
  public static final int REQUEST_PERMISSION = 3000;
  private final String TAG = CameraHelper.class.getName();
  private Activity activity;
  private String currentPhotoPath;

  /**
   * Provides convenience access to device camera.
   *
   * @param activity The current activity
   */
  public CameraHelper(Activity activity) {
    this.activity = activity;
  }

  /**
   * Starts an activity using the device's onboard camera app.
   */
  public void dispatchTakePictureIntent() {
    if (checkPermissions()) {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      // Ensure that there's a camera activity to handle the intent
      if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
        // Create the File where the photo should go
        File photoFile = null;
        try {
          photoFile = createImageFile();
        } catch (IOException ex) {
          Log.e(TAG, "IOException", ex);
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
          Uri photoURI;
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            photoURI =
                    FileProvider.getUriForFile(activity, "com.ibm.watson.developer_cloud.android.provider", photoFile);
          } else {
            photoURI = Uri.fromFile(photoFile);
          }
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
          activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
      }
    }
  }

  private boolean checkPermissions() {
    String permissions[] = {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    boolean grantCamera =
            ContextCompat.checkSelfPermission(activity, permissions[0]) == PackageManager.PERMISSION_GRANTED;
    boolean grantExternal =
            ContextCompat.checkSelfPermission(activity, permissions[1]) == PackageManager.PERMISSION_GRANTED;

    if (!grantCamera && !grantExternal) {
      ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSION);
    } else if (!grantCamera) {
      ActivityCompat.requestPermissions(activity, new String[]{permissions[0]}, REQUEST_PERMISSION);
    } else if (!grantExternal) {
      ActivityCompat.requestPermissions(activity, new String[]{permissions[1]}, REQUEST_PERMISSION);
    }

    return grantCamera && grantExternal;
  }

  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    currentPhotoPath = "file:" + image.getAbsolutePath();
    return image;
  }

  /**
   * This method returns the file of the photo that was taken. It should be called in onActivityResult.
   *
   * @param resultCode Result Code of the previous activity
   * @return If successful, the image file. Null otherwise.
   */
  public File getFile(int resultCode) {
    Log.d("Result code test", Integer.toString(resultCode));
    if (resultCode == RESULT_OK) {
      Uri targetUri = Uri.parse(currentPhotoPath);
      return new File(targetUri.getPath());
    }
    Log.e(TAG, "Result Code was not OK");
    return null;
  }

  /**
   * This method returns a bitmap of the photo that was taken. It should be called in onActivityResult. Because the
   * CameraHelper knows the path it's unnecessary to get it from the returned data.
   *
   * @param resultCode Result Code of the previous activity
   * @return If successful, a bitmap of the image. Null otherwise.
   */
  public Bitmap getBitmap(int resultCode) {
    if (resultCode == RESULT_OK) {
      Uri targetUri = Uri.parse(currentPhotoPath);
      try {
        return BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(targetUri));
      } catch (FileNotFoundException e) {
        Log.e(TAG, "File Not Found", e);
        return null;
      }
    }
    Log.e(TAG, "Result Code was not OK");
    return null;
  }
}
