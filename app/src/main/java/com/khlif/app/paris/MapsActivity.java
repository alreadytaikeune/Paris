package com.khlif.app.paris;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private SparseArray<Object> mTasks;
    private int mLastTaskId;
    private Boolean mMonumentsPlotted;
    private ArrayList<Monument> mMonumentsList;
    private HashMap<String, Marker> mMarkers;
    private HashMap<Marker, Boolean> mMarkerClicked;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        int version = p.getInt("monument_version", -1);

        Log.d("Monument", "Current database version for monuments is " + version);
        mLastTaskId = 0;
        mTasks = new SparseArray<>();
        mMarkers = new HashMap<>();
        mMonumentsList = new ArrayList<>();
        mMarkerClicked = new HashMap<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMonumentsPlotted = false;
        downloadMonuments();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        LatLng paris = new LatLng(48.8594854, 2.3454267);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(paris, 13.0f));
        plotMonuments();
    }

    public void plotMonuments() {
        if (mMonumentsList == null || mMonumentsList.size() == 0) {
            return;
        }
        if (mMonumentsPlotted)
            return;
        Log.d("Monument", "plotting monuments");
        for (Monument m : mMonumentsList) {
            Marker marker = mMap.addMarker(new MarkerOptions().position(m.getLatLng())
                    .title(m.getName()));
            mMarkers.put(m.getName(), marker);
            mMarkerClicked.put(marker, false);
        }

        mMonumentsPlotted = true;
    }

    public void onConnectionError(){
        Toast.makeText(this, "La connexion au serveur a échoué", Toast.LENGTH_LONG);
    }

    public void onMonumentSuccess(){
        Monument.stashVersion(this);
    }

    public void getMonumentsFromDb(){
        if(Commons.Debug > 1){
            Log.d("Monument"+Commons.Debug, "Trying to read monuments from database");
        }
        try{
            mMonumentsList = Monument.getMonumentsAsList();
        }catch(Monument.DatabaseStillLoadingException e){
            // TODO: add callback and sleep a little?
        }catch(Monument.DatabaseNotLoadedException e1){
            Monument.initAndReadMonumentDatabase(this);
        }catch(Monument.DatabaseLoadException e2){
            // TODO: shit that sucks, gotta play it tight
        }
    }

    public void onThumbnailDownloaded(Monument m, Bitmap b){
        m.saveThumbNail(this, b);
    }

    public void downloadMonuments() {
        RequestCallback<String> c = new RequestCallback<String>() {
            int rstatus;
            @Override
            public void setResponse(String o, int status, int task) {
                mTasks.put(task, o);
                rstatus = status;
            }

            @Override
            public void done(int task) {
                Log.d("Monument", "task " + task + " is done");
                if(rstatus < 0){
                    onConnectionError();
                }
                if(rstatus==1){ // There was no need to download
                    getMonumentsFromDb();
                }
                String s = (String) mTasks.get(task);
                if (s == null) {
                    Log.d("Monument", "no entry for task " + task);
                    return;
                }
                mTasks.remove(task);
                mMonumentsList = Monument.parseMonuments(s);
                onMonumentSuccess();
                Log.d("Monument", "Monuments correctly parsed, size is " + mMonumentsList.size());
                //plotMonuments();
                getMarkersBitmaps();
            }
        };

        mLastTaskId++;
        ApiRequester a = new ApiRequester(c, mLastTaskId);
        a.execute(Commons.all_monuments + "?" + Commons.format);
    }


    public void getMarkersBitmaps() {
        RequestCallback<Bitmap> c = new RequestCallback<Bitmap>() {
            Bitmap bmp = null;
            int rstatus;
            @Override
            public void setResponse(Bitmap o, int status, int task) {
                rstatus = status;
                if (status >= 0) {
                    bmp = o;
                } else {
                    //TODO: something with the error
                }
            }

            @Override
            public void done(int task) {
                Monument m = (Monument) mTasks.get(task);
                setMarkerBitmap(bmp, m);

                if(rstatus > 1){ // The bitmap has been downloaded
                    onThumbnailDownloaded(m, bmp);
                }
            }
        };

        for (Monument m : mMonumentsList) {
            if (m.getImageUrl() != null) {
                int id = mLastTaskId;
                mTasks.put(id, m);
                mLastTaskId++;
                m.getBitmap(this, c, id);
            }
        }
    }

    public void setMarkerBitmap(Bitmap b, Monument mon) {
        MarkerOptions op = new MarkerOptions();
        op.position(mon.getLatLng());
        op.title(mon.getName());

        if(b != null){
            int w = 220;
            int h = 220;
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bmp = Bitmap.createBitmap(w, h, conf);

            float orgSize = 400;
            float r = w/orgSize;

            float centerDiameter = 224*r;
            float topOffset = 28*r;

            float leftOffset = w/2-centerDiameter/2+1;

            // It is maybe more efficient to decode only once the place marker
            // and make a copy for the subsequent uses.
            Bitmap marker = BitmapManager.decodeFile(R.drawable.marker, w, h, this);
            Rect src = new Rect(0,0,marker.getWidth()-1, marker.getHeight()-1);
            Rect dest = new Rect(0,0,w-1, h-1);

            Canvas canvas1 = new Canvas(bmp);

            Paint color = new Paint();
            color.setTextSize(35);
            color.setColor(Color.BLACK);
            b = BitmapManager.getCroppedBitmap(b, (int) centerDiameter);
            canvas1.drawBitmap(marker, src, dest, color);
            canvas1.drawBitmap(b, leftOffset, topOffset, color);
            op.icon(BitmapDescriptorFactory.fromBitmap(bmp));
        }


        Marker m = mMap.addMarker(op);
        mMarkers.put(mon.getName(), m);
        mMarkerClicked.put(m, false);
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.khlif.app.paris/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.khlif.app.paris/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.d("Monument", "marker clicked");
        for(Marker m : mMarkerClicked.keySet()){
            if(marker.equals(m)){
                Log.d("Monument", "found monument clicked, value is " + mMarkerClicked.get(m));
                // Marker has been clicked twice, launch activity
                if(mMarkerClicked.get(m)){
                    mMarkerClicked.put(m, false);
                    Intent intent = new Intent(this, ScrollingActivity.class);
                    startActivity(intent);
                }
                else{
                    mMarkerClicked.put(m, true);
                }
            }

            // If marker has been clicked before
            else if(mMarkerClicked.get(m)){
                mMarkerClicked.put(m, false);
            }
        }
        return false;
    }
}
