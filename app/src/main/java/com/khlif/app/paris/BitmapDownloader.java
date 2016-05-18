package com.khlif.app.paris;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by anis on 11/05/16.
 */
public class BitmapDownloader extends AbstractRequester<String, Integer, Integer> {

    private Bitmap bitmap;

    public BitmapDownloader(RequestCallback c, int t){
        super(c, t);
    }


    @Override
    protected Integer doInBackground(String... data) {
        int status=-1;
        HttpURLConnection urlConnection=null;
        try {
            int width;
            int height;
            Log.d("Monument", "contacting url  " + data[0]);

            URL url = new URL(data[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            Bitmap non_resized = BitmapFactory.decodeStream(urlConnection.getInputStream());
            if(data.length >= 3){
                width = Integer.parseInt(data[1]);
                height = Integer.parseInt(data[2]);
                bitmap = BitmapManager.getResizedBitmap(non_resized, width, height);
                non_resized.recycle();
            }
            else{
                bitmap = non_resized;
            }
            status = urlConnection.getResponseCode();
        }
        catch(MalformedURLException e){
            status = -1;
        }
        catch(IOException e){
            status = -2;
            Log.e("Monument", "IOException");
            e.printStackTrace();
        }
        finally{
            if(urlConnection != null)
                urlConnection.disconnect();
        }
        callback.setResponse(bitmap, status, task);
        return status;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

    }

    @Override
    protected void onPostExecute(Integer result) {
        callback.done(task);
    }

}
