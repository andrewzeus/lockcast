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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;
//import android.widget.EditText;
import android.widget.FrameLayout;
//import android.widget.RadioButton;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.doublesunflower.android.lockcast.GPSLoggerService;
import com.doublesunflower.android.lockcast.R;

//import com.admob.android.ads.AdView;
//import android.view.WindowManager;

/**
 * Activity which lets the user select a search area
 *
 */
public class lockcast extends MapActivity {
	
	
    private static final String tag = "Hengx";
	private static final String tripFileName = "currentTrip.txt";
	private String currentTripName = "";
	private int altitudeCorrectionMeters = 20;
	
	//private AdView example_adview;
	
	private String halfspan = "10000";
	
	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
	
    private MapView mMapView;
    private MyLocationOverlay mMyLocationOverlay;
    private ImageManager mImageManager;
    
    
    public static final int MILLION = 1000000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
    	
    	//full screen window and no title bar
    	final Window win = getWindow(); 
    	win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
    	WindowManager.LayoutParams.FLAG_FULLSCREEN); 
    	requestWindowFeature(Window.FEATURE_NO_TITLE);  
    	
        setContentView(R.layout.main);
        
        //example_adview = (AdView) findViewById(R.id.ad);
		//example_adview.setVisibility(AdView.VISIBLE);
        
        Button button = (Button)findViewById(R.id.ButtonStart);
        button.setOnClickListener(mStartListener);
        button = (Button)findViewById(R.id.ButtonStop);
        button.setOnClickListener(mStopListener);
        button = (Button)findViewById(R.id.ButtonExport);
		button.setOnClickListener(mExportListener);
		ToggleButton toggleDebug = (ToggleButton)findViewById(R.id.ToggleButtonDebug);
		toggleDebug.setOnClickListener(mToggleDebugListener);
		toggleDebug.setChecked(GPSLoggerService.isShowingDebugToast());
		
		EditText editAltitudeCorrection = (EditText)findViewById(R.id.EditTextHalfSpan);
		editAltitudeCorrection.setText(String.valueOf(halfspan));
        
        
        initTripName();
        
        mImageManager = ImageManager.getInstance(this);
        
        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
        Button goButton = (Button) findViewById(R.id.go);
        goButton.setOnClickListener(mSearchListener);
       
        // Add the map view to the frame
        mMapView = new MapView(this, "0FkguPkCBiXmIbMlVvKQ_r9kpr_KckXncK6FBpg");
        frame.addView(mMapView, 
                new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        // Create an overlay to show current location
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
            mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
        }});

        mMapView.getOverlays().add(mMyLocationOverlay);
        mMapView.getController().setZoom(15);
        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setSatellite(true);
        
        addZoomControls(frame);
        
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onStop() {
        mMyLocationOverlay.disableMyLocation();
        super.onStop();
    }
    
    
    private OnClickListener mStartListener = new OnClickListener() {
        public void onClick(View v)
        {
        	EditText editHalfSpan = (EditText)findViewById(R.id.EditTextHalfSpan);
			//halfspan = Integer.parseInt(editHalfSpan.getText().toString());
			halfspan = editHalfSpan.getText().toString();
        	
			Intent i = new Intent(lockcast.this,
                    GPSLoggerService.class);
	        i.putExtra(ImageManager.halfspan, halfspan);
	        
	        ImageManager.halfspan = halfspan;
        	
        	startService(i);       
            
        }
    };

    private OnClickListener mStopListener = new OnClickListener() {
        public void onClick(View v)
        {
            stopService(new Intent(lockcast.this,
                    GPSLoggerService.class));
        }
    };
    
    private OnClickListener mExportListener = new OnClickListener() {
    	public void onClick(View v) {
    		doExport();
    	}
    };
    
    private OnClickListener mToggleDebugListener = new OnClickListener() {
		public void onClick(View v) {
			boolean currentDebugState = GPSLoggerService.isShowingDebugToast();
			GPSLoggerService.setShowingDebugToast(!currentDebugState);
			ToggleButton toggleButton = (ToggleButton)findViewById(R.id.ToggleButtonDebug);
			toggleButton.setChecked(!currentDebugState);
		}
    };
    
    /**
     * Starts a new search when the user clicks the search button.
     */
    private OnClickListener mSearchListener = new OnClickListener() {
    public void onClick(View view) {
        // Get the search area
        int latHalfSpan = mMapView.getLatitudeSpan() >> 1;
        int longHalfSpan = mMapView.getLongitudeSpan() >> 1;
        
        // Remember how the map was displayed so we can show it the same way later
        GeoPoint center = mMapView.getMapCenter();
        int zoom = mMapView.getZoomLevel();
        int latitudeE6 = center.getLatitudeE6();
        int longitudeE6 = center.getLongitudeE6();

        Intent i = new Intent(lockcast.this, ImageList.class);
        i.putExtra(ImageManager.ZOOM_EXTRA, zoom);
        i.putExtra(ImageManager.LATITUDE_E6_EXTRA, latitudeE6);
        i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, longitudeE6);        

        float minLong = ((float) (longitudeE6 - longHalfSpan)) / MILLION;
        float maxLong = ((float) (longitudeE6 + longHalfSpan)) / MILLION;
        float minLat = ((float) (latitudeE6 - latHalfSpan)) / MILLION;
        float maxLat = ((float) (latitudeE6 + latHalfSpan)) / MILLION;

        mImageManager.clear();
        mImageManager.load(minLong, maxLong, minLat, maxLat);
        startActivity(i);
    }
    };
    

    /**
     * Add zoom controls to our frame layout
     */
    private void addZoomControls(FrameLayout frame) {
        // Get the zoom controls and add them to the bottom of the map
        View zoomControls = mMapView.getZoomControls();

        FrameLayout.LayoutParams p = 
            new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL);
        frame.addView(zoomControls, p);
    }
    
    
    private void initTripName() {
		// see if there's currently a trip in the trip file
    	String tripName = "new";
    	try {
	        FileInputStream fIn = openFileInput(tripFileName);
	        InputStreamReader isr = new InputStreamReader(fIn);
	        char[] inputBuffer = new char[1024];
	        isr.read(inputBuffer);
	        isr.close();
	        fIn.close();
	        tripName = new String(inputBuffer).trim();
	        Log.i(tag,"loaded trip name: "+tripName);
    	} catch (FileNotFoundException fnfe) {
    		Log.i(tag,"first run, no "+tripFileName);
    		try {
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMMdd");
	    		tripName = sdf.format(new Date());
	    		saveTripName(tripName);
    		} catch (Exception e) {
    			Log.e(tag, e.toString());
    		}
    	} catch (IOException ioe) {
    		Log.e(tag, ioe.toString());
    	}
    	//EditText tripNameEditor = (EditText)findViewById(R.id.EditTextTripName);
    	//tripNameEditor.setText(tripName);
    	currentTripName = tripName;
	}
    
    private void saveTripName(String tripName) throws FileNotFoundException, IOException {
        FileOutputStream fOut = openFileOutput(tripFileName,
                MODE_PRIVATE);
        OutputStreamWriter osw = new OutputStreamWriter(fOut); 
        osw.write(tripName);
        osw.flush();
        osw.close();
        fOut.close();
    }
    
    
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    
    
    private void doExport() {
		// export the db contents to a kml file
		SQLiteDatabase db = null;
		Cursor cursor = null;
		try {
			//EditText editAlt = (EditText)findViewById(R.id.EditTextAltitudeCorrection);
			//altitudeCorrectionMeters = Integer.parseInt(editAlt.getText().toString());
			
			altitudeCorrectionMeters = 20;
			Log.i(tag, "altitude Correction updated to "+altitudeCorrectionMeters);
			
			db = openOrCreateDatabase(GPSLoggerService.DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
			cursor = db.rawQuery("SELECT * " +
                    " FROM " + GPSLoggerService.POINTS_TABLE_NAME +
                    " ORDER BY GMTTIMESTAMP ASC",
                    null);
            
			int gmtTimestampColumnIndex = cursor.getColumnIndexOrThrow("GMTTIMESTAMP");
            int latitudeColumnIndex = cursor.getColumnIndexOrThrow("LATITUDE");
            int longitudeColumnIndex = cursor.getColumnIndexOrThrow("LONGITUDE");
            int altitudeColumnIndex = cursor.getColumnIndexOrThrow("ALTITUDE");
            int accuracyColumnIndex = cursor.getColumnIndexOrThrow("ACCURACY");
			
            if (cursor.moveToFirst()) {
				
            	StringBuffer fileBuf = new StringBuffer();
				String beginTimestamp = null;
				String endTimestamp = null;
				String gmtTimestamp = null;
				initFileBuf(fileBuf, initValuesMap());
				
				do {
					gmtTimestamp = cursor.getString(gmtTimestampColumnIndex);
					if (beginTimestamp == null) {
						beginTimestamp = gmtTimestamp;
					}
					double latitude = cursor.getDouble(latitudeColumnIndex);
					double longitude = cursor.getDouble(longitudeColumnIndex);
					double altitude = cursor.getDouble(altitudeColumnIndex) + altitudeCorrectionMeters;
					double accuracy = cursor.getDouble(accuracyColumnIndex);
					fileBuf.append(sevenSigDigits.format(longitude)+","+sevenSigDigits.format(latitude)+","+altitude+"\n");
				} while (cursor.moveToNext());
				
				endTimestamp = gmtTimestamp;
				closeFileBuf(fileBuf, beginTimestamp, endTimestamp);
				String fileContents = fileBuf.toString();
				Log.d(tag, fileContents);
				File sdDir = new File("/sdcard/GPSLogger");
				sdDir.mkdirs();
				File file = new File("/sdcard/GPSLogger/"+currentTripName+".kml");
				FileWriter sdWriter = new FileWriter(file, false);
				sdWriter.write(fileContents);
				sdWriter.close();
    			Toast.makeText(getBaseContext(),
    					"Export completed!",
    					Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getBaseContext(),
						"I didn't find any location points in the database, so no KML file was exported.",
						Toast.LENGTH_LONG).show();
			}
		} catch (FileNotFoundException fnfe) {
			Toast.makeText(getBaseContext(),
					"Error trying access the SD card.  Make sure your handset is not connected to a computer and the SD card is properly installed",
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(getBaseContext(),
					"Error trying to export: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			if (db != null && db.isOpen()) {
				db.close();
			}
		}
	}

	private HashMap initValuesMap() {
		HashMap valuesMap = new HashMap();

		valuesMap.put("FILENAME", currentTripName);
		
		//RadioButton airButton = (RadioButton)findViewById(R.id.RadioAir);
		//if (airButton.isChecked()) {
			// use air settings
			//valuesMap.put("EXTRUDE", "1");
			//valuesMap.put("TESSELLATE", "0");
			//valuesMap.put("ALTITUDEMODE", "absolute");
		//} else {
			// use ground settings for the export
			valuesMap.put("EXTRUDE", "0");
			valuesMap.put("TESSELLATE", "1");
			valuesMap.put("ALTITUDEMODE", "clampToGround");
		//}
		
		return valuesMap;
	}

	private void initFileBuf(StringBuffer fileBuf, HashMap valuesMap) {
		fileBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		fileBuf.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
		fileBuf.append("  <Document>\n");
		fileBuf.append("    <name>"+valuesMap.get("FILENAME")+"</name>\n");
		fileBuf.append("    <description>GPSLogger KML export</description>\n");
		fileBuf.append("    <Style id=\"yellowLineGreenPoly\">\n");
		fileBuf.append("      <LineStyle>\n");
		fileBuf.append("        <color>7f00ffff</color>\n");
		fileBuf.append("        <width>4</width>\n");
		fileBuf.append("      </LineStyle>\n");
		fileBuf.append("      <PolyStyle>\n");
		fileBuf.append("        <color>7f00ff00</color>\n");
		fileBuf.append("      </PolyStyle>\n");
		fileBuf.append("    </Style>\n");
		fileBuf.append("    <Placemark>\n");
		fileBuf.append("      <name>Absolute Extruded</name>\n");
		fileBuf.append("      <description>Transparent green wall with yellow points</description>\n");
		fileBuf.append("      <styleUrl>#yellowLineGreenPoly</styleUrl>\n");
		fileBuf.append("      <LineString>\n");
		fileBuf.append("        <extrude>"+valuesMap.get("EXTRUDE")+"</extrude>\n");
		fileBuf.append("        <tessellate>"+valuesMap.get("TESSELLATE")+"</tessellate>\n");
		fileBuf.append("        <altitudeMode>"+valuesMap.get("ALTITUDEMODE")+"</altitudeMode>\n");
		fileBuf.append("        <coordinates>\n");
	}
	
	private void closeFileBuf(StringBuffer fileBuf, String beginTimestamp, String endTimestamp) {
		fileBuf.append("        </coordinates>\n");
		fileBuf.append("     </LineString>\n");
		fileBuf.append("	 <TimeSpan>\n");
		String formattedBeginTimestamp = zuluFormat(beginTimestamp);
		fileBuf.append("		<begin>"+formattedBeginTimestamp+"</begin>\n");
		String formattedEndTimestamp = zuluFormat(endTimestamp);
		fileBuf.append("		<end>"+formattedEndTimestamp+"</end>\n");
		fileBuf.append("	 </TimeSpan>\n");
		fileBuf.append("    </Placemark>\n");
		fileBuf.append("  </Document>\n");
		fileBuf.append("</kml>");
	}

	private String zuluFormat(String beginTimestamp) {
		// turn 20081215135500 into 2008-12-15T13:55:00Z
		StringBuffer buf = new StringBuffer(beginTimestamp);
		buf.insert(4, '-');
		buf.insert(7, '-');
		buf.insert(10, 'T');
		buf.insert(13, ':');
		buf.insert(16, ':');
		buf.append('Z');
		return buf.toString();
	}

	public void setAltitudeCorrectionMeters(int altitudeCorrectionMeters) {
		this.altitudeCorrectionMeters = altitudeCorrectionMeters;
	}

	public int getAltitudeCorrectionMeters() {
		return altitudeCorrectionMeters;
	}
    
    
    
}
