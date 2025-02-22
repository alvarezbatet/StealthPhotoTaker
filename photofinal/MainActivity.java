package com.example.photofinal;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;

import android.hardware.camera2.*;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.OutputConfiguration;

import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;

import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    String TAG  = "MMMMMMMMMMMMM";

    private int count = 0;

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened: ");
            cameraDevice = camera;
            try {
                createCaptureSession();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
        }
    };
    private final CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: ");
            CaptureRequest.Builder captureRequestBuilder = null;
            try {
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            captureRequestBuilder.addTarget(imageReader.getSurface()); // Add the image reader surface
            try {
                session.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        Image image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        buffer.rewind();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        image.close();

                        // Save the image bytes as a JPEG file
                        try {
                            File galleryDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            String fileName = "take-photo";
                            String extension = ".jpg"; // Customize the file name as needed
                            fileName += Integer.toString(count);
                            count += 1;
                            File imageFile = new File(galleryDir, fileName + extension);
                            FileOutputStream fos = new FileOutputStream(imageFile);
                            fos.write(bytes);
                            fos.close();
                            buffer.clear();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, null);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed: ");
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        Button myButton = findViewById(R.id.button);
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
        Button myButton2 = findViewById(R.id.button2);
        myButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteImages();
            }
        });
    }

    private void deleteImages() {
        String galleryDir = "/storage/emulated/0/Pictures/";
        String extension = ".jpg"; // Customize the file name as needed
        int i = 0;
        while(i < 100) {
            String fileName = "take-photo";
            fileName += Integer.toString(i);
            File imageFile = new File(galleryDir, fileName + extension);
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    Log.d(TAG, "deleted: " + fileName);

                }
            }
            i += 1;
        }
    }

    @NonNull
    public Size getResolution(@NonNull final CameraManager cameraManager, @NonNull final String cameraId) throws CameraAccessException
    {
        final CameraCharacteristics  characteristics = cameraManager.getCameraCharacteristics(cameraId);
        final StreamConfigurationMap map             = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null)
        {
            throw new IllegalStateException("Failed to get configuration map.");
        }

        final Size[] choices = map.getOutputSizes(ImageFormat.JPEG);

        Arrays.sort(choices, Collections.reverseOrder(new Comparator<Size>()
        {
            @Override
            public int compare(@NonNull final Size lhs, @NonNull final Size rhs)
            {
                // Cast to ensure the multiplications won't overflow
                return Long.signum((lhs.getWidth() * (long)lhs.getHeight()) - (rhs.getWidth() * (long)rhs.getHeight()));
            }
        }));

        return choices[0];
    }
    private void openCamera() {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                Log.d(TAG, id);
            }
            String cameraId = "0"; // Rear camera ID (adjust as needed)

            Size size = getResolution(cameraManager, cameraId);

            Size imageSize = new Size(size.getWidth(), size.getHeight()); // Example size
            imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCaptureSession() throws CameraAccessException {
        List<OutputConfiguration> outputConfig = Collections.singletonList(new OutputConfiguration(imageReader.getSurface()));
        cameraDevice.createCaptureSessionByOutputConfigurations(outputConfig, captureSessionCallback, null);
    }
}