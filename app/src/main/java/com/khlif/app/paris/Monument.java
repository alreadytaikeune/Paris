package com.khlif.app.paris;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by anis on 08/05/16.
 */
public class Monument {

    private String mName;
    private LatLng mPos;
    private String mAddress;
    private String mUrl;
    private String mImageUrl;
    private int mId;
    private static String mCheckSum;

    private static SQLiteDatabase mDbReader;
    private static SQLiteDatabase mDbWriter;
    private static MonumentReaderDbHelper mDbHelper;


    private enum DatabaseStatus {NOT_LOADED, LOADING, AVAILABLE, ERROR};
    private static DatabaseStatus mDbStatus = DatabaseStatus.NOT_LOADED;

    private String mThumbnailPath;
    private String mLandscapePath;

    private static int new_version = 0;

    private static int VERSION = 0;

    private static ArrayList<Monument> mMonumentsList;

    public static abstract class MonumentEntry implements BaseColumns {
        public static final String TABLE_NAME = "monuments";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_ADDRESS = "address";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_IMAGE_URL = "image_url";
        public static final String COLUMN_THUMBNAIL_PATH = "thumbnail";
        public static final String COLUMN_LANDSCAPE_PATH = "landscape";
        private static final String TEXT_TYPE = " TEXT";
        private static final String INT_TYPE = " INTEGER";
        private static final String REAL_TYPE = " REAL";
        private static final String COMMA_SEP = ",";
        private static final String UNIQUE = " UNIQUE";

        private static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + MonumentEntry.TABLE_NAME + " (" +
                        MonumentEntry._ID + " INTEGER PRIMARY KEY," +
                        MonumentEntry.COLUMN_NAME + TEXT_TYPE + UNIQUE+ COMMA_SEP +
                        MonumentEntry.COLUMN_LATITUDE + REAL_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_LONGITUDE + REAL_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_ADDRESS + TEXT_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_URL + TEXT_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_IMAGE_URL + TEXT_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_THUMBNAIL_PATH + TEXT_TYPE + COMMA_SEP +
                        MonumentEntry.COLUMN_LANDSCAPE_PATH + TEXT_TYPE + COMMA_SEP +
                        " )";
    }


    public static class MonumentReaderDbHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 0;
        public static final String DATABASE_NAME = "event.db";

        public MonumentReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public MonumentReaderDbHelper(Context context, int version) {
            super(context, DATABASE_NAME, null, version);
        }

        public void onCreate(SQLiteDatabase db) {
            if(Commons.Debug > 1){
                Log.d("Monument"+Commons.Debug, "Creating the SQL database");
            }
            db.execSQL(MonumentEntry.SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //TODO: drop old database and save new
            if(Commons.Debug > 1){
                Log.d("Monument"+Commons.Debug, "upgrading database from " + oldVersion + " to " + newVersion);
            }
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
            onCreate(db);
        }
    }

    public static void initAndReadMonumentDatabase(Context context){
        if(Commons.Debug > 1){
            Log.d("Monument"+Commons.Debug, "init and read database");
        }
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);

