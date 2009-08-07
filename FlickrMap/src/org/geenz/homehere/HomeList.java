package org.geenz.homehere;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geenz.homehere.DataRetrievalService.DataRetrievalBinder;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

import com.google.android.maps.GeoPoint;

public class HomeList extends ListActivity {	

	private DataRetrievalService dataRetrievalService = null;
	private List<Map<String, String>> dataset = new ArrayList<Map<String, String>>();
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);	// Load layout from xml
    }

    /** Called when activity gets to the foreground */
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// bind *only* needed when using Service directly
        bindService(new Intent(this, DataRetrievalService.class), connection, Context.BIND_AUTO_CREATE);
        
        // Register broadcastReceiver to receive callbacks from Service
        registerReceiver(receiver, new IntentFilter(DataRetrievalService.UPDATE_ACTION));
    }

    private void setListAdapter() {
    	
        SimpleAdapter adapter = new SimpleAdapter(this, dataset,
        		
// Commented out version for text only list        		
//        		android.R.layout.two_line_list_item,  
//        		new String[] {DataRetrievalService.TITLE, DataRetrievalService.INFO}, 
//        		new int[] { android.R.id.text1, android.R.id.text2 }

// Active version for image+text list, w/ custom layout and custom ViewBinder        		
        		R.layout.image_list_item,
        		new String[] { DataRetrievalService.TITLE, DataRetrievalService.IMAGE },
        		new int[] { R.id.text, R.id.image }
        );
        adapter.setViewBinder(binder);
      
        setListAdapter(adapter);

        // Simple geocoding lookup to start with a location
		GeoPoint gp;
		try {
			Geocoder geoCoder = new Geocoder(getApplicationContext());
			Address addr = geoCoder.getFromLocationName("Damrak, Amsterdam, The Netherlands", 1).get(0);
			gp = new GeoPoint((new Double(addr.getLatitude() * 1e6)).intValue(), 
					(new Double(addr.getLongitude() * 1e6)).intValue());
			
			// direct (synchronous!) call to bound service, see FlickrMap class for proper use of service
			dataRetrievalService.updateLocation(gp);
		} catch (IOException e) {
			e.printStackTrace();
		}

    }
    
    @Override
    protected void onDestroy() {
    	unbindService(connection); // free service when we're not active 
    	super.onDestroy();
    }

// Dummy dataset...
    
//    private List<Map<String, String>> getData() {
//    	ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();
//    	for (int i = 0; i < 10; i++) {
//    		HashMap<String, String> map = new HashMap<String, String>();
//    		map.put(TITLE, "This is item nr. " + i);
//    		map.put(INFO, "More info on nr. " + i);
//    		data.add(map);
//    	}
//    	return data;
//    }
    
    /**
     * Open flickr url in browser on click.
     */
    @SuppressWarnings("unchecked")
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	// Get item clicked
    	Map<String, String> item = (Map<String, String>) getListView().getItemAtPosition(position);
    	
    	// Get url
    	String fullUrl = item.get(DataRetrievalService.FULL_URL);
    	if (fullUrl != null) {
    		
    		// Open url
	    	Intent openWeb = new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl));
	    	startActivity(openWeb);
    	}
    }
    
    // Service connection used when binding to a service.
    private ServiceConnection connection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		dataRetrievalService = ((DataRetrievalBinder) service).getService();
    		setListAdapter();
    	}

    	public void onServiceDisconnected(ComponentName name) {
    		dataRetrievalService = null;
    	}
    };
    
    /**
     * BroadcaseReceiver, receive updates from service, update adapter dataset.
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
		@Override
    	public void onReceive(Context context, Intent intent) {
    		ArrayList<Map<String, String>> map 
    			= (ArrayList<Map<String, String>>) intent.getExtras().get("data");
    		dataset.clear();
    		dataset.addAll(map);
    		((SimpleAdapter) getListAdapter()).notifyDataSetChanged();
    	}	
    };
    
    /**
     * Custom image ViewBinder.
     * 
     * Note complete lack of caching... And hogging of UI thread.
     */
	private class ImageViewBinder implements ViewBinder {
		public boolean setViewValue(View view, Object data,
				String textRepresentation) {
			if (view instanceof ImageView) {
				try {
					URL url = new URL((String) data);
					System.out.println("Retrieving: " + url.toExternalForm());
					InputStream is = url.openStream();
					Bitmap bm = BitmapFactory.decodeStream(is);
					((ImageView) view).setImageBitmap(bm);
					return true;
				} catch (IOException e) {
					// @TODO: Need better error handling
					e.printStackTrace();
				}
			}
			return false;
		}
	}
	private ImageViewBinder binder = new ImageViewBinder();
}