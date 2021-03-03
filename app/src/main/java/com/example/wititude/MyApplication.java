package com.example.wititude;

import android.app.Application;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.tracker.ObjectTrackerListener;

public class MyApplication extends Application{


    private ExternalRendering externalRendering;

    @Override
    public void onCreate() {
        super.onCreate();
    }

}
