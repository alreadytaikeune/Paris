package com.khlif.app.paris;

/**
 * Created by anis on 07/05/16.
 */
public interface RequestCallback<T> {
    void setResponse(T o, int status, int task);
    void done(int task);
}
