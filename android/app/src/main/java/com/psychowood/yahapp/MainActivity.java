package com.psychowood.yahapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.thefinestartist.finestwebview.FinestWebView;
import com.thefinestartist.finestwebview.listeners.WebViewListener;

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
        mainCards.add(new MainCard(R.string.menu_installvpk, getString(R.string.menu_installvpk), getString(R.string.menu_installvpk_action)));
        mainCards.add(new MainCard(R.string.menu_browsevpkmirror, getString(R.string.menu_browsevpkmirror), getString(R.string.menu_browsevpkmirror_action)/*, R.mipmap.vpkmirror_logo, 0x403ec4 */));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about: {

                final TextView message = new TextView(this);
                message.setPadding(25,25,25,25);
                final SpannableString s =
                        new SpannableString(this.getText(R.string.about_text));
                Linkify.addLinks(s, Linkify.WEB_URLS);
                message.setText(s);
                message.setMovementMethod(LinkMovementMethod.getInstance());

                String version = null;
                try {
                    version = App.getPackageInfo(this).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG,"Cannot get version info",e);
                    version = "";
                }

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.about_title) + " " + version)
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, null)
                        .setView(message)
                        .create().show();
                break;
            }
            case R.id.action_check_updates: {
                final Intent intent = new Intent(main, UpdateCheckerActivity.class);
                this.startActivity(intent);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void doMainAction(View v, int position, final Context context) {
        MainCard card = mainCards.get(position);
        Log.d(TAG,"clicked " + position);

        Class<? extends Activity> activityClass = null;

        int activityId = mainCards.get(position).id;

        switch (activityId) {
            case R.string.menu_henkaku:
                activityClass = HenkakuWebServerActivity.class;
                break;
            case R.string.menu_installvpk:
                //activityClass = FtpClientActivity.class;
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.menu_installvpk))
                        .setMessage(getString(R.string.menu_installvpk_help))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;
            case R.string.menu_browsevpkmirror:
                new FinestWebView.Builder(this)
                        /*
                        .webViewJavaScriptEnabled(true)
                        .webViewAllowContentAccess(true)
                        .webViewAllowFileAccess(true)
                        .webViewAllowFileAccessFromFileURLs(true)
                        .webViewAllowUniversalAccessFromFileURLs(true)
                        .webViewJavaScriptCanOpenWindowsAutomatically(true)
                        .webViewSupportMultipleWindows(true)
                        */
                        .backPressToClose(true)
                        .setWebViewListener(new WebViewListener() {
                            @Override
                            public void onPageStarted(String url) {
                                super.onPageStarted(url);
                            }

                            @Override
                            public void onPageFinished(String url) {
                                super.onPageFinished(url);
                            }

                            @Override
                            public void onLoadResource(String url) {
                                super.onLoadResource(url);
                            }

                            @Override
                            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                                if (!url.toLowerCase().endsWith(".vpk")) {
                                    Toast.makeText(context, R.string.error_not_a_vpk_file, Toast.LENGTH_SHORT).show();
                                } else {
                                    final Intent intent = new Intent(main, FtpClientActivity.class);
                                    intent.putExtra(FtpClientActivity.INTENTEXTRA_DOWNLOAD_VPK_URL, url);
                                    context.startActivity(intent);
                                }
                                //super.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength);
                            }
                        })
                        .show(R.string.url_vpkmirror);
                break;
            default:
                activityClass = null;
        }

        if (activityClass != null) {
            final Intent intent = new Intent(main, activityClass);
            context.startActivity(intent);
        }
    }

    @Override
    public void doSubAction(View v, int position, Context context) {
        doMainAction(v, position, context);
    }

}

