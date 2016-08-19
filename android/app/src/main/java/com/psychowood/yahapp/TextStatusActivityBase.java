package com.psychowood.yahapp;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

public class TextStatusActivityBase extends AppCompatActivity {

    protected TextView textView;
    protected Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.statusTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());

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

}
