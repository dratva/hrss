package com.dratva.hrss;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;

/**
 * 
 * @author dratva
 *
 * 
 * Annahme: der Algorithmus geht davon aus, dass in der Nachrichtenliste die neueren Nachrichten "unten" sind
 * (haben höhere Indizes). Anscheinend ist diese Annahme falsch, aber die Nachrichten beinhalten kein Zeitstempel.
 * TODO: korrekter wäre, eine heruntergeladene Nachrichtenliste mit der vorherigen zu vergleichen, so die neuen
 * Nachrichten ermitteln und diese oben anhängen. Das wäre aber die Aufgabe für den HRSS-Service. So wie es jetzt
 * implementiert ist, sind die Nachrichten in der ListView einfach umgekehrt sortiert, als auf der web-Seite.
 * 
 * Algorithmus: jede Sekunde wird der HRSS-Service abgefragt und die Liste der sichtbaren Nachrichten erstellt. Die Liste beinhaltet
 * am Anfang 0 Nachrichten und jedes Mal wird die Anzahl inkrementiert, bis die Anzahl der Nachrichten, die vom
 * HRSS-Service ausgelesen wurden erreicht ist.
 * 
 * In der ListView werden die neueren Nachrichten oben und die älteren unten dargestellt. Das Laden nach
 * dem Start fängt mit älteren Nachrichten an.
 * 
 * TODO: Beim Drehen des Geräts wird die Activity neu erstellt, was dazu führt, dass die Nachrichten neu
 * geladen werden, also die Anzahl der sichtbaren Nachrichten auf 0 gesetzt und jede Sekunde inkrementiert
 * wird. Eigentlich soll beim Drehen das gleiche sofort dargestellt werden, was bisher sichtbar war.
 */
public class HRSSActivity extends Activity implements ServiceConnection, AdapterView.OnItemClickListener {

	List<HRSSService.HRSSEntry> listData;
	ListView hrssListView;
	HRSSService.LocalBinder binder = null;
    int visibleMessagesCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hrss);
		
		hrssListView = (ListView)findViewById(R.id.hrssListView);
		listData = new ArrayList<HRSSService.HRSSEntry>();
		ArrayAdapter<HRSSService.HRSSEntry> listAdapter = new ArrayAdapter<HRSSService.HRSSEntry>(this, android.R.layout.simple_list_item_1, listData);
		
		hrssListView.setAdapter(listAdapter);
		hrssListView.setOnItemClickListener(this);
		
		Timer timer = new Timer();
		timer.schedule(new AddingTask(listAdapter), 1, 1000);
		
		bindService(new Intent(this, HRSSService.class), this, BIND_AUTO_CREATE);
	}
	
	class AddingTask extends TimerTask {
		
		ArrayAdapter<HRSSService.HRSSEntry> adapter;
		Runnable notifier;
		ArrayList<HRSSService.HRSSEntry> newList = new ArrayList<HRSSService.HRSSEntry>(); 

		AddingTask(ArrayAdapter<HRSSService.HRSSEntry> _adapter) {
			adapter = _adapter;
			notifier = new Runnable() {
				public void run() {
					synchronized(this) {
						listData.clear();
						for(HRSSService.HRSSEntry item : newList) {
							listData.add(item);
						}
					}
					adapter.notifyDataSetChanged();
				}
			};
		}

		@Override
		public void run() {
			if(binder != null) {
				synchronized(this) {
					newList.clear();
					List<HRSSService.HRSSEntry> currentHRSSList = binder.getService().getHRSSList();
					if(currentHRSSList.size() > visibleMessagesCount) {
						++ visibleMessagesCount;
					} else {
						visibleMessagesCount = currentHRSSList.size();
					}
					for(int i = 0; i < visibleMessagesCount; ++i) {
						newList.add(0, currentHRSSList.get(i) );
					}
				}				
				hrssListView.post(notifier);
			}
		}	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_hrss, menu);
		return true;
	}

	@Override
	public void onServiceConnected(ComponentName arg0, IBinder arg1) {
		binder = (HRSSService.LocalBinder)arg1;
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		binder = null;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ArrayAdapter<HRSSService.HRSSEntry> adapter = (ArrayAdapter<HRSSService.HRSSEntry>)arg0.getAdapter();
		Intent browserIntent = new Intent(Intent.ACTION_VIEW);
		browserIntent.setData(Uri.parse(( (HRSSService.HRSSEntry)adapter.getItem(arg2) ).link) );
		startActivity(browserIntent);	
	}

}

