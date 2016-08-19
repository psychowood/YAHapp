package com.psychowood.yahapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainCardAdapter.MainCardActionListener {

    private static final String TAG = "YAHMainActivity";
    private List<MainCard> mainCards;
    private Activity main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        main = this;
        super.onCreate(savedInstanceState);

        final Context baseContext = getBaseContext();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_back);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        RecyclerView rv = (RecyclerView)findViewById(R.id.rv_main);
        rv.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(baseContext);
        rv.setLayoutManager(llm);

        initializeCardList();

        MainCardAdapter adapter = new MainCardAdapter(mainCards, this);
        rv.setAdapter(adapter);
    }

    private void initializeCardList(){
        mainCards = new ArrayList<>();
        mainCards.add(new MainCard(R.string.menu_henkaku, getString(R.string.menu_henkaku), getString(R.string.menu_henkaku_action)));
        //mainCards.add(new MainCard(R.string.menu_installvdk, getString(R.string.menu_installvdk), getString(R.string.menu_installvdk_action)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void doMainAction(View v, int position, Context context) {
        MainCard card = mainCards.get(position);
        Log.d(TAG,"clicked " + position);

        final Class<? extends Activity> activityClass;

        int activityId = mainCards.get(position).id;

        switch (activityId) {
            case R.string.menu_henkaku:
                activityClass = HenkakuWebServerActivity.class;
                break;
            default:
                activityClass = null;
        }

        if (activityClass != null) {
            final Intent intent = new Intent(main, activityClass);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Action not supported (yet?)", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void doSubAction(View v, int position, Context context) {
        doMainAction(v, position, context);
    }

}