        VERSION = p.getInt("monument_version", -1);
        mDbHelper = new MonumentReaderDbHelper(context, VERSION);
        AsyncTask<MonumentReaderDbHelper, Void, Integer> retrieveDBs = new AsyncTask<MonumentReaderDbHelper, Void, Integer>(){

            @Override
            protected void onPreExecute(){
                mDbStatus = DatabaseStatus.LOADING;
            }

            @Override
            protected Integer doInBackground(MonumentReaderDbHelper... params) {
                MonumentReaderDbHelper helper = params[0];
                if(helper==null){
                    return -1;
                }
                mDbReader = helper.getReadableDatabase();
                mDbWriter = helper.getWritableDatabase();
                if(mDbReader == null || mDbWriter == null)
                    return -1;
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if(result == 0){
                    if(Commons.Debug > 1){
                        Log.d("Event"+Commons.Debug, "database is now available");
                    }

                    mDbStatus = DatabaseStatus.AVAILABLE;
                }
                else{
                    mDbStatus = DatabaseStatus.ERROR;
                }
            }

        };
        retrieveDBs.execute(mDbHelper);
    }

    public static class DatabaseStillLoadingException extends Exception{
        public DatabaseStillLoadingException(){

        }
    }

    public static class DatabaseNotLoadedException extends Exception{
        public DatabaseNotLoadedException(){

        }
    }

    public static class DatabaseLoadException extends Exception{
        public DatabaseLoadException(){

        }
    }

    public Monument(int id, String n, double lat, double lng, String ad, String u, String image){
        mName = n;
        mPos = new LatLng(lat, lng);
        mAddress = ad;
        mUrl = u;
        mImageUrl = image;
        mId = id;
        mThumbnailPath="";
        mLandscapePath="";
        mMonumentsList = null;
    }

    public Monument(int id, String n, double lat, double lng, String ad, String u, String image,
                    String th_path, String l_path){
        mName = n;
        mPos = new LatLng(lat, lng);
        mAddress = ad;
        mUrl = u;
        mImageUrl = image;
        mId = id;
        mThumbnailPath=th_path;
        mLandscapePath=l_path;
        mMonumentsList = null;
    }

    public static void stashVersion(Context c){
        if(new_version > 0){
            setVersion(c, new_version);
        }
    }

    public static void setVersion(Context c, int v){

        VERSION = v;
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        p.edit().putInt("monument_version", VERSION).apply();
    }


    public LatLng getLatLng(){
        return mPos;
    }

    public String getName(){
        return mName;
    }

    public String getAddress(){
        return mAddress;
    }

    public String getUrl(){
        return mUrl;
    }

    public int getId() {
        return mId;
    }

    public String getLandscapePath(){
        return mLandscapePath;
    }

    public String getThumbnailPath(){
        return mThumbnailPath;
    }


    // Asynchronously download a bitmap if needed
    // otherwise read internal storage, transmit it to
    // the callback
    public void getBitmap(Context ctx, RequestCallback c, int tid){
        if(getThumbnailPath().equals("")){
            BitmapDownloader a = new BitmapDownloader(c, tid);
            a.execute(Commons.server + getImageUrl(), "200", "200");
        }
        else {
            Bitmap b = BitmapManager.decodeFile(new File(ctx.getFilesDir(),
                    getThumbnailPath()).getPath(), 200, 200);

            c.setResponse(b, 1, tid);
            c.done(tid);
        }
    }

    public String getImageUrl(){
        return mImageUrl;
    }

    static public ArrayList<Monument> parseMonuments(String m){
        ArrayList<Monument> out = new ArrayList<>();
        try{
            JSONArray j = new JSONObject(m).getJSONArray("objects");
            for(int k=0; k<j.length(); k++){
                JSONObject f = j.getJSONObject(k);
                String n = f.getString("name");
                double lat = f.getDouble("latitude");
                double lng = f.getDouble("longitude");
                String ad = f.getString("address");
                String u = f.getString("url_info");
                String image = f.getString("image");
                int id = f.getInt("id");
                if(image.equals("null"))
                    image = null;
                out.add(new Monument(id, n, lat, lng, ad, u, image));
            }
        } catch(JSONException j){
            Log.d("Monument", "error parsing monuments");
            return null;
        } finally{
            return out;
        }
    }

    public static ArrayList<Monument> getMonumentsAsList() throws DatabaseStillLoadingException,
            DatabaseNotLoadedException, DatabaseLoadException{
        if(mMonumentsList != null){
            return mMonumentsList;
        }
        else{
            if(mDbStatus != DatabaseStatus.AVAILABLE){
                if(mDbStatus != DatabaseStatus.LOADING){
                    throw new DatabaseStillLoadingException();
                }
                else{
                    throw new DatabaseNotLoadedException();
                }
            }
            else{
                loadMonumentsFromDatabase();
            }
        }
        return mMonumentsList;
    }


    public static void loadMonumentsFromDatabase() throws DatabaseStillLoadingException,
            DatabaseNotLoadedException, DatabaseLoadException{

        if(mDbStatus != DatabaseStatus.AVAILABLE){
            if(mDbStatus == DatabaseStatus.LOADING)
                throw new DatabaseStillLoadingException();
            else if(mDbStatus == DatabaseStatus.NOT_LOADED)
                throw new DatabaseNotLoadedException();
        }

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            MonumentEntry._ID,
            MonumentEntry.COLUMN_NAME,
            MonumentEntry.COLUMN_LATITUDE,
            MonumentEntry.COLUMN_LONGITUDE,
            MonumentEntry.COLUMN_ADDRESS,
            MonumentEntry.COLUMN_URL,
            MonumentEntry.COLUMN_IMAGE_URL,
            MonumentEntry.COLUMN_THUMBNAIL_PATH,
            MonumentEntry.COLUMN_LANDSCAPE_PATH
        };
        Cursor c = mDbReader.query(
                MonumentEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        if(!c.moveToFirst()){
            throw new DatabaseLoadException();
        }
        do{
            int id = c.getInt(c.getColumnIndexOrThrow(MonumentEntry._ID));
            String name = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_NAME));
            float latitude = c.getFloat(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_LATITUDE));
            float longitude = c.getFloat(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_LONGITUDE));
            String address = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_ADDRESS));
            String url = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_URL));
            String image_url = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_IMAGE_URL));
            String th_path = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_THUMBNAIL_PATH));
            String l_path = c.getString(c.getColumnIndexOrThrow(MonumentEntry.COLUMN_LANDSCAPE_PATH));



            Monument m = new Monument(id, name, latitude, longitude, address,
                    url, image_url, th_path, l_path);
            mMonumentsList.add(m);
        }while(c.moveToNext());

    }

    public static long insertMonument(Monument m){
        if(mDbStatus != DatabaseStatus.AVAILABLE){
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(MonumentEntry._ID, m.getId());
        values.put(MonumentEntry.COLUMN_NAME, m.getName());
        values.put(MonumentEntry.COLUMN_LATITUDE, m.getLatLng().latitude);
        values.put(MonumentEntry.COLUMN_LONGITUDE,  m.getLatLng().longitude);
        values.put(MonumentEntry.COLUMN_ADDRESS, m.getAddress());
        values.put(MonumentEntry.COLUMN_URL, m.getUrl());
        values.put(MonumentEntry.COLUMN_IMAGE_URL, m.getImageUrl());
        values.put(MonumentEntry.COLUMN_THUMBNAIL_PATH, "");
        values.put(MonumentEntry.COLUMN_LANDSCAPE_PATH, "");

        long newRowId;
        newRowId = mDbWriter.insert(
                MonumentEntry.TABLE_NAME,
                null,
                values);
        return newRowId;
    }


    public long updateThumbnail(String th_path){
        if(mDbStatus != DatabaseStatus.AVAILABLE){
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(MonumentEntry._ID, getId());
        values.put(MonumentEntry.COLUMN_NAME, getName());
        values.put(MonumentEntry.COLUMN_LATITUDE, getLatLng().latitude);
        values.put(MonumentEntry.COLUMN_LONGITUDE,  getLatLng().longitude);
        values.put(MonumentEntry.COLUMN_ADDRESS, getAddress());
        values.put(MonumentEntry.COLUMN_URL, getUrl());
        values.put(MonumentEntry.COLUMN_IMAGE_URL, getImageUrl());
        values.put(MonumentEntry.COLUMN_THUMBNAIL_PATH, th_path);
        values.put(MonumentEntry.COLUMN_LANDSCAPE_PATH, getLandscapePath());

        long newRowId;
        newRowId = mDbWriter.update(
                MonumentEntry.TABLE_NAME,
                values,
                "_id=" + mId,
                null);
        return newRowId;
    }


    public void moveOldThumbnail(Context c){
        if(getThumbnailPath().equals("")){
            return;
        }
        String prevThumb = getThumbnailPath();
        renameFile(c, prevThumb, "tmp_"+prevThumb);
    }

    public void recoverOldThumbnail(Context c){
        if(getThumbnailPath().equals("")){
            return;
        }
        String prevThumb = getThumbnailPath();
        renameFile(c, "tmp_"+prevThumb, prevThumb);
    }

    public boolean deleteOldThumbnail(Context c){
        if(getThumbnailPath().equals("")){
            return true;
        }
        return c.deleteFile("tmp_"+getThumbnailPath());
    }

    // Renames a file stored in the application's private store.
    public static void renameFile(Context context, String originalFileName, String newFileName) {
        File originalFile = context.getFileStreamPath(originalFileName);
        File newFile = new File(originalFile.getParent(), newFileName);
        if (newFile.exists()) {
            Log.e("Monument", newFileName + " was already existing when trying to rename " +
                    originalFileName);
            context.deleteFile(newFileName);
        }
        originalFile.renameTo(newFile);
    }

    public void saveThumbNail(Context c, Bitmap bm){
        FileOutputStream fos;
        // Use the compress method on the Bitmap object to write image to the OutputStream

        // To recover in case the saving fails
        moveOldThumbnail(c);

        int filename = getImageUrl().hashCode();

        try {
            fos = c.openFileOutput(filename+".png", Context.MODE_PRIVATE);
            // Writing the bitmap to the output stream
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            updateThumbnail(filename+".png");

            deleteOldThumbnail(c);
        }
        catch (FileNotFoundException e) {
            recoverOldThumbnail(c);
            e.printStackTrace();
        }
        catch (IOException e) {
            recoverOldThumbnail(c);
            e.printStackTrace();
        }
    }

    public static void downloadMonuments(RequestCallback c, int tid){
        ApiRequester a = new ApiRequester(c, tid);
        a.execute(Commons.all_monuments + "?" + Commons.format);
    }

    public static void downloadMonumentsIfNeeded(final RequestCallback c, final int tid){
        RequestCallback<String> r = new RequestCallback<String>() {
            int version=-1;
            int rstatus;
            @Override
            public void setResponse(String o, int status, int task) {
                rstatus = status;
                if(status > 0){
                    try{
                        version = Integer.parseInt(o);
                    }catch(NumberFormatException e){

                    }
                }
            }

            @Override
            public void done(int task) {
                if(rstatus < 0){
                    c.setResponse("", Commons.CONNEXION_ERROR, tid);
                    c.done(tid);
                }
                if(version > VERSION){
                    if(Commons.Debug > 1){
                        Log.d("Monument"+Commons.Debug, "Database is outdated, downloading new...");
                    }
                    new_version = version;
                    downloadMonuments(c, tid);
                }
                else{
                    if(Commons.Debug > 1){
                        Log.d("Monument"+Commons.Debug, "Database is up to date");
                    }
                    c.setResponse("", 1, tid);
                    c.done(tid);
                }
            }
        };
        ApiRequester a = new ApiRequester(r, 0);
        a.execute(Commons.version);
    }

}
