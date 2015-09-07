package com.farnabaz.android.radiogeek;

import java.io.File;

import com.farnabaz.android.FActivity;
import com.farnabaz.android.MediaPlayerService;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PodcastActivity extends FActivity implements
		OnSeekBarChangeListener {

	private ImageButton btnPlay;

	private MediaPlayer mp;
	private SeekBar progressBar;
	private Handler mHandler = new Handler();
	private TextView audioTotalDurationLabel;
	private TextView audioCurrentDurationLabel;
	private Utilities utils;
	private DataHandler data;

	private Item item;

	private PhoneStateListener phoneStateListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_podcast);

		initToolbar((Toolbar) findViewById(R.id.action_bar), true);
		// Media Player
		progressBar = (SeekBar) findViewById(R.id.player_seekbar);

		audioCurrentDurationLabel = (TextView) findViewById(R.id.current_time);
		audioTotalDurationLabel = (TextView) findViewById(R.id.full_time);
		btnPlay = (ImageButton) findViewById(R.id.player_play);
		mp = new MediaPlayer();
		progressBar.setOnSeekBarChangeListener(this);
		utils = new Utilities();

		/**
		 * Play button click event plays a song and changes button to pause
		 * image pauses a song and changes button to play image
		 * */
		btnPlay.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mp != null) {
					if (mp.isPlaying()) {
						puasePlayer();
					} else {
						resumePlayer();
					}
				}
			}
		});

		data = new DataHandler(this);

		Bundle extra = getIntent().getExtras();
		if (extra != null && extra.containsKey("podcast_number")) {
			data.openDB();
			int number = extra.getInt("podcast_number");
			item = data.getEpisode(number);
			File mp3 = DataHandler.episodeAudio(number);
			if (mp3 != null) {
				playPodcast(mp3.getAbsolutePath());
				Intent intent = new Intent( getApplicationContext(), MediaPlayerService.class );
		        intent.setAction( MediaPlayerService.ACTION_PLAY );
		        intent.putExtra(MediaPlayerService.EXTRA_PATH, mp3.getAbsolutePath());
		        intent.putExtra(MediaPlayerService.EXTRA_TITLE, item.title);
		        intent.putExtra(MediaPlayerService.EXTRA_NUM, item.getId());
		        startService( intent );
			}
			
			// ((TextView)
			// findViewById(R.id.podcast_number)).setText(item.title);
			setTitle(item.title);
			File file = data.podcastContent(number);
			if (file.exists()) {
				WebView web = new WebView(this);
				web.loadUrl("file://" + file.getAbsolutePath());
				// web.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
				WebSettings sett = web.getSettings();
				sett.setBlockNetworkImage(false);
				sett.setBlockNetworkLoads(false);
				sett.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
				((LinearLayout) findViewById(R.id.content_layout)).addView(web);
			} else {
				TextView text = new TextView(this);
				text.setText(R.string.no_content);
				((LinearLayout) findViewById(R.id.content_layout))
						.addView(text);
			}

			if (item.played == Item.ITEM_NOT_PLAYED) {
				data.setItemPlayed(item.getId(), Item.ITEM_PARTIALY_PLAYED);
			}
			data.closeDB();
		}

		// listen to call and pause
		phoneStateListener = new PhoneStateListener() {
			private boolean isPlaying = false;

			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (state == TelephonyManager.CALL_STATE_RINGING) {
					isPlaying = mp.isPlaying();
					if (isPlaying) {
						puasePlayer();
					}
				} else if (state == TelephonyManager.CALL_STATE_IDLE) {
					if (isPlaying) {
						resumePlayer();
					}
				} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
					// A call is dialing, active or on hold
				}
				super.onCallStateChanged(state, incomingNumber);
			}
		};
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	private void puasePlayer() {
		mp.pause();
		btnPlay.setImageResource(R.drawable.ic_play);
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	private void resumePlayer() {
		mp.start();
		btnPlay.setImageResource(R.drawable.ic_pause);
		mHandler.postDelayed(mUpdateTimeTask, 100);
	}

	private void playPodcast(String path) {
		try {
			mp.reset();
			mp.setDataSource(path);

			mp.prepare();
			mp.start();
			mp.seekTo(item.position);
			// Updating progress bar
			updateProgressBar();
			btnPlay.setImageResource(R.drawable.ic_pause);
		} catch (Exception e) {
		}
	}

	public static Intent createIntent(Context context) {
		Intent i = new Intent(context, PodcastActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.player, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_share:
			if (mp != null) {
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				String comment = "@radiojadi #" + PodcastActivity.this.item.id
						+ " ["
						+ utils.milliSecondsToTimer(mp.getCurrentPosition())
						+ "]";
				shareIntent.putExtra(Intent.EXTRA_TEXT, comment);
				startActivity(Intent.createChooser(shareIntent,
						"Share your comment"));
			} else {
				Toast.makeText(PodcastActivity.this, "No Player",
						Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.action_open_site:
			Intent browse = new Intent(Intent.ACTION_VIEW,
					Uri.parse(PodcastActivity.this.item.getPodcastLink()));
			startActivity(browse);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {

	}

	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar() {
		mHandler.postDelayed(mUpdateTimeTask, 100);
	}

	/**
	 * Background Runnable thread
	 * */
	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {
			long totalDuration = mp.getDuration();
			long currentDuration = mp.getCurrentPosition();

			// Displaying Total Duration time
			audioTotalDurationLabel.setText(""
					+ utils.milliSecondsToTimer(totalDuration));
			audioCurrentDurationLabel.setText(""
					+ utils.milliSecondsToTimer(currentDuration));

			// Updating progress bar
			int progress = (int) (utils.getProgressPercentage(currentDuration,
					totalDuration));
			progressBar.setProgress(progress);

			if (totalDuration <= currentDuration) {
				mp.seekTo(0);
				mp.stop();
				data.openDB();
				data.setItemPlayed(item.getId(), Item.ITEM_COMPLETELY_PLAYED);
				data.closeDB();
			}
			// Running this thread after 100 milliseconds
			mHandler.postDelayed(this, 100);
		}
	};

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	/**
	 * When user stops moving the progress handler
	 * */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = mp.getDuration();
		int currentPosition = utils.progressToTimer(seekBar.getProgress(),
				totalDuration);

		mp.seekTo(currentPosition);
		updateProgressBar();
	}

	@Override
	public void onDestroy() {
		data.openDB();
		data.setItemPosition(item.getId(), mp.getCurrentPosition());
		mHandler.removeCallbacks(mUpdateTimeTask);
		data.close();
		mp.release();
		mp = null;
		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		super.onDestroy();
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
