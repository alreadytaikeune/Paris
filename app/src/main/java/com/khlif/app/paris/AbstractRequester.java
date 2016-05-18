package com.khlif.app.paris;

import android.os.AsyncTask;

/**
 * Created by anis on 11/05/16.
 */
public abstract class AbstractRequester<T, U, V> extends AsyncTask<T, U, V> {

    protected RequestCallback callback;
    protected int task;

    public AbstractRequester(RequestCallback c, int t){
        callback = c;
        task = t;
    }
}
