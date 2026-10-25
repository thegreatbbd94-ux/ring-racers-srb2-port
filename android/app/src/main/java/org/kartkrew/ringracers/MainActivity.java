package org.kartkrew.ringracers;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_STORAGE = 1;

    private final ExecutorService extractor = Executors.newSingleThreadExecutor();
    private ProgressBar progress;
    private TextView status;
    private Button retry;
    private Button grantStorage;
    private Button skipStorage;
    private boolean waitingForStorage;
    private boolean preparing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(createLoadingView());
        if (AssetExtractor.hasSharedStorageAccess(this)) {
            prepareGame();
        } else {
            askForStorage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coming back from the "All files access" settings screen.
        if (waitingForStorage && AssetExtractor.hasSharedStorageAccess(this)) {
            prepareGame();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE && AssetExtractor.hasSharedStorageAccess(this)) {
            prepareGame();
        }
    }

    @Override
    protected void onDestroy() {
        extractor.shutdownNow();
        super.onDestroy();
    }

    private View createLoadingView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(48), dp(32), dp(48), dp(32));
        root.setBackgroundColor(Color.rgb(13, 15, 18));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        status = new TextView(this);
        status.setText("Preparing game data");
        status.setTextColor(Color.rgb(194, 201, 207));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.topMargin = dp(16);
        root.addView(status, statusParams);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);
        progress.setMax(16);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(244, 81, 77)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(360), dp(8));
        progressParams.topMargin = dp(18);
        root.addView(progress, progressParams);

        retry = new Button(this);
        retry.setText("Retry");
        retry.setAllCaps(false);
        retry.setVisibility(View.GONE);
        retry.setOnClickListener(view -> prepareGame());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(120), dp(48));
        retryParams.topMargin = dp(20);
        root.addView(retry, retryParams);

        grantStorage = new Button(this);
        grantStorage.setText("Allow storage access");
        grantStorage.setAllCaps(false);
        grantStorage.setVisibility(View.GONE);
        grantStorage.setOnClickListener(view -> requestStorage());
        LinearLayout.LayoutParams grantParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        grantParams.topMargin = dp(20);
        root.addView(grantStorage, grantParams);

        skipStorage = new Button(this);
        skipStorage.setText("Continue without");
        skipStorage.setAllCaps(false);
        skipStorage.setVisibility(View.GONE);
        skipStorage.setOnClickListener(view -> prepareGame());
        LinearLayout.LayoutParams skipParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        skipParams.topMargin = dp(8);
        root.addView(skipStorage, skipParams);

        return root;
    }

    private void askForStorage() {
        waitingForStorage = true;
        progress.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        status.setText("To install addons and keep your saves in an easy-to-find place, "
            + "Ring Racers needs access to your storage.\n\n"
            + "Your files will go in Internal storage/RingRacers, "
            + "with mods in Internal storage/RingRacers/addons.");
        grantStorage.setVisibility(View.VISIBLE);
        skipStorage.setVisibility(View.VISIBLE);
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: shared storage requires the "All files access" special permission,
            // which can only be granted from the system settings screen.
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException exception) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_STORAGE);
        }
    }

    private void prepareGame() {
        if (preparing) {
            return;
        }
        preparing = true;
        waitingForStorage = false;
        grantStorage.setVisibility(View.GONE);
        skipStorage.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        status.setText("Preparing game data");

        extractor.execute(() -> {
            try {
                AssetExtractor.prepare(this, (completed, total, name) -> runOnUiThread(() -> {
                    progress.setMax(total);
                    progress.setProgress(completed);
                    status.setText(name);
                }));
                AssetExtractor.prepareUserHome(this);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startActivity(new Intent(this, GameActivity.class));
                        finish();
                    }
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    preparing = false;
                    status.setText(exception.getMessage());
                    progress.setVisibility(View.GONE);
                    retry.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
