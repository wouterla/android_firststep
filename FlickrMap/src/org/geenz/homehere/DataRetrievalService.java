/**
 * 
 */
package org.geenz.homehere;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;
import com.google.android.maps.GeoPoint;

/**
 * Demo Service used for data retrieval in the background.
 * 
 * Communication with the service should be completely through firing Intents, backcalls
 * are done using a broadcase/BroadcastReceiver combi.
 * 
 * This service definition does expose the Service class API through a Binder interface
 * for local usage, but this is for demonstration purposes only: this allows a client class
 * to use a bindService call to get the Service interface and use it. This usage is synchronous!
 * 
 * @author Wouter Lagerweij
 * 
 */
public class DataRetrievalService extends Service {

	// Please get your own... flickr is very generously handing them out.
	String apikey = "";
	String secret = "";
	
	// Constants defining out data formate in Intent communication
	// Each constant is a value used as key in a map for each photo found
	// The returned List of Maps is directly usable in a SimpleAdapter
    public static final String TITLE = "TITLE";
	public static final String INFO = "INFO";
	public static final String IMAGE = "IMAGE";
	public static final String FULL_URL = "FULL_URL";
	public static final String LATITUDE = "LATITUDE";
	public static final String LONGITUDE = "LONGITUDE";
	
	/** 
	 * Name of the action for an Intent used to update the location in this service, triggering a 
	 * new query to flickr, and a broadcast of the resulting photolist. 
	 */
	public static final String UPDATE_ACTION = "org.geenz.homehere.DataRetrievalService.UPDATE_ACTION";
	public static final String GEOPOINT = "GEOPOINT";
	
	private final IBinder binder = new DataRetrievalBinder();
	private GeoPoint gp = null;
	private PhotoList searchResult = null;

	DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Binder subclass intended solely to communicate this service's interface for local usage.
	 */
	public class DataRetrievalBinder extends Binder {
		DataRetrievalService getService() {
			return DataRetrievalService.this;
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		/*
		 * The onStart is called whenever a startService(intent) is called with
		 * this service's class as parameter.
		 * 
		 * We assume here that LATITUDE and LONGITUDE are always passed.
		 * Some checking would be appropriate:-)
		 */		
		Double lat = new Double(intent.getDoubleExtra(LATITUDE, 0) * 1e6);
		Double lon = new Double(intent.getDoubleExtra(LONGITUDE, 0) * 1e6);
		GeoPoint gp = new GeoPoint(lat.intValue(), lon.intValue());
		updateLocation(gp);
	}
	
	public void updateLocation(GeoPoint gp) {
		this.gp = gp;
		refreshDataSet();
	}

	private void refreshDataSet() {
		if (gp == null) {
			return;
		}
		
		String lat = Double.toString(((double) gp.getLatitudeE6()) / 1e6);
		String lon = Double.toString(((double) gp.getLongitudeE6()) / 1e6);
		int radius = 5;
		String radiusUnits = "km";

		SearchParameters params = new SearchParameters();
		params.setExtrasGeo(true);
		params.setExtrasDateTaken(true);
		params.setLatitude(lat);
		params.setLongitude(lon);
		params.setRadius(radius);
		params.setRadiusUnits(radiusUnits);

		try {
			params.setMinTakenDate(format.parse("2008-01-01"));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			Flickr flickr = new Flickr(apikey, secret, new REST());
			PhotosInterface photos = flickr.getPhotosInterface();
			searchResult = photos.search(params, 20, 0);
			
			// Create Intent, put query results in it, broadcast			
			Intent intent = new Intent(UPDATE_ACTION);
			intent.putExtra("data", filterSearchResult(searchResult));
			sendBroadcast(intent);
			
			// debug info
			for (Object o : searchResult) {
				Photo photo = (Photo) o;
				System.out.println(photo.getTitle() + ": " + format.format(photo.getDateTaken()));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	/**
	 * Convert the flickrj PhotoList to a SimpleAdapter suitable List of Maps
	 */
	private ArrayList<Map<String, String>> filterSearchResult(PhotoList searchResult) {
    	ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();
    	for (Object o : searchResult) {
    		Photo photo = (Photo) o;
    		HashMap<String, String> map = new HashMap<String, String>();
    		map.put(TITLE, photo.getTitle());
    		map.put(INFO, format.format(photo.getDateTaken()));
    		map.put(IMAGE, photo.getSmallSquareUrl());
			map.put(FULL_URL, photo.getUrl());
			map.put(LATITUDE, Float.toString(photo.getGeoData().getLatitude()));
			map.put(LONGITUDE, Float.toString(photo.getGeoData().getLongitude()));
    		data.add(map);
    	}
    	return data;
	}

	/**
	 * Called when using Service interface directly (and synchronously).
	 */
	public ArrayList<Map<String, String>> getData() {
		return filterSearchResult(searchResult);
	}
}
