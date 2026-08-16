package com.example.webwrap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "webwrap";
    private static final String KEY_URL = "url";
    private static final String ACTION_SETTINGS = "com.zhouwjfi.zcode.SETTINGS";

    private WebView webView;
    private View setupScreen;
    private EditText urlInput;
    private BarcodeView barcodeView;
    private View cameraOverlay;
    private View scanLine;
    private boolean manualMode = false;
    private ObjectAnimator scanAnim;
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

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        startEmbeddedScanner();
                    } else {
                        Toast.makeText(this, R.string.toast_camera_denied, Toast.LENGTH_SHORT).show();
                    }
                });

        webView = findViewById(R.id.webview);
        setupScreen = findViewById(R.id.setupScreen);
        urlInput = findViewById(R.id.urlInput);
        barcodeView = findViewById(R.id.barcodeView);
        cameraOverlay = findViewById(R.id.cameraOverlay);
        scanLine = findViewById(R.id.scanLine);

        // 占位层可点击：仅在未授权时重新申请（手动输入模式下点击不唤醒相机）
        cameraOverlay.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
        // 扫码 ↔ 手动 模式切换
        findViewById(R.id.manualToggle).setOnClickListener(v -> switchMode(manualMode = !manualMode));
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                submitUrl();
                return true;
            }
            return false;
        });
        findViewById(R.id.goButton).setOnClickListener(v -> submitUrl());
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

        // 设置入口1：桌面图标长按快捷方式（见 onNewIntent）
        // 设置入口2：网页退无可退时按返回，弹退出/设置对话框（见 onBackPressed）
        // 首次启动未配置 URL 时直接显示设置页

        String url = getPrefs().getString(KEY_URL, null);
        if (url == null || url.isEmpty()) {
            showSetupScreen();
        } else {
            setupScreen.setVisibility(View.GONE);
            webView.loadUrl(url);
        }
        // 冷启动直接走快捷方式的情况
        if (ACTION_SETTINGS.equals(getIntent().getAction())) {
            showSetupScreen();
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

    /** 扫码 ↔ 手动 输入模式切换 */
    private void switchMode(boolean manual) {
        findViewById(R.id.scanArea).setVisibility(manual ? View.GONE : View.VISIBLE);
        findViewById(R.id.manualArea).setVisibility(manual ? View.VISIBLE : View.GONE);
        TextView toggle = findViewById(R.id.manualToggle);
        toggle.setText(manual ? R.string.scan_toggle : R.string.manual_toggle);
        if (manual) {
            stopEmbeddedScanner();
            urlInput.requestFocus();
            urlInput.setSelection(urlInput.getText().length());
        } else {
            prepareCamera();
        }
    }

    /** 显示设置页并回填当前 URL（默认扫码模式），并启动嵌入式扫码 */
    private void showSetupScreen() {
        String current = getPrefs().getString(KEY_URL, "");
        urlInput.setText(current);
        urlInput.setSelection(urlInput.getText().length());
        manualMode = false;
        switchMode(false);
        setupScreen.setVisibility(View.VISIBLE);
        applyImmersive();
    }

    private void submitUrl() {
        String raw = urlInput.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_url, Toast.LENGTH_SHORT).show();
            return;
        }
        saveAndLoad(raw);
    }

    /** 已授权直接开扫；未授权立即弹权限申请（占位层仅作等待提示） */
    private void prepareCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startEmbeddedScanner();
        } else {
            cameraOverlay.setVisibility(View.VISIBLE);
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** 嵌入式扫码：取景框内实时预览，识别即连 */
    private void startEmbeddedScanner() {
        cameraOverlay.setVisibility(View.GONE);
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String text = result.getText();
                if (text != null && !text.isEmpty()) {
                    barcodeView.pause();
                    saveAndLoad(text);
                }
            }
        });
        barcodeView.resume();
        startScanLine();
    }

    private void stopEmbeddedScanner() {
        barcodeView.pause();
        barcodeView.stopDecoding();
        if (scanAnim != null) {
            scanAnim.cancel();
            scanAnim = null;
        }
    }

    /** 扫描线往返动画 */
    private void startScanLine() {
        if (scanAnim != null) return;
        scanLine.post(() -> {
            float h = ((View) scanLine.getParent()).getHeight() - scanLine.getHeight();
            scanAnim = ObjectAnimator.ofFloat(scanLine, View.TRANSLATION_Y, h * 0.06f, h * 0.94f);
            scanAnim.setDuration(1300);
            scanAnim.setRepeatCount(ValueAnimator.INFINITE);
            scanAnim.setRepeatMode(ValueAnimator.REVERSE);
            scanAnim.start();
        });
    }

    /** 规范化 URL、保存并加载 */
    private void saveAndLoad(String raw) {
        raw = raw.trim();
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = "https://" + raw;
        }
        getPrefs().edit().putString(KEY_URL, raw).apply();
        stopEmbeddedScanner();
        setupScreen.setVisibility(View.GONE);
        applyImmersive();
        webView.loadUrl(raw);
    }

    /** 网页模式：沉浸式全屏；设置页模式：退出 immersive，保证粘贴等选择浮层能弹出 */
    private void applyImmersive() {
        boolean setup = setupScreen != null && setupScreen.getVisibility() == View.VISIBLE;
        if (setup) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            return;
        }
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
        if (setupScreen.getVisibility() == View.VISIBLE) {
            stopEmbeddedScanner();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        applyImmersive();
        if (setupScreen.getVisibility() == View.VISIBLE && !manualMode
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
            startEmbeddedScanner();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SETTINGS.equals(intent.getAction())) {
            showSetupScreen();
        }
    }

    @Override
    public void onBackPressed() {
        if (setupScreen.getVisibility() == View.VISIBLE) {
            super.onBackPressed(); // 设置页再按返回直接退出
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        // 设置入口2：退出前询问
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_dialog_title)
                .setPositiveButton(R.string.exit_dialog_settings, (d, w) -> showSetupScreen())
                .setNegativeButton(R.string.exit_dialog_exit, (d, w) -> finish())
                .setCancelable(true)
                .show();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
