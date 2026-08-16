package com.example.webwrap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "webwrap";
    private static final String KEY_URL = "url";

    private WebView webView;
    private ScrollView setupScreen;
    private EditText urlInput;
    private ActivityResultLauncher<ScanOptions> qrLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private static boolean splashPlayed = false; // 进程级：品牌尾屏每次冷启动只播一次

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View splash = findViewById(R.id.splashScreen);
        if (savedInstanceState == null && !splashPlayed) {
            splashPlayed = true;
            playBrandSplash();
        } else {
            splash.setVisibility(View.GONE);
        }

        qrLauncher = registerForActivityResult(new ScanContract(), result -> {
            String scanned = result.getContents();
            if (scanned != null && !scanned.isEmpty()) {
                saveAndLoad(scanned);
            }
        });
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        startScanner();
                    } else {
                        Toast.makeText(this, R.string.toast_camera_denied, Toast.LENGTH_SHORT).show();
                    }
                });

        webView = findViewById(R.id.webview);
        setupScreen = findViewById(R.id.setupScreen);
        urlInput = findViewById(R.id.urlInput);

        findViewById(R.id.manualToggle).setOnClickListener(v -> {
            View manual = findViewById(R.id.manualInput);
            boolean show = manual.getVisibility() != View.VISIBLE;
            manual.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                urlInput.requestFocus();
            }
        });
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                submitUrl();
                return true;
            }
            return false;
        });
        findViewById(R.id.goButton).setOnClickListener(v -> submitUrl());
        findViewById(R.id.scanButton).setOnClickListener(v -> startScanWithPermission());
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 深色模式下不强制反转颜色，交给网页自己处理
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                // http/https 留在 app 内，其他 scheme（mailto:、tel:、intent: 等）交给系统
                if (scheme.equals("http") || scheme.equals("https")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        // 长按屏幕打开设置
        webView.setOnLongClickListener(v -> {
            showSetupScreen();
            return true;
        });

        String url = getPrefs().getString(KEY_URL, null);
        if (url == null || url.isEmpty()) {
            showSetupScreen();
        } else {
            setupScreen.setVisibility(View.GONE);
            webView.loadUrl(url);
        }

        applyImmersive();
    }

    /** 品牌尾屏：图标回弹入场 → 标题/细线/邮箱错落上浮 → 整页淡出 */
    private void playBrandSplash() {
        View splash = findViewById(R.id.splashScreen);
        View icon = findViewById(R.id.splashIcon);
        View title = findViewById(R.id.splashTitle);
        View line = findViewById(R.id.splashLine);
        View email = findViewById(R.id.splashEmail);

        AnimatorSet iconSet = new AnimatorSet();
        iconSet.playTogether(
                ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.6f, 1f),
                ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.6f, 1f),
                ObjectAnimator.ofFloat(icon, View.ALPHA, 0f, 1f));
        iconSet.setDuration(500);
        iconSet.setInterpolator(new OvershootInterpolator(1.4f));

        AnimatorSet textSet = new AnimatorSet();
        textSet.playSequentially(
                riseIn(title, 220),
                riseIn(line, 180),
                riseIn(email, 200));

        AnimatorSet all = new AnimatorSet();
        all.playTogether(iconSet, textSet);
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                splash.postDelayed(() -> {
                    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(splash, View.ALPHA, 1f, 0f);
                    fadeOut.setDuration(500);
                    fadeOut.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            splash.setVisibility(View.GONE);
                        }
                    });
                    fadeOut.start();
                }, 900);
            }
        });
        all.start();
    }

    /** 上浮 + 淡入 */
    private AnimatorSet riseIn(View view, long duration) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 24f, 0f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f));
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator(1.6f));
        return set;
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    /** 显示设置页并回填当前 URL（手动输入区默认收起） */
    private void showSetupScreen() {
        String current = getPrefs().getString(KEY_URL, "");
        urlInput.setText(current);
        urlInput.setSelection(urlInput.getText().length());
        findViewById(R.id.manualInput).setVisibility(View.GONE);
        setupScreen.setVisibility(View.VISIBLE);
    }

    private void submitUrl() {
        String raw = urlInput.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_url, Toast.LENGTH_SHORT).show();
            return;
        }
        saveAndLoad(raw);
    }

    private void startScanWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.scan_prompt));
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        options.setCaptureActivity(ScanActivity.class);
        qrLauncher.launch(options);
    }

    /** 规范化 URL、保存并加载 */
    private void saveAndLoad(String raw) {
        raw = raw.trim();
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = "https://" + raw;
        }
        getPrefs().edit().putString(KEY_URL, raw).apply();
        setupScreen.setVisibility(View.GONE);
        webView.loadUrl(raw);
    }

    private void applyImmersive() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        applyImmersive();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
