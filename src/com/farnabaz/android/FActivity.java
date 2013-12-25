package com.farnabaz.android;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.farnabaz.android.radiogeek.DownloadService;
import com.farnabaz.android.radiogeek.R;
import com.nullwire.trace.ExceptionHandler;

public abstract class FActivity extends SherlockActivity {

	private ProgressDialog mProgressDialog;

	private MainDataReciver mReceiver;

	protected abstract void onDownloadFinish();

	protected abstract void onDownloadStopped();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// send exceptions to server
		ExceptionHandler.register(this,
				"http://4paye.ir/external/log/server.php");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.download)
				.setOnMenuItemClickListener(
						new MenuItem.OnMenuItemClickListener() {

							@Override
							public boolean onMenuItemClick(MenuItem item) {
								if (isDownloadServiceRunning()) {
									showProgressDialog();
								} else {
									showShortMessageInToast(R.string.no_download);
								}
								return true;
							}
						}).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * show message in android toast
	 * 
	 * @param message
	 *            Message to show
	 */
	protected void showShortMessageInToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * show message in android toast
	 * 
	 * @param message
	 *            Message to show
	 */
	protected void showShortMessageInToast(int message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * return true is a download is running with <code>DownloadService</code>
	 * 
	 * @return
	 */
	protected boolean isDownloadServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.farnabaz.android.radiogeek.DownloadService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check for android connected to network
	 * 
	 * @return true if connected
	 */
	protected boolean isConnectedToNetwork() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	private void hideProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	protected void showProgressDialog() {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("Download");
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Hide",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						hideProgressDialog();
					}
				});
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				"Stop Download", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						hideProgressDialog();

						Intent broadcastIntent = new Intent();
						broadcastIntent
								.setAction(DownloadService.RADIOGEEK_STATE_ACTION);
						broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
						broadcastIntent.putExtra("State",
								DownloadService.STATE_CANCEL_DOWNLOAD);
						sendBroadcast(broadcastIntent);
						onDownloadStopped();
					}
				});
		mProgressDialog.show();
		IntentFilter filter = new IntentFilter(
				DownloadService.RADIOGEEK_DOWNLOAD_ACTION);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		mReceiver = new MainDataReciver();
		registerReceiver(mReceiver, filter);
	}

	private class MainDataReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			int progress = arg1
					.getIntExtra(DownloadService.CURRENT_PROGRESS, 0);
			int total = arg1.getIntExtra(DownloadService.TOTAL_PROGRESS, 0);
			if (mProgressDialog != null) {
				mProgressDialog.setProgress(progress);
				mProgressDialog.setMax(total);
			}
			if (progress == total) {
				hideProgressDialog();
				onDownloadFinish();
			}
		}

	}

}
