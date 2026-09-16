package com.championbrain.overlay;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import androidx.annotation.Nullable;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 41;
    private static final int CAPTURE_REQUEST = 42;
    private MediaProjectionManager projectionManager;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        setContentView(buildScreen());
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 43);
        }
    }

    private LinearLayout buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(42, 80, 42, 42); root.setBackgroundColor(Color.rgb(7,11,21));
        TextView badge = text("◉", 42, 0xFFF7C948); root.addView(badge);
        TextView title = text("Champion Brain", 30, Color.WHITE); title.setPadding(0,20,0,8); root.addView(title);
        TextView copy = text("A floating battle assistant for Pokémon Champions. Screenshots are analyzed on your phone and discarded after each recommendation.", 17, 0xFFB8C3DA); copy.setGravity(Gravity.CENTER); copy.setPadding(0,0,0,34); root.addView(copy);
        Button start = new Button(this); start.setText("Start overlay assistant"); start.setAllCaps(false); start.setTextSize(18); start.setOnClickListener(v -> {
            try { startFlow(); }
            catch (Exception error) { Toast.makeText(this, "Could not start: " + error.getMessage(), Toast.LENGTH_LONG).show(); }
        });
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, -2);
        startParams.setMargins(0, 12, 0, 12);
        root.addView(start, startParams);
        TextView note = text("You will approve screen capture when a session begins. Champion Brain recommends a play; it never taps the game for you.", 14, 0xFF8593B0); note.setGravity(Gravity.CENTER); note.setPadding(0,28,0,0); root.addView(note);
        return root;
    }

    private TextView text(String value, int size, int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t; }

    private void startFlow() {
        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, OVERLAY_REQUEST); return;
        }
        startActivityForResult(projectionManager.createScreenCaptureIntent(), CAPTURE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST) { if (Settings.canDrawOverlays(this)) startFlow(); return; }
        if (requestCode == CAPTURE_REQUEST && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, OverlayService.class);
            service.putExtra("resultCode", resultCode); service.putExtra("resultData", data);
            startForegroundService(service); Toast.makeText(this, "Champion Brain is ready", Toast.LENGTH_SHORT).show(); finish();
        }
    }
}
