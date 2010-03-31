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

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import android.widget.Toast;
import android.os.Handler;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity which displays the list of images.
 */
public class ImageList extends ListActivity {
    
    ImageManager mImageManager;
    ImageAdapter mImageAdapter;
    
    private Handler mHandler;
    
    private static final int MENU_STOP = Menu.FIRST + 1;
    
    
    private MyDataSetObserver mObserver = new MyDataSetObserver();

    /**
     * The zoom level the user chose when picking the search area
     */
    private int mZoom;

    /**
     * The latitude of the center of the search area chosen by the user
     */
    private int mLatitudeE6;

    /**
     * The longitude of the center of the search area chosen by the user
     */
    private int mLongitudeE6;

    private static final String tag = "hengx";
    
    
    /**
     * Observer used to turn the progress indicator off when the {@link ImageManager} is
     * done downloading.
     */
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (!mImageManager.isLoading()) {
                getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                        Window.PROGRESS_VISIBILITY_OFF);
            }
        }

        @Override
        public void onInvalidated() {
        }
    }
    
    public void onCreate(Bundle savedInstanceState) {
    	
    	//full screen window and no title bar
    	final Window win = getWindow(); 
    	win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
    	WindowManager.LayoutParams.FLAG_FULLSCREEN); 
    	requestWindowFeature(Window.FEATURE_NO_TITLE);  
        
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        
        //initialize ImageManager
        mImageManager = ImageManager.getInstance(this);
        
        mHandler = new Handler();
        
        //set up ListView
        ListView listView = getListView();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View footer = inflater.inflate(R.layout.list_footer, listView, false);
        listView.addFooterView(footer, null, false);
        
        //associate ListActivity with Adapter
        mImageAdapter = new ImageAdapter(this);
        setListAdapter(mImageAdapter);

        // Theme.Light sets a background on our list.
        listView.setBackgroundDrawable(null);
        
        //set up the progress bar and associate it with mImageManager downloading....
        if (mImageManager.isLoading()) {
            getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                    Window.PROGRESS_VISIBILITY_ON);
            mImageManager.addObserver(mObserver);
        }
        
        // Read the user's search area from the intent
        Intent i = getIntent();
        mZoom = i.getIntExtra(ImageManager.ZOOM_EXTRA, Integer.MIN_VALUE);
        mLatitudeE6 = i.getIntExtra(ImageManager.LATITUDE_E6_EXTRA, Integer.MIN_VALUE);
        mLongitudeE6 = i.getIntExtra(ImageManager.LONGITUDE_E6_EXTRA, Integer.MIN_VALUE);
        
        //start a new thread to load up the large image
        new LoadThread().start();
        
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop)
        .setIcon(android.R.drawable.ic_menu_view)
        .setAlphabeticShortcut('S');
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_STOP: {
        	stopService(new Intent(ImageList.this,
                    GPSLoggerService.class));
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
    }
    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PanoramioItem item = mImageManager.get(position);  
        
        //Toast.makeText(
			//	getBaseContext(),
				//"hengx: ImageList cursor position is " + position,
				//Toast.LENGTH_SHORT).show();
        
        // Create an intent to show a particular item.
        // Pass the user's search area along so the next activity can use it
        Intent i = new Intent(this, ViewImage.class);
        i.putExtra(ImageManager.PANORAMIO_ITEM_EXTRA, item);
        i.putExtra(ImageManager.ZOOM_EXTRA, mZoom);
        i.putExtra(ImageManager.LATITUDE_E6_EXTRA, mLatitudeE6);
        i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, mLongitudeE6);
        startActivity(i);
    }   
    
    
    /**
     * Utility to load a larger version of the image in a separate thread.
     *
     */
    private class LoadThread extends Thread {

        public LoadThread() {
        }

        @Override
        public void run() {
            try {
                   mHandler.postDelayed(
                		   
                		   new Runnable() {
                    public void run() {
                    	
                    	if (mImageManager.size() > 0)
                    	{
                    	/*
                    		PanoramioItem item = mImageManager.get(0);  
                        	Intent i = new Intent(ImageList.this, ViewImage.class);
                        	i.putExtra(ImageManager.PANORAMIO_ITEM_EXTRA, item);
                        	i.putExtra(ImageManager.ZOOM_EXTRA, mZoom);
                        	i.putExtra(ImageManager.LATITUDE_E6_EXTRA, mLatitudeE6);
                        	i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, mLongitudeE6);
                        	startActivity(i);
                    	 */
                    		
                    		PanoramioItem item = mImageManager.get(0);  
                            Intent i = new Intent(ImageList.this, LolcatActivity.class);
                            i.putExtra(ImageManager.PANORAMIO_ITEM_EXTRA, item);
                            i.putExtra(ImageManager.ZOOM_EXTRA, mZoom);
                            i.putExtra(ImageManager.LATITUDE_E6_EXTRA, mLatitudeE6);
                            i.putExtra(ImageManager.LONGITUDE_E6_EXTRA, mLongitudeE6);
                            startActivity(i);
                    		
                    	}
                    }
                }, 1000);
            } catch (Exception e) {
                Log.e(tag, e.toString());
            }
        }
    }
    
    
}