/*
 * Copyright (C) 2010 DSF Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.doublesunflower.android.lockcast;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.doublesunflower.android.lockcast.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import android.util.Log;
import android.widget.Toast;


public class GPSLoggerService extends Service {

	public static final String DATABASE_NAME = "GPSLOGGERDB";
	public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";
	public static final String TRIPS_TABLE_NAME = "TRIPS";

	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
	private final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	private LocationManager lm;
	private LocationListener locationListener;
	private SQLiteDatabase db;
	
	private static long minTimeMillis = 2000;
	private static long minDistanceMeters = 10;
	private static float minAccuracyMeters = 35;
	
	private int lastStatus = 0;
	private static boolean showingDebugToast = false;
	
	private static final String tag = "hengx";
	
    private ImageManager mImageManager;
    
    //private LolcatView mLolcatView;
    
    
    public static final int MILLION = 1000000;
    
    private int intHalfSpan = 10000;
	
	

	/** Called when the activity is first created. */
	private void startLoggerService() {

		// ---use the LocationManager class to obtain GPS locations---
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationListener = new MyLocationListener();

		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				minTimeMillis, 
				minDistanceMeters,
				locationListener);
		
		initDatabase();
		
		mImageManager = ImageManager.getInstance(this);		
		if (mImageManager == null) 			
			Toast.makeText(
				getBaseContext(),
				"mImageManager is NULL pointer!",
				Toast.LENGTH_SHORT).show();
		
		intHalfSpan = Integer.parseInt(ImageManager.halfspan);
	}
	
	private void shutdownLoggerService() {
		lm.removeUpdates(locationListener);
	}
	
	
	private void initDatabase() {
		db = this.openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
		db.execSQL("CREATE TABLE IF NOT EXISTS " +
				POINTS_TABLE_NAME + " (GMTTIMESTAMP VARCHAR, LATITUDE REAL, LONGITUDE REAL," +
						"ALTITUDE REAL, ACCURACY REAL, SPEED REAL, BEARING REAL);");
		db.close();
		Log.i(tag, "Database opened ok");
	}

	

	public class MyLocationListener implements LocationListener {
		
		public void onLocationChanged(Location loc) {
			
			if (loc != null) {
				
				boolean pointIsRecorded = false;
				try {
					if (loc.hasAccuracy() && loc.getAccuracy() <= minAccuracyMeters) {
						
						pointIsRecorded = true;
						GregorianCalendar greg = new GregorianCalendar();
						TimeZone tz = greg.getTimeZone();
						int offset = tz.getOffset(System.currentTimeMillis());
						greg.add(Calendar.SECOND, (offset/1000) * -1);
						StringBuffer queryBuf = new StringBuffer();
						
						queryBuf.append("INSERT INTO "+POINTS_TABLE_NAME+
								" (GMTTIMESTAMP,LATITUDE,LONGITUDE,ALTITUDE,ACCURACY,SPEED,BEARING) VALUES (" +
								"'"+timestampFormat.format(greg.getTime())+"',"+
								loc.getLatitude()+","+
								loc.getLongitude()+","+
								(loc.hasAltitude() ? loc.getAltitude() : "NULL")+","+
								(loc.hasAccuracy() ? loc.getAccuracy() : "NULL")+","+
								(loc.hasSpeed() ? loc.getSpeed() : "NULL")+","+
								(loc.hasBearing() ? loc.getBearing() : "NULL")+");");
						Log.i(tag, queryBuf.toString());
						
						db = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
						db.execSQL(queryBuf.toString());
					} 
				} catch (Exception e) {
					Log.e(tag, e.toString());
				} finally {
					if (db.isOpen())
						db.close();
				}
				
				if (pointIsRecorded) {
					if (showingDebugToast) 
						Toast.makeText(
							getBaseContext(),
							"Location stored: \nLat: " + sevenSigDigits.format(loc.getLatitude())
									+ " \nLon: " + sevenSigDigits.format(loc.getLongitude())
									+ " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
									+ " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
							Toast.LENGTH_SHORT).show();
				} else {
					if (showingDebugToast) 
						Toast.makeText(
							getBaseContext(),
							"Location not accurate enough: \nLat: " + sevenSigDigits.format(loc.getLatitude())
									+ " \nLon: " + sevenSigDigits.format(loc.getLongitude())
									+ " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?")
									+ " \nAcc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
							Toast.LENGTH_SHORT).show();
				}
				
				//startImage(loc.getLatitude(), loc.getLongitude());
				//loadSDImage(loc.getLatitude(), loc.getLongitude());     						
			}
		}

		public void onProviderDisabled(String provider) {
			if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderDisabled: " + provider,
					Toast.LENGTH_SHORT).show();

		}

		public void onProviderEnabled(String provider) {
			if (showingDebugToast) Toast.makeText(getBaseContext(), "onProviderEnabled: " + provider,
					Toast.LENGTH_SHORT).show();

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			String showStatus = null;
			if (status == LocationProvider.AVAILABLE)
				showStatus = "Available";
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				showStatus = "Temporarily Unavailable";
			if (status == LocationProvider.OUT_OF_SERVICE)
				showStatus = "Out of Service";
			if (status != lastStatus && showingDebugToast) {
				Toast.makeText(getBaseContext(),
						"new status: " + showStatus,
						Toast.LENGTH_SHORT).show();
			}
			lastStatus = status;
		}

	}
	
	public void startImage(double latitude, double longitude) {
		
		
		//Toast.makeText(getBaseContext(),
				//"hengx -- Lat: " + latitudeE6 + "; Long: " + longitudeE6,
				//Toast.LENGTH_SHORT).show();
	
        int zoom = 22;
        double double_latitudeE6 = latitude * MILLION;
        double double_longitudeE6 = longitude * MILLION;
        
        int latitudeE6 = (int)double_latitudeE6;
        int longitudeE6 = (int)double_longitudeE6;
        

        Intent i = new Intent(this, ImageList.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
        
        i.putExtra(ImageManager.ZOOM_EXTRA, zoom);
        i.putExtra(ImageManager.LATITUDE_E6_EXTRA, latitudeE6);
        i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, longitudeE6);
        
        
      //Toast.makeText(getBaseContext(),
		//	"hengx -- intHalfSpan: " + intHalfSpan,
			//Toast.LENGTH_SHORT).show();
        
        float minLong = ((float) (longitudeE6 - intHalfSpan)) / MILLION;
        float maxLong = ((float) (longitudeE6 + intHalfSpan)) / MILLION;

        float minLat = ((float) (latitudeE6 - intHalfSpan)) / MILLION;
        float maxLat = ((float) (latitudeE6 + intHalfSpan)) / MILLION;
        
        
        if (!mImageManager.isLoading()) {
        	mImageManager.clear();
        	
        	Toast.makeText(getBaseContext(),
					"hengx -- minLong: " + minLong + "; minLat: " + minLat,
					Toast.LENGTH_SHORT).show();
        	
        	mImageManager.load(minLong, maxLong, minLat, maxLat);
        	startActivity(i);
        }
        
       
	}
	
	public void loadSDImage(double latitude, double longitude) {
	
        int zoom = 22;
        double double_latitudeE6 = latitude * MILLION;
        double double_longitudeE6 = longitude * MILLION;
        int latitudeE6 = (int)double_latitudeE6;
        int longitudeE6 = (int)double_longitudeE6;
        
        Intent i = new Intent(this, LolcatActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
        
        i.putExtra(ImageManager.ZOOM_EXTRA, zoom);
        i.putExtra(ImageManager.LATITUDE_E6_EXTRA, latitudeE6);
        i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, longitudeE6);
        
        //float minLong = ((float) (longitudeE6 - intHalfSpan)) / MILLION;
        //float maxLong = ((float) (longitudeE6 + intHalfSpan)) / MILLION;

        //float minLat = ((float) (latitudeE6 - intHalfSpan)) / MILLION;
        //float maxLat = ((float) (latitudeE6 + intHalfSpan)) / MILLION;
        
        startActivity(i);
	}
	

	// Below is the service framework methods

	private NotificationManager mNM;


	public void onCreate() {
		super.onCreate();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// Add the map view to the frame
        //mMapView = new MapView(this, "Panoramio_DummyAPIKey");

		startLoggerService();
		showNotification();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		shutdownLoggerService();
		
		// Cancel the persistent notification.
		mNM.cancel(R.string.local_service_started);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped,
						Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.gpslogger16,
				text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, lockcast.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_name),
				text, contentIntent);

		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.local_service_started, notification);
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public static void setMinTimeMillis(long _minTimeMillis) {
		minTimeMillis = _minTimeMillis;
	}

	public static long getMinTimeMillis() {
		return minTimeMillis;
	}

	public static void setMinDistanceMeters(long _minDistanceMeters) {
		minDistanceMeters = _minDistanceMeters;
	}

	public static long getMinDistanceMeters() {
		return minDistanceMeters;
	}

	public static float getMinAccuracyMeters() {
		return minAccuracyMeters;
	}
	
	public static void setMinAccuracyMeters(float minAccuracyMeters) {
		GPSLoggerService.minAccuracyMeters = minAccuracyMeters;
	}

	public static void setShowingDebugToast(boolean showingDebugToast) {
		GPSLoggerService.showingDebugToast = showingDebugToast;
	}

	public static boolean isShowingDebugToast() {
		return showingDebugToast;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		GPSLoggerService getService() {
			return GPSLoggerService.this;
		}
	}

}
