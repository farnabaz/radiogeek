package com.farnabaz.android.radiogeek;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

public class DownloadService extends IntentService {
	public static final String RADIOGEEK_DOWNLOAD_ACTION = "RGDLA";
	public static final String RADIOGEEK_DOWNLOAD_COMPLETE_ACTION = "RGDLAC";
	public static final String RADIOGEEK_STATE_ACTION = "RGDLAState";
	public static final String CURRENT_PROGRESS = "progress";
	public static final String TOTAL_PROGRESS = "total";

	public static final int STATE_ACTIVE_DOWNLOAD = 0;
	public static final int STATE_PAUSE_DOWNLOAD = 1;
	public static final int STATE_CANCEL_DOWNLOAD = 2;

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
		String urlToDownload = intent.getStringExtra("url");
		String destination = intent.getStringExtra("destination");
		number = intent.getIntExtra("id", 0);

		// boolean isComplete = false;
		File temp = new File(destination + ".tmp");
		// int downloaded = 0;
		InputStream input = null;
		// OutputStream output = null;

		Long downloadedSize = (long) 0;
		RandomAccessFile outputr = null;

		// ///
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

			// if (connection.getResponseCode() / 100 != 2){}
			// throw new Exception("Invalid response code!");
			// else
			// {
			String connectionField = connection.getHeaderField("content-range");

			if (connectionField != null) {
				String[] connectionRanges = connectionField.substring(
						"bytes=".length()).split("-");
				downloadedSize = Long.valueOf(connectionRanges[0]);
			}

			if (connectionField == null && temp.exists())
				temp.delete();

			fileLength = connection.getContentLength() + downloadedSize;
			input = new BufferedInputStream(connection.getInputStream());
			outputr = new RandomAccessFile(temp, "rw");
			outputr.seek(downloadedSize);

			byte data[] = new byte[1024];
			int count = 0;
			int __progress = 0;

			while ((count = input.read(data, 0, 1024)) != -1
					&& __progress != 100) {
				downloadedSize += count;
				outputr.write(data, 0, count);
				Log.i("dd", fileLength + "-" + downloadedSize);
				__progress = (int) ((downloadedSize * 100) / fileLength);
				send(downloadedSize, RADIOGEEK_DOWNLOAD_ACTION);
				if (downloadState != STATE_ACTIVE_DOWNLOAD) {
					break;
				}
			}

			outputr.close();
			input.close();
			// }
			// /// ----
			/*
			 * if (temp.exists()) { downloaded = (int) temp.length();
			 * connection.setRequestProperty("Range", "bytes=" + (downloaded) +
			 * "-"); total = downloaded; } // connection.setDoInput(true); //
			 * connection.setDoOutput(true); // connection.connect(); // this
			 * will be useful so that you can show a typical 0-100% fileLength =
			 * connection.getContentLength();
			 * 
			 * // download the file input = new
			 * BufferedInputStream(url.openStream()); output = (downloaded == 0)
			 * ? new FileOutputStream(temp) : new FileOutputStream(temp, true);
			 * byte data[] = new byte[1024]; int count; while ((count =
			 * input.read(data)) != -1) { total += count; // publishing the
			 * progress.... send((int) total, RADIOGEEK_DOWNLOAD_ACTION);
			 * output.write(data, 0, count); Log.i("dd", fileLength+"-"+total);
			 * if (downloadState != STATE_ACTIVE_DOWNLOAD) { break; } }
			 * 
			 * output.flush(); output.close(); input.close(); // isComplete =
			 * true;
			 */
		} catch (IOException e) {
			Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
			try {
				outputr.close();
				input.close();
			} catch (Exception e2) {
				Toast.makeText(this, "Error2", Toast.LENGTH_SHORT).show();
			}
		}
		if (downloadState == STATE_ACTIVE_DOWNLOAD
				&& fileLength == downloadedSize) {
			temp.renameTo(new File(destination));
			send(100, RADIOGEEK_DOWNLOAD_COMPLETE_ACTION);
		}
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