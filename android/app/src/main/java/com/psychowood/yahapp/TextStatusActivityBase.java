package com.psychowood.yahapp;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class TextStatusActivityBase extends AppCompatActivity {

    private String TAG = "YAHapp";

    protected TextView textView;
    protected TextView messageView;
    protected ImageView imageView;
    protected Handler handler;
    protected Activity me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.statusTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        messageView = (TextView) findViewById(R.id.statusMessage);
        imageView = (ImageView) findViewById(R.id.statusImageView);

        handler = new Handler();

        FloatingActionButton fab_bak = (FloatingActionButton) findViewById(R.id.fab_back);
        fab_bak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPause();
                finish();
            }
        });
    }

    protected AlertDialog showError(String message) {
        return new AlertDialog.Builder(me)
            .setTitle(getString(R.string.error))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    protected void log(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,message);
                textView.append(message);
                textView.append("\n");
            }
        });
    }

    protected void showMessage(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,message);
                messageView.setText(message);
            }
        });
    }

    protected SharedPreferences getPrefs() {
        return getSharedPreferences(App.APP_PREFS, MODE_PRIVATE);
    }

    protected SharedPreferences getPrefs(String suffix) {
        if (suffix == null || suffix.length() == 0) {
            throw new IllegalArgumentException("Preferences suffix cannot be null");
        }
        return getSharedPreferences(App.APP_PREFS + "-" + suffix, MODE_PRIVATE);
    }
}
