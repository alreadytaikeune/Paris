package com.khlif.app.paris;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.CookieManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by anis on 07/05/16.
 */
public class ApiRequester extends AbstractRequester<String, Void, Integer> {

    private CookieManager cookieManager;


    public ApiRequester(RequestCallback c, int t){
        super(c, t);
    }

    @Override
    public Integer doInBackground(String... data){
        int status=-1;
        Object response=null;
        HttpURLConnection urlConnection=null;
        try {
            Log.d("Monument", "contacting url  " + data[0]);
            URL url = new URL(data[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            response = total.toString();
            in.close();
        }
        catch(MalformedURLException e){

        }
        catch(IOException e){

        }
        finally{
            if(urlConnection != null)
                urlConnection.disconnect();
        }
        Log.d("Monument", "response received is " + response);
        callback.setResponse(response, status, task);
        return status;
    }

    @Override
    protected void onPostExecute(Integer result) {
        callback.done(task);
    }

}
