/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.icsdroid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.MTMLoader;
import at.bitfire.icsdroid.R;
import de.duenndns.ssl.MemorizingTrustManager;
import lombok.Cleanup;

public class AddCalendarValidationFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<ResourceInfo> {

    AddCalendarActivity activity;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (AddCalendarActivity)activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);

        ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setMessage(getString(R.string.add_calendar_validating));
        return progress;
    }


    // loader callbacks

    @Override
    public Loader<ResourceInfo> onCreateLoader(int id, Bundle args) {
        return new ResourceInfoLoader(activity);
    }

    @Override
    public void onLoadFinished(Loader<ResourceInfo> loader, ResourceInfo info) {
        getDialog().dismiss();

        String errorMessage = null;
        if (info.exception != null)
            errorMessage = info.exception.getMessage();
        else if (info.statusCode != 200)
            errorMessage = info.statusCode + " " + info.statusMessage;

        if (errorMessage == null) {
            Bundle args = new Bundle(1);
            args.putString(AddCalendarDetailsFragment.KEY_TITLE, info.calendarName != null ?
                    info.calendarName : info.url.getPath());

            Fragment detailsFrag = new AddCalendarDetailsFragment();
            detailsFrag.setArguments(args);

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, detailsFrag)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } else
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(Loader<ResourceInfo> loader) {
    }


    // loader

    protected static class ResourceInfoLoader extends AsyncTaskLoader<ResourceInfo> {
        ResourceInfo info;
        boolean loaded;

        public ResourceInfoLoader(AddCalendarActivity activity) {
            super(activity);

            info = new ResourceInfo(activity.url, activity.authRequired, activity.username, activity.password);
        }

        @Override
        protected void onStartLoading() {
            synchronized(this) {
                if (!loaded) {
                    forceLoad();
                    loaded = true;
                }
            }
        }

        @Override
        public ResourceInfo loadInBackground() {
            URLConnection conn = null;
            try {
                conn = info.url.openConnection();
                conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(20000);
                if (info.authRequired) {
                    String basicCredentials = info.username + ":" + info.password;
                    conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.getBytes(), 0));
                }

                if (conn instanceof HttpsURLConnection)
                    MTMLoader.prepareHttpsURLConnection(getContext(), (HttpsURLConnection)conn);

                boolean readFromStream = false;
                if (conn instanceof HttpURLConnection) {
                    info.statusCode = ((HttpURLConnection)conn).getResponseCode();
                    info.statusMessage = ((HttpURLConnection)conn).getResponseMessage();

                    if (info.statusCode == 200)
                        readFromStream = true;
                } else {
                    info.statusCode = 200;
                    readFromStream = true;
                }

                if (readFromStream) {
                    @Cleanup InputStream is = conn.getInputStream();
                    Map<String, String> properties = new HashMap<>();
                    Event[] events = Event.fromStream(is, null, properties);

                    info.calendarName = properties.get(Event.CALENDAR_NAME);
                    info.eventsFound = events.length;
                }

            } catch (IOException|InvalidCalendarException e) {
                info.exception = e;
            }
            return info;
        }
    }
}