package com.farnabaz.android;

import java.io.IOException;

import com.farnabaz.android.radiogeek.MainActivity;
import com.farnabaz.android.radiogeek.R;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

/**
 * Created by paulruiz on 10/28/14.
 */
public class MediaPlayerService extends Service {

	public static final String ACTION_PLAY = "action_play";
	public static final String ACTION_PAUSE = "action_pause";
	public static final String ACTION_REWIND = "action_rewind";
	public static final String ACTION_FAST_FORWARD = "action_fast_foward";
	public static final String ACTION_NEXT = "action_next";
	public static final String ACTION_PREVIOUS = "action_previous";
	public static final String ACTION_STOP = "action_stop";

	public static final String EXTRA_TITLE = "__title";
	public static final String EXTRA_PATH = "__path";
	public static final String EXTRA_NUM = "__num";

	private MediaPlayer mMediaPlayer;
	private MediaSessionManager mManager;
	private MediaSessionCompat mSession;
	private MediaControllerCompat mController;
	private String oth;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void handleIntent(Intent intent) {
		if (intent == null || intent.getAction() == null)
			return;

		String action = intent.getAction();

		if (action.equalsIgnoreCase(ACTION_PLAY)) {
			mController.getTransportControls().play();
		} else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
			mController.getTransportControls().pause();
		} else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
			mController.getTransportControls().fastForward();
		} else if (action.equalsIgnoreCase(ACTION_REWIND)) {
			mController.getTransportControls().rewind();
		} else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
			mController.getTransportControls().skipToPrevious();
		} else if (action.equalsIgnoreCase(ACTION_NEXT)) {
			mController.getTransportControls().skipToNext();
		} else if (action.equalsIgnoreCase(ACTION_STOP)) {
			mController.getTransportControls().stop();
		}
	}

	private Action generateAction(int icon, String title, String intentAction) {
		Intent intent = new Intent(getApplicationContext(),
				MediaPlayerService.class);
		intent.setAction(intentAction);
		PendingIntent pendingIntent = PendingIntent.getService(
				getApplicationContext(), 1, intent, 0);
		return new NotificationCompat.Action.Builder(icon, title, pendingIntent)
				.build();
	}

	private void buildNotification(NotificationCompat.Action action) {
//		NotificationCompat.Style f= new NotificationCompat.Style
		Notification.MediaStyle style = new Notification.MediaStyle();

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		// intent.setAction(ACTION_STOP);
		PendingIntent pendingIntent = PendingIntent.getService(
				getApplicationContext(), 1, intent, 0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_play)
				.setShowWhen(false)
				.setLargeIcon(
						BitmapFactory.decodeResource(getResources(),
								R.drawable.ic_player_showcase_image))
				.setContentTitle("RadioGeek " + mId).setContentText(mTitle)
				.setContentIntent(pendingIntent).setDeleteIntent(pendingIntent)
				.setStyle(style);

		builder.addAction(action);
		style.setShowActionsInCompactView(0);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1, builder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mSession == null) {
			initMediaSessions();
			if (intent.getExtras() != null) {
				oth = intent.getExtras().getString(EXTRA_PATH);
				mTitle = intent.getExtras().getString(EXTRA_TITLE);
				mId = intent.getExtras().getInt(EXTRA_NUM);
				mMediaPlayer.reset();
				try {
					mMediaPlayer.setDataSource(oth);
					mMediaPlayer.prepare();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		handleIntent(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	ComponentName mEventReceiver;
	PendingIntent mMediaPendingIntent;
	String mTitle;
	int mId;

	private void initMediaSessions() {
		mMediaPlayer = new MediaPlayer();

		mManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

		mEventReceiver = new ComponentName(getPackageName(),
				MediaPlayerService.class.getName());

		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mEventReceiver);
		mMediaPendingIntent = PendingIntent.getBroadcast(this, 0,
				mediaButtonIntent, 0);
		mSession = new MediaSessionCompat(getApplicationContext(),
				"simple player session", mEventReceiver, mMediaPendingIntent);
		try {
			mController = new MediaControllerCompat(getApplicationContext(),
					mSession.getSessionToken());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
		stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY
				| PlaybackStateCompat.ACTION_PLAY_PAUSE
				| PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
				| PlaybackStateCompat.ACTION_PAUSE);
		stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
		mSession.setPlaybackState(stateBuilder.build());
		mSession.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public void onPlay() {
				super.onPlay();
				Log.e("MediaPlayerService", "onPlay" + mMediaPlayer.isPlaying());
				buildNotification(generateAction(R.drawable.ic_pause, "Pause",
						ACTION_PAUSE));
				mMediaPlayer.start();
			}

			@Override
			public void onPause() {
				super.onPause();
				Log.e("MediaPlayerService",
						"onPause" + mMediaPlayer.isPlaying());
				buildNotification(generateAction(R.drawable.ic_play, "Play",
						ACTION_PLAY));
				mMediaPlayer.pause();
			}

			@Override
			public void onStop() {
				super.onStop();
				Log.e("MediaPlayerService", "onStop");
				// Stop media player here
				// NotificationManager notificationManager =
				// (NotificationManager) getApplicationContext()
				// .getSystemService(Context.NOTIFICATION_SERVICE);
				// notificationManager.cancel(1);
				// Intent intent = new Intent(getApplicationContext(),
				// MediaPlayerService.class);
				// stopService(intent);
			}

			// @Override
			// public void onSetRating(Rating rating) {
			// super.onSetRating(rating);
			// }
		});
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mSession.release();
		return super.onUnbind(intent);
	}
}