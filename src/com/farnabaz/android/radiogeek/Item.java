package com.farnabaz.android.radiogeek;

import android.annotation.SuppressLint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Item {
	public static final int ITEM_NOT_PLAYED = 0;
	public static final int ITEM_PARTIALY_PLAYED = 1;
	public static final int ITEM_COMPLETELY_PLAYED = 2;

	/**
	 * Date formatter that convert string to Date
	 */
	@SuppressLint("SimpleDateFormat")
	private static SimpleDateFormat FORMATTER = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss Z");

	/**
	 * Item id
	 */
	public int id = -1;

	/**
	 * Item title
	 */
	public String title;

	/**
	 * Item description
	 */
	public String description;

	/**
	 * Item link
	 */
	public String link;

	/**
	 * Item guid
	 */
	public String content;

	/**
	 * Item image url
	 */
	public String imageUrl;

	/**
	 * Item media url
	 */
	public String mediaUrl;

	/**
	 * publish Date
	 */
	private Date pubDate;

	public int position;
	public int played;

	/**
	 * Set publish date with String
	 * 
	 * @param aDate
	 */
	public void setPubDate(String aDate) {
		// pad the date if necessary
		while (!aDate.endsWith("00")) {
			aDate += "0";
		}
		try {
			this.pubDate = FORMATTER.parse(aDate.trim());
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * set publish date with a Date
	 * 
	 * @param aDate
	 */
	public void setPubDate(Date aDate) {
		this.pubDate = aDate;
	}

	/**
	 * return publish date
	 * 
	 * @return
	 */
	public Date getPubDate() {
		return pubDate;
	}

	public int getId() {
		if (id != -1) {
			return id;
		}
		if (mediaUrl != null) {
			String regex = "(.*)_(\\d*)_.*";

			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(mediaUrl);

			if (matcher.find()) {
				id = Integer.parseInt(matcher.group(2));
			}
		}

		return id;
	}

	public String getUrl() {
		return mediaUrl.replace("http://jadi.net",
				DataHandler.ANTIFILTER_BASE_URL);
	}

	public String getPodcastLink() {
		return link.replace("http://jadi.net", "http://jadi2.undo.it");
	}
}
