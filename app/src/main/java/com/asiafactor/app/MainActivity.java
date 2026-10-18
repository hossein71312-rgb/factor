package com.asiafactor.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Intent;
import android.webkit.ValueCallback;
import android.graphics.Color;
import android.content.res.AssetFileDescriptor;

public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout root;
    private ValueCallback<Uri[]> fileCallback;
    private MediaPlayer videoPlayer, musicPlayer;
    private static final int FILE_PICKER = 510;
    private boolean appShown = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(3,21,47));
        getWindow().setNavigationBarColor(Color.rgb(3,21,47));
        root = new FrameLayout(this);
        setContentView(root);
        if (state == null) showIntro(); else showApp();
    }

    private void showIntro() {
        SurfaceView surface = new SurfaceView(this);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) { playIntro(holder); }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
            @Override public void surfaceDestroyed(SurfaceHolder holder) { releasePlayers(); }
        });
    }

    private void playIntro(SurfaceHolder holder) {
        if (appShown) return;
        try {
            AssetFileDescriptor music = getAssets().openFd("intro.wav");
            musicPlayer = new MediaPlayer();
            musicPlayer.setDataSource(music.getFileDescriptor(), music.getStartOffset(), music.getLength());
            music.close();
            musicPlayer.setVolume(0.65f, 0.65f);
            musicPlayer.setOnCompletionListener(mp -> { mp.release(); if (musicPlayer == mp) musicPlayer = null; });
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (Exception ignored) { }
        try {
            AssetFileDescriptor video = getAssets().openFd("splash.mp4");
            videoPlayer = new MediaPlayer();
            videoPlayer.setDataSource(video.getFileDescriptor(), video.getStartOffset(), video.getLength());
            video.close();
            videoPlayer.setDisplay(holder);
            videoPlayer.setOnCompletionListener(mp -> showApp());
            videoPlayer.setOnErrorListener((mp, what, extra) -> { showApp(); return true; });
            videoPlayer.setOnPreparedListener(MediaPlayer::start);
            videoPlayer.prepareAsync();
        } catch (Exception e) { showApp(); }
    }

    private void releasePlayers() {
        if (videoPlayer != null) { try { videoPlayer.setOnCompletionListener(null); videoPlayer.release(); } catch (Exception ignored) {} videoPlayer = null; }
        if (musicPlayer != null) { try { musicPlayer.release(); } catch (Exception ignored) {} musicPlayer = null; }
    }

    private void showApp() {
        if (appShown) return;
        appShown = true;
        releasePlayers();
        root.removeAllViews();
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadsImagesAutomatically(true);
        s.setDefaultTextEncodingName("UTF-8");
        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_PICKER); return true; }
                catch (Exception e) { fileCallback = null; return false; }
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        releasePlayers();
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
