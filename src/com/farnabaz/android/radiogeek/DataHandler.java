package com.farnabaz.android.radiogeek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

public class DataHandler {

	private static final String DATABASE_NAME = "radiogeek.db";
	private static final int DATABASE_VERSION = 1;

	public static final String ANTIFILTER_BASE_URL = "http://rg.tori.ir";
	public static final String PODCAST_URL = "http://rg.tori.ir/tag/podcast/feed/";
	/**
	 * application main directory
	 */
	public static final String SDCARD_DIRECTORY_PATH = Environment
			.getExternalStorageDirectory() + "/radiogeek/";

	private SQLiteDatabase db;
	private OpenHelper oh;

	public DataHandler(Context context) {
		this.oh = new OpenHelper(context);
	}

	public void close() {
		db.close();
		oh.close();
		db = null;
		oh = null;
		SQLiteDatabase.releaseMemory();
	}

	public void closeDB() {
		db.close();
		db = null;
		SQLiteDatabase.releaseMemory();
	}

	public void openDB() {
		this.db = oh.getWritableDatabase();
	}

	/**
	 * fetch episodes list form database and add to items (clear list)
	 * 
	 * @param items
	 * @return
	 */
	public int getEpisodesList(ArrayList<Item> items) {
		return getEpisodesList(items, true);
	}

	/**
	 * fetch episodes list form database and add to items (clear list)
	 * 
	 * @param items
	 * @param clear
	 *            determine whenever list clear or not
	 * @return
	 */
	public int getEpisodesList(ArrayList<Item> items, boolean clear) {
		Cursor rows = db.rawQuery("SELECT * FROM podcast ORDER BY id DESC",
				null);
		if (clear)
			items.clear();
		if (rows.getCount() == 0) {
			rows.close();
			return 0;
		}
		rows.moveToFirst();
		while (!rows.isAfterLast()) {
			items.add(fetchItem(rows, new Item()));
			rows.moveToNext();
		}
		rows.close();
		return rows.getCount();
	}

	public Item getEpisode(int id) {
		Cursor rows = db.rawQuery("SELECT * FROM podcast WHERE id = " + id,
				null);
		rows.moveToFirst();
		Item item = new Item();
		if (rows.getCount() == 0) {
			rows.close();
			return item;
		}
		while (!rows.isAfterLast()) {
			fetchItem(rows, item);
			rows.moveToNext();
		}
		rows.close();
		return item;
	}

	public void setEpisodes(ArrayList<Item> items) {
		for (Item item : items) {
			setEpisode(item);
		}
	}

	public void setEpisode(Item item) {
		ContentValues cv = new ContentValues();
		cv.put("id", item.getId());
		cv.put("title", item.title);
		cv.put("description", item.description);
		cv.put("pubDate", item.getPubDate() + "");
		cv.put("link", item.link);
		cv.put("mediaUrl", item.mediaUrl);
		db.insertWithOnConflict("podcast", null, cv,
				SQLiteDatabase.CONFLICT_REPLACE);
		try {
			String header = "<html><head><meta http-equiv=\"Content-Type\" "
					+ "content=\"text/html; charset=UTF-8\" />"
					+ "<style type='text/css'>body{direction:rtl;font-size: 110%;line-height: 1.5;}"
					+ "a{color:#00009f;text-decoration:none;}img{max-width: 100%;}</style></head><body>";
			String footer = "</body></html>";
			String html = item.content.replaceAll("http://jadi.net",
					ANTIFILTER_BASE_URL);
			File content = podcastContent(item.getId());
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(content), "UTF-8"));
			out.write(header + html + footer);
			out.flush();
			out.close();
		} catch (Exception e) {

		}
	}

	public void setItemPosition(int id, int pos) {
		ContentValues cv = new ContentValues();
		cv.put("position", pos);
		db.update("podcast", cv, "id = ?", new String[] { id + "" });
	}

	public void setItemPlayed(int id, int dl) {
		ContentValues cv = new ContentValues();
		cv.put("played", dl);
		db.update("podcast", cv, "id = ?", new String[] { id + "" });
	}

	private Item fetchItem(Cursor rows, Item item) {
		item.title = rows.getString(rows.getColumnIndex("title"));
		item.id = rows.getInt(rows.getColumnIndex("id"));
		item.played = rows.getInt(rows.getColumnIndex("played"));
		item.position = rows.getInt(rows.getColumnIndex("position"));
		item.mediaUrl = rows.getString(rows.getColumnIndex("mediaUrl"));
		item.link = rows.getString(rows.getColumnIndex("link"));
		item.description = rows.getString(rows.getColumnIndex("description"));
		return item;
	}

	/**
	 * return true if episode has audio file
	 * 
	 * @param number
	 * @return
	 */
	public static File episodeAudio(int number) {
		File file = new File(SDCARD_DIRECTORY_PATH + "podcast-" + number
				+ ".mp3");
		if (!file.exists()) {
			file = new File(SDCARD_DIRECTORY_PATH + "podcast-" + number
					+ ".ogg");
		}
		return file;
	}

	public File podcastCover(int number) {
		File file = new File(SDCARD_DIRECTORY_PATH + "podcast-" + number
				+ ".jpg");
		return file;
	}

	public File podcastContent(int number) {
		File file = new File(SDCARD_DIRECTORY_PATH + "podcast-" + number
				+ ".html");
		return file;
	}

	public static boolean checkSDCard() {
		File file = new File(SDCARD_DIRECTORY_PATH);
		if (!file.exists()) {
			return file.mkdir();
		}
		return true;
	}

	// sdcard

	private static class OpenHelper extends SQLiteOpenHelper {
		AssetManager as;

		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			as = context.getAssets();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				InputStream is = as.open("radiogeek.sql");
				byte b[] = new byte[is.available()];
				is.read(b);
				is.close();
				String q = new String(b);
				String o[] = q.trim().split(";\\r?\\n");
				db.beginTransaction();
				try {
					for (String string : o) {
						if (string.trim().equals(""))
							continue;
						db.execSQL(string);
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
				}
			} catch (IOException e) {
			}
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}

		@Override
		public synchronized void close() {
			super.close();
		}
	}
}
