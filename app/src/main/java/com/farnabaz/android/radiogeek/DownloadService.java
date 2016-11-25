package com.farnabaz.android.radiogeek;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DownloadService extends IntentService {
	public static final String RADIOGEEK_DOWNLOAD_ACTION = "RGDLA";
	public static final String RADIOGEEK_DOWNLOAD_COMPLETE_ACTION = "RGDLAC";
	public static final String RADIOGEEK_DOWNLOAD_INTERUPTED_ACTION = "RGDLIA";
	public static final String RADIOGEEK_STATE_ACTION = "RGDLAState";
	public static final String CURRENT_PROGRESS = "progress";
	public static final String TOTAL_PROGRESS = "total";

	public static final int STATE_ACTIVE_DOWNLOAD = 0;
	public static final int STATE_PAUSE_DOWNLOAD = 1;
	public static final int STATE_CANCEL_DOWNLOAD = 2;
	public static final int STATE_CANCELED_DOWNLOAD = 3;

	private int number;
	private int downloadState = STATE_ACTIVE_DOWNLOAD;
	private ServiceReceiver receiver;

	long fileLength = 0;
	long total = 0;

	public DownloadService() {
		super("DownloadService");
	}

	@Override
	public void onCreate() {
		IntentFilter filter = new IntentFilter(
				DownloadService.RADIOGEEK_STATE_ACTION);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ServiceReceiver();
		registerReceiver(receiver, filter);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		int id = 1;
		String finalMessage = "Download Complete";
		String title = intent.getStringExtra("title");
		NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);
		mBuilder.setContentTitle(title)
				.setTicker(title)
				.setContentText("Download in progress")
				.setSmallIcon(R.drawable.ic_favicon_144)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent
								.getActivity(getBaseContext(), 0, new Intent(
										getBaseContext(), MainActivity.class),
										Intent.FLAG_ACTIVITY_SINGLE_TOP));

		// update notification
		mBuilder.setProgress(100, 0, true);
		mNotifyManager.notify(id, mBuilder.build());
		// END -- update notification
		String urlToDownload = intent.getStringExtra("url");
		String destination = intent.getStringExtra("destination");
		number = intent.getIntExtra("id", 0);

		File temp = new File(destination + ".tmp");
		InputStream input = null;

		Long downloadedSize = (long) 0;
		RandomAccessFile outputr = null;

		try {
			URL url = new URL(urlToDownload);
			URLConnection connection = url.openConnection();
			// /// ----
			if (temp.exists()) {
				connection.setAllowUserInteraction(true);
				connection.setRequestProperty("Range", "bytes=" + temp.length()
						+ "-");
			}

			connection.setConnectTimeout(14000);
			connection.setReadTimeout(20000);
			connection.connect();

			String connectionField = connection.getHeaderField("content-range");

			if (connectionField != null) {
				String[] connectionRanges = connectionField.substring(
						"bytes=".length()).split("-");
				downloadedSize = Long.valueOf(connectionRanges[0]);
			}

			if (connectionField == null && temp.exists())
				temp.delete();

			fileLength = connection.getContentLength() + downloadedSize;
			int _length = (int) (fileLength / 1000);
			input = new BufferedInputStream(connection.getInputStream());
			outputr = new RandomAccessFile(temp, "rw");
			outputr.seek(downloadedSize);

			byte data[] = new byte[1024];
			int count = 0;

			while ((count = input.read(data, 0, 1024)) != -1
					&& fileLength != downloadedSize) {
				downloadedSize += count;
				outputr.write(data, 0, count);

				// update notification
				mBuilder.setProgress(_length, (int) (downloadedSize / 1000),
						false);
				mBuilder.setContentText("[" + (downloadedSize / 1000) + "/"
						+ _length + "] Download in progress");
				mNotifyManager.notify(id, mBuilder.build());
				// END -- update notification

				send(downloadedSize, RADIOGEEK_DOWNLOAD_ACTION);
				if (downloadState != STATE_ACTIVE_DOWNLOAD) {
					finalMessage = "Download Stoped";
					break;
				}
			}

			outputr.close();
			input.close();
		} catch (FileNotFoundException efnf) {
			downloadState = STATE_CANCELED_DOWNLOAD;
			finalMessage = "Cannot create temp file";
			send(0, RADIOGEEK_DOWNLOAD_INTERUPTED_ACTION);
		} catch (IOException e) {
			downloadState = STATE_CANCELED_DOWNLOAD;
			finalMessage = "Cannot communicating with server";
			send(0, RADIOGEEK_DOWNLOAD_INTERUPTED_ACTION);
			try {
				outputr.close();
				input.close();
			} catch (Exception e2) {

			}
		}
		if (downloadState == STATE_ACTIVE_DOWNLOAD
				&& fileLength == downloadedSize) {
			temp.renameTo(new File(destination));
			send(100, RADIOGEEK_DOWNLOAD_COMPLETE_ACTION);
		}
		// update notification
		mBuilder.setContentText(finalMessage).setTicker(finalMessage)
				.setProgress(0, 0, false).setOngoing(false);
		mNotifyManager.notify(id, mBuilder.build());
		// END -- update notification
	}

	private void send(long progress, String action) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(action);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(CURRENT_PROGRESS, (int) (progress / 1000));
		broadcastIntent.putExtra(TOTAL_PROGRESS, (int) (fileLength / 1000));
		broadcastIntent.putExtra("id", number);
		sendBroadcast(broadcastIntent);
	}

	private class ServiceReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			downloadState = arg1.getIntExtra("State", STATE_ACTIVE_DOWNLOAD);
			if (downloadState == STATE_CANCEL_DOWNLOAD) {
				Toast.makeText(DownloadService.this, "Download Stoped",
						Toast.LENGTH_SHORT).show();
				stopSelf();
			}
		}

	}
}