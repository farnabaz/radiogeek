package com.farnabaz.android.radiogeek;

import com.farnabaz.android.FActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends FActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		WebView web = (WebView) findViewById(R.id.about_webview);
		web.loadUrl("file:///android_asset/about.html");
	}

	@Override
	protected void onDownloadFinish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDownloadStopped() {
		// TODO Auto-generated method stub
		
	}

}
