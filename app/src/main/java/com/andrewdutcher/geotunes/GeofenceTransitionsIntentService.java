package com.andrewdutcher.geotunes;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition.
 * Do stuff with that transition (like play music) here.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "geofence-transitions-service";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int transitionType = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            doSomethingWithThis(transitionType, triggeringGeofences);
        } else {
            // handle error
        }
    }

    private void doSomethingWithThis(int transitionType, List<Geofence> triggers) {
        // get a geofence, find the Zone or Playlist object associated with it via some sort of db
        Log.d(TAG, getString(transitionType, triggers));
        MainActivity.startPlaylist("Party Playlist");
    }

    /**
     * Maps geofence transitions to their human-readable equivalents.
     */
    private String getString(int transitionType, List<Geofence> triggers) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entered region " + triggers.get(0).getRequestId();
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exited region " + triggers.get(0).getRequestId();
            default:
                return "What happened? I'm lost.";
        }
    }
}