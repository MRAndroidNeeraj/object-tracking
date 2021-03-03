package com.example.wititude;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.wititude.rendering.external.CustomSurfaceView;
import com.example.wititude.rendering.external.Driver;
import com.example.wititude.rendering.external.GLRenderer;
import com.example.wititude.rendering.external.OccluderCube;
import com.example.wititude.rendering.external.StrokedCube;
import com.example.wititude.util.DropDownAlert;
import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.CallStatus;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.devicesupport.Feature;
import com.wikitude.common.permission.PermissionManager;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.tracker.ObjectTarget;
import com.wikitude.tracker.ObjectTracker;
import com.wikitude.tracker.ObjectTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import java.util.Arrays;
import java.util.EnumSet;

public class MainActivity extends AppCompatActivity implements ObjectTrackerListener, ExternalRendering {


    private static final String TAG = "SimpleObjectTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;
    private DropDownAlert dropDownAlert;

    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        if(!checkPermission()){
            requestPermission();
        }

        WikitudeSDK.deleteRootCacheDirectory(this);

        addFeature();
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
    }

    private void initWikitudeSDK(){
        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(Constants.licenceKey);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);
        wikitudeSDK.onCreate(getApplicationContext(),this,startupConfiguration);

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/bottle.wto");
        wikitudeSDK.getTrackerManager().createObjectTracker(targetCollectionResource, MainActivity.this, null);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Loading Target:");
        dropDownAlert.setTextWeight(1);
        dropDownAlert.show();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
        customSurfaceView.onResume();
        driver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        customSurfaceView.onPause();
        driver.stop();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }

    @Override
    public void onTargetsLoaded(ObjectTracker objectTracker) {
        Log.v(TAG, "Object tracker loaded");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dropDownAlert.setText("Scan Target:");
                //dropDownAlert.addImages("firetruck_image.png");
                dropDownAlert.addImages("bottle_image.jpg");
                dropDownAlert.setTextWeight(0.5f);
            }
        });
    }

    @Override
    public void onErrorLoadingTargets(ObjectTracker objectTracker, WikitudeError wikitudeError) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + wikitudeError.getMessage());
    }

    @Override
    public void onObjectRecognized(ObjectTracker objectTracker, ObjectTarget objectTarget) {
        Log.v(TAG, "Recognized target " + objectTarget.getName());
        dropDownAlert.dismiss();

        StrokedCube strokedCube = new StrokedCube();
        OccluderCube occluderCube = new OccluderCube();

        glRenderer.setRenderablesForKey(objectTarget.getName(), strokedCube, occluderCube);
    }

    @Override
    public void onObjectTracked(ObjectTracker objectTracker, ObjectTarget target) {
        StrokedCube strokedCube = (StrokedCube) glRenderer.getRenderableForKey(target.getName());
        if (strokedCube != null) {
            strokedCube.viewMatrix = target.getViewMatrix();
            strokedCube.setXScale(target.getTargetScale().x);
            strokedCube.setYScale(target.getTargetScale().y);
            strokedCube.setZScale(target.getTargetScale().z);
        }

        OccluderCube occluderCube = (OccluderCube) glRenderer.getOccluderForKey(target.getName());
        if (occluderCube != null) {
            occluderCube.viewMatrix = target.getViewMatrix();
            occluderCube.setXScale(target.getTargetScale().x);
            occluderCube.setYScale(target.getTargetScale().y);
            occluderCube.setZScale(target.getTargetScale().z);
        }
    }

    @Override
    public void onObjectLost(ObjectTracker objectTracker, ObjectTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName());
    }

    @Override
    public void onExtendedTrackingQualityChanged(ObjectTracker objectTracker, ObjectTarget objectTarget, int i, int i1) {

    }

    @Override
    public void onRenderExtensionCreated(RenderExtension renderExtension) {
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);
        setContentView(customSurfaceView);
    }

    public void showDeviceMissingFeatures(String errorMessage) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.device_missing_features)
                .setMessage(errorMessage)
                .show();
    }

    private void onDeviceComapitble(){
        WikitudeSDK.getPermissionManager().checkPermissions(this, new String[]{Manifest.permission.CAMERA}, PermissionManager.WIKITUDE_PERMISSION_REQUEST, new PermissionManager.PermissionManagerCallback() {
            @Override
            public void permissionsGranted(int requestCode) {
                initWikitudeSDK();
            }

            @Override
            public void permissionsDenied(String[] deniedPermissions) {
                Toast.makeText(MainActivity.this, "The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(deniedPermissions), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void showPermissionRationale(final int requestCode, final String[] permissions) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("Wikitude Permissions");
                alertBuilder.setMessage("The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(permissions));
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WikitudeSDK.getPermissionManager().positiveRationaleResult(requestCode, permissions);
                    }
                });

                AlertDialog alert = alertBuilder.create();
                alert.show();
            }
        });
    }

    private void addFeature(){
        CallStatus callStatus = WikitudeSDK.isDeviceSupporting(this, EnumSet.of(Feature.OBJECT_TRACKING));
        if (callStatus.isSuccess()) {
            onDeviceComapitble();
        } else {
            showDeviceMissingFeatures(callStatus.getError().getMessage());
        }
    }
}