package com.dratva.hrss;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

/**
 * 
 * @author dratva
 *
 * @brief Dieser Service lädt jede Minute die Nachrichten aus https://news.ycombinator.com/rss herunter, parst diese
 * als XML und speichert die geparsten Nachrichten (Titel, Beschreibung und Link) in einer Liste. 
 * Mit der Methode getHRSSList kann man auf diese Liste zugreifen.
 * 
 */
public class HRSSService extends Service {

    private final IBinder binder = new LocalBinder();
    private Timer timer;
    private final List<HRSSEntry> hrssList = Collections.synchronizedList(new ArrayList<HRSSEntry>() );

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Toast.makeText(this, "HRSS service unbinded", Toast.LENGTH_SHORT).show();
		timer.cancel();
		return super.onUnbind(intent);
	}
	
    public class LocalBinder extends Binder {
    	HRSSService getService() {
            return HRSSService.this;
        }
    }
    
    public List<HRSSEntry> getHRSSList() {
    	synchronized(this) {
    		ArrayList<HRSSEntry> result = new ArrayList<HRSSEntry>(hrssList.size() );
    		for(HRSSEntry entry : hrssList) {
    			result.add(entry);
    		}
    		return result;
    	}
    }

    @Override
    public IBinder onBind(Intent intent) {
		Toast.makeText(this, "HRSS service started", Toast.LENGTH_SHORT).show();
		
		timer = new Timer();
		timer.schedule(new DownloadingAndParsingTask(), 1, 60000);
		
        return binder;
    }
	
    /**
     * 
     * @author dratva
     *
     * @breif Beinhaltet eine einzelne Nachricht 
     *
     */
	public class HRSSEntry {
		public String title, description, link;
		
		public HRSSEntry(String _t, String _d, String _l) {
			title = _t;
			description = _d;
			link = _l;
		}

		@Override
		public String toString() {
			return title;
		}
	}
	
	class DownloadingAndParsingTask extends TimerTask {

		@Override
		public void run() {
			try {
				URL url = new URL("https://news.ycombinator.com/rss");
				HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	
				if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
					InputStream is = conn.getInputStream();
	
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
	
					Document document = db.parse(is);
					conn.disconnect();
					Element element = document.getDocumentElement();
	
					NodeList nodeList = element.getElementsByTagName("item");
	
					if (nodeList.getLength() > 0) {
						synchronized(this) {
							hrssList.clear();
							for (int i = 0; i < nodeList.getLength(); i++) {
								Element entry = (Element) nodeList.item(i);
								Element titleE = (Element) entry.getElementsByTagName("title").item(0);
								Element descriptionE = (Element) entry.getElementsByTagName("description").item(0);
								Element linkE = (Element) entry.getElementsByTagName("link").item(0);
		
								titleE.normalize();
								String title = titleE.getFirstChild().getNodeValue();
								descriptionE.normalize();
								String description = descriptionE.getFirstChild().getNodeValue();
								linkE.normalize();
								String link = linkE.getFirstChild().getNodeValue();
								
								HRSSEntry newEntry = new HRSSEntry(title, description, link);
								hrssList.add(newEntry);
							}
						}
					}
				}
			} catch(Exception e) {
				//ignore connection failure
			}
		}
	}
}

