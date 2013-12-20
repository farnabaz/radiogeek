package com.farnabaz.android.radiogeek;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class RSSParser extends DefaultHandler {

	private URL _url;

	private String _lastBuildTime;

	public RSSParser() {

	}

	public void setUrl(URL url) {
		this._url = url;
	}

	public void SetLastBuildDate(String lbt) {
		this._lastBuildTime = lbt;
	}

	public void parse() {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.parse(new InputSource(_url.openConnection().getInputStream()));
		} catch (ParserConfigurationException e) {
			Log.e("RssParser", e.getMessage());
			items = null;
		} catch (SAXException e) {
			Log.e("RssParser", e.getMessage());
			items = null;
		} catch (IOException e) {
			Log.e("RssParser", e.getMessage());
			items = null;
		}
	}

	// booleans that check whether it's in a specific tag or not
	private boolean _isActive = true;

	// this holds the data
	private ArrayList<Item> items;

	private Item item;

	private String content;

	private String lastBuildDate;

	private void setUpdateTime(String lbt) {
		lastBuildDate = lbt;
		if (lastBuildDate.equals(_lastBuildTime)) {
			_isActive = false;
		}
	}

	/**
	 * Returns the data object
	 * 
	 * @return
	 */
	public ArrayList<Item> getRSS() {
		return items;
	}

	/**
	 * This gets called when the xml document is first opened
	 * 
	 * @throws SAXException
	 */
	@Override
	public void startDocument() throws SAXException {
		items = new ArrayList<Item>();
	}

	/**
	 * Called when it's finished handling the document
	 * 
	 * @throws SAXException
	 */
	@Override
	public void endDocument() throws SAXException {

	}

	/**
	 * This gets called at the start of an element. Here we're also setting the
	 * booleans to true if it's at that specific tag. (so we know where we are)
	 * 
	 * @param namespaceURI
	 * @param localName
	 * @param qName
	 * @param atts
	 * @throws SAXException
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (_isActive) {
			if (localName.equals(""))
				localName = qName;
			if (localName.equals("item")) {
				item = new Item();
			} else if (localName.equals("enclosure") && item.mediaUrl == null) {
				item.mediaUrl = atts.getValue("url");
			}
			content = "";
		} else {
			throw new SAXException("Break");
		}
	}

	/**
	 * Called at the end of the element. Setting the booleans to false, so we
	 * know that we've just left that tag.
	 * 
	 * @param namespaceURI
	 * @param localName
	 * @param qName
	 * @throws SAXException
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (_isActive) {
			if (localName.equals(""))
				localName = qName;
			if (localName.equals("item")) {
				items.add(item);
				Log.i("aa", item.mediaUrl);
			}
			if (localName.equals("lastBuildDate")) {
				setUpdateTime(content);
			}

			if (item == null)
				return;
			if (localName.equals("title")) {
				item.title = content;
			} else if (localName.equals("description")) {
				item.description = content;
			} else if (qName.equals("content:encoded")) {
				item.content = content;
			} else if (localName.equals("link")) {
				item.link = content;
			} else if (localName.equals("pubDate")) {
				item.setPubDate(content);
			}
		}
	}

	/**
	 * Calling when we're within an element. Here we're checking to see if there
	 * is any content in the tags that we're interested in and populating it in
	 * the Config object.
	 * 
	 * @param ch
	 * @param start
	 * @param length
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		if (_isActive) {
			String chars = new String(ch, start, length);
			chars = chars.trim();
			content += chars;
		}
	}

	public String getLastBuildDate() {
		return lastBuildDate;
	}

	public boolean hasItem() {
		return items != null && items.size() > 0;
	}

	public void stop() {
		_isActive = false;
	}
}
