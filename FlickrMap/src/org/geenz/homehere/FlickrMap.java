package org.geenz.homehere;

import java.util.ArrayList;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

/**
 * Demo class showing location of flickr images on mapview.
 * 
 * @author Wouter Lagerweij
 *
 */
public class FlickrMap extends MapActivity {

	MapView mapView = null;
	protected Location currentLocation;
	protected DataRetrievalService dataRetrievalService;
	protected FlickrOverlay flickrOverlay;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.map); // Load layout from xml
		
		mapView = (MapView) findViewById(R.id.mapview); // expose mapView for use in class
		mapView.displayZoomControls(true); // display zoom controls. Doesn't work very well in emulator
		mapView.getController().setZoom(20);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// register as a BroadcastReceiver for Intent action from our Service
        registerReceiver(receiver, new IntentFilter(DataRetrievalService.UPDATE_ACTION));
		activateLocationTracking();
	}
	
	/**
	 * Enable use of GPS location
	 */
	private void activateLocationTracking() {
		LocationManager mgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); // less accuracy is acceptable
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String providerName = mgr.getBestProvider(criteria, false); // but get best available
		
		// Register for location updates
		mgr.requestLocationUpdates(providerName, 10000, 200, locationListener);
		
		// initialize current location
		currentLocation = mgr.getLastKnownLocation(providerName);
		
		// initialize current location overlay
		MyLocationOverlay myOverlay = new MyLocationOverlay(this, mapView);
		myOverlay.enableMyLocation();
		
		// scroll map to currentLocation as soon as we have it
		myOverlay.runOnFirstFix(new Runnable() { public void run() { gotoCurrentLocation(); }});
		
		// initialize flickr overlay
		Drawable defaultMarker = getResources().getDrawable(android.R.drawable.ic_menu_mylocation);
		flickrOverlay = new FlickrOverlay(defaultMarker);
		mapView.getOverlays().add(flickrOverlay);
	}
	
	protected void gotoCurrentLocation() {
		mapView.getController().animateTo(getLocationAsGeoPoint());
		mapView.postInvalidate();
	}
	
	/**
	 * Do the damn conversion thing.
	 */
	protected GeoPoint getLocationAsGeoPoint() {
    	if (currentLocation != null) {
    		return new GeoPoint((int) (currentLocation.getLatitude() * 1e6),
    				(int) (currentLocation.getLongitude() * 1e6));
    	}
    	return null;
	}

	// Licencing issue
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			System.out.println("Location changed: " + location.toString());

			// Set current location, and center mapView on it
			currentLocation = location;
			gotoCurrentLocation();

			// get flickr updates by updating location in service
			Intent intent = new Intent(FlickrMap.this, DataRetrievalService.class);
			intent.putExtra(DataRetrievalService.LATITUDE, location.getLatitude());
			intent.putExtra(DataRetrievalService.LONGITUDE, location.getLongitude());
			startService(intent);
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
	
	/**
	 * Receive messages from our Service
	 */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
		@Override
    	public void onReceive(Context context, Intent intent) {
    		ArrayList<Map<String, String>> map 
    			= (ArrayList<Map<String, String>>) intent.getExtras().get("data");
    		flickrOverlay.setItems(map);
    		mapView.postInvalidate();
    	}	
    };

    @SuppressWarnings("unchecked")
	private class FlickrOverlay extends ItemizedOverlay {

    	ArrayList<Map<String, String>> items = null;
    	Drawable marker = null;
    	
    	public FlickrOverlay(Drawable defaultMarker) {
			super(defaultMarker);
			this.marker = defaultMarker;
		}
    	
    	public void setItems(ArrayList<Map<String, String>> items) {
    		this.items = items;
    		populate(); // @NOTE: Do not forget this call, or you won't see any items.
    	}
    	
    	public ArrayList<Map<String, String>> getItems() {
    		if (items == null) {
    			items = new ArrayList<Map<String, String>>();
    		}
    		return items;
    	}
    	    	
    	@Override
    	protected OverlayItem createItem(int i) {
    		Map<String, String> item = getItems().get(i);
    		
    		String title = item.get(DataRetrievalService.TITLE);
    		String info = item.get(DataRetrievalService.INFO);
    		String latStr = item.get(DataRetrievalService.LATITUDE);
    		String lonStr = item.get(DataRetrievalService.LONGITUDE);
    		
    		Double lat = new Float(latStr).doubleValue() * 1e6;
    		Double lon = new Float(lonStr).doubleValue() * 1e6;

    		GeoPoint gp = new GeoPoint(lat.intValue(), lon.intValue());

    		OverlayItem oi = new OverlayItem(gp, title, info);
    	
    		// This marker.setBounds is needed for the OverlayItem to draw correctly
    		// Leave it out, and nothing shows...
			marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
			oi.setMarker(marker);
			
    		return oi;
    	}
    	
    	/**
    	 * Show flick webpage for photo, when tapping item on map.
    	 */
    	@Override
    	protected boolean onTap(int i) {
    		Map<String, String> item = getItems().get(i);
        	String fullUrl = item.get(DataRetrievalService.FULL_URL);
        	if (fullUrl != null) {
    	    	Intent openWeb = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
    	    	startActivity(openWeb);
        	}
        	return true;
    	}
    	
    	/**
    	 * Only overridden to disable shadows. They're not pretty on 2D markers.
    	 */
    	@Override
    	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    		// get rid of irritating shadows
    		if (!shadow) {
    			super.draw(canvas, mapView, shadow);
    		}
    	}
    	
    	@Override
    	public int size() {
    		return getItems().size();
    	}    	
    };
}
