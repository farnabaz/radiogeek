package com.farnabaz.android.radiogeek;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;

import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.farnabaz.android.FActivity;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FActivity {

	private boolean isDownloadInProgress;

	private ArrayList<Item> episodesList;
	private PodcastListAdapter listAdapter;
	private PullToRefreshListView listView;

	DataHandler dh;

	class RSSUpdator extends AsyncTask<Integer, Integer, Integer> {
		private RSSParser parser = null;

		@Override
		protected void onPreExecute() {
			if (isConnectedToNetwork()) {
				if (!isDownloadInProgress) {
					parser = new RSSParser();
				} else {
					showShortMessageInToast(R.string.downloading);
				}
			} else {
				showShortMessageInToast(R.string.no_internet);
			}
			setDownloadState(true);
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			if (parser != null)
				try {
					parser.setUrl(new URL(DataHandler.PODCAST_URL));
					parser.parse();
				} catch (MalformedURLException e) {
					// Log.e(TAG, "URL Error " + e.getMessage());
				}
			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (parser != null)
				if (parser.hasItem()) {
					showShortMessageInToast(R.string.feed_updated);
					checkFeedList(parser.getRSS());
					listAdapter.notifyDataSetChanged();
				} else {
					showShortMessageInToast(R.string.feed_update_error);
				}
			setDownloadState(false);
			super.onPostExecute(result);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		episodesList = new ArrayList<Item>();
		listAdapter = new PodcastListAdapter(this,
				android.R.layout.simple_list_item_1, episodesList);
		listView = (PullToRefreshListView) findViewById(R.id.podcast_list);
		listView.setAdapter(listAdapter);

		registerForContextMenu(listView.getRefreshableView());
		listView.setOnRefreshListener(new PullToRefreshListView.OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				new RSSUpdator().execute();
			}
		});

		dh = new DataHandler(this);
		dh.openDB();
		dh.getEpisodesList(episodesList);
		dh.closeDB();

		listAdapter.notifyDataSetChanged();

		if (!DataHandler.checkSDCard()) {
			Toast.makeText(this,
					"radiogeek folder could not created on SDCARD",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.podcast_list_context, menu);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Item i;
		switch (item.getItemId()) {
		case R.id.mark_read:
			i = episodesList.get(info.position - 1);
			i.played = Item.ITEM_COMPLETELY_PLAYED;
			dh.openDB();
			dh.setItemPlayed(i.getId(), Item.ITEM_COMPLETELY_PLAYED);
			dh.closeDB();
			listAdapter.notifyDataSetChanged();
			break;
		case R.id.mark_unread:
			i = episodesList.get(info.position - 1);
			i.played = Item.ITEM_NOT_PLAYED;
			dh.openDB();
			dh.setItemPlayed(i.getId(), Item.ITEM_NOT_PLAYED);
			dh.closeDB();
			listAdapter.notifyDataSetChanged();
			break;
		case R.id.view_podcast_page:
			Intent browse = new Intent(Intent.ACTION_VIEW,
					Uri.parse(episodesList.get(info.position - 1).link));
			startActivity(browse);
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		menu.add(R.string.about)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(
							com.actionbarsherlock.view.MenuItem item) {
						startActivity(new Intent(MainActivity.this,
								AboutActivity.class));
						return true;
					}
				})
				.setShowAsAction(
						com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_NEVER);

		return super.onCreateOptionsMenu(menu);
	}

	private void checkFeedList(ArrayList<Item> items) {
		dh.openDB();
		int max = episodesList.size() == 0 ? -1 : episodesList.get(0).getId();
		for (int i = items.size() - 1; i >= 0; i--) {
			Item item = items.get(i);
			if (item.getId() > max) {
				dh.setEpisode(item);
				episodesList.add(0, item);
			}
		}
		dh.closeDB();
	}

	/**
	 * change download state
	 * 
	 * @param state
	 */
	private void setDownloadState(boolean state) {
		isDownloadInProgress = state;
		if (state) {
			listView.setRefreshing(true);
		} else {
			listView.onRefreshComplete();
		}
	}

	class PodcastListAdapter extends ArrayAdapter<Item> implements
			OnClickListener {
		FActivity _context;

		public PodcastListAdapter(FActivity context, int textViewResourceId,
				List<Item> objects) {
			super(context, textViewResourceId, objects);
			_context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inf = _context.getLayoutInflater();
				convertView = inf.inflate(R.layout.layout_podcast_list_item,
						null);
			}
			Item item = getItem(position);
			TextView number = (TextView) convertView
					.findViewById(R.id.podcast_list_item_number);
			TextView title = (TextView) convertView
					.findViewById(R.id.podcast_list_item_title);
			ImageButton image = (ImageButton) convertView
					.findViewById(R.id.podcast_list_item_button);
			ImageView status = (ImageView) convertView
					.findViewById(R.id.podcast_list_item_status);
			number.setText(item.getId() + "");
			String s = (item.description.length() > 100) ? item.description
					.substring(0, 100) + " ..." : item.description;
			((TextView) convertView
					.findViewById(R.id.podcast_list_item_description))
					.setText(s);

			title.setText(item.title);
			switch (item.played) {
			case Item.ITEM_NOT_PLAYED:
				status.setImageResource(R.drawable.ic_w8_new);
				break;
			case Item.ITEM_PARTIALY_PLAYED:
				status.setImageResource(R.drawable.ic_w8_half);
				break;
			default:
				status.setImageDrawable(null);
				break;
			}
			image.setFocusable(false);
			image.setTag(item);
			image.setOnClickListener(this);
			if (!DataHandler.episodeAudio(item.getId()).exists()) {
				image.setImageResource(R.drawable.ic_w8_download);
			} else {
				image.setImageResource(R.drawable.ic_w8_online);
			}

			return convertView;
		}

		@Override
		public void onClick(View arg0) {
			if (arg0 instanceof ImageButton) {
				ImageButton image = (ImageButton) arg0;
				Item item = (Item) image.getTag();
				if (DataHandler.episodeAudio(item.getId()).exists()) {
					Intent i = PodcastActivity.createIntent(MainActivity.this);
					i.putExtra("podcast_number", item.getId());
					startActivity(i);
				} else {
					if (!isDownloadServiceRunning()) {
						Intent msgIntent = new Intent(MainActivity.this,
								DownloadService.class);
						msgIntent.putExtra("id", item.getId());
						msgIntent.putExtra("url", item.getUrl());
						msgIntent.putExtra("destination", DataHandler
								.episodeAudio(item.getId()).getAbsolutePath());
						msgIntent.putExtra("title", item.title);
						startService(msgIntent);
						showProgressDialog();
					} else {
						showShortMessageInToast(R.string.download_only_one);
					}
				}
			}
		}

	}

	@Override
	protected void onDownloadFinish() {
		showShortMessageInToast(R.string.download_finish);
		listAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onDownloadStopped() {

	}
}
