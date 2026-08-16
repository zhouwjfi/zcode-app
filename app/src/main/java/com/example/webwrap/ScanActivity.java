package com.example.webwrap;

import android.content.pm.ActivityInfo;

import com.journeyapps.barcodescanner.CaptureActivity;

/** 竖屏扫码页（覆盖库默认的横屏锁定） */
public class ScanActivity extends CaptureActivity {
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
    }
}
