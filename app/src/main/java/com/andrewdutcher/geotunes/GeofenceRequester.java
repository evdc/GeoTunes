package com.andrewdutcher.geotunes;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.model.LatLng;

/**
 * Class for connecting to Location Services and requesting geofences. <b> Note:
 * Clients must ensure that Google Play services is available before requesting
 * geofences. </b> Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to
 * check.
 *
 *
 * To use a GeofenceRequester, instantiate it and call AddGeofence(). Everything
 * else is done automatically.
 *
 */
public class GeofenceRequester implements ResultCallback<Status> {

    private GoogleApiClient mGoogleApiClient;
    private Geofence mGeofence;
    private PendingIntent mGeofencePendingIntent;
    private String TAG = "GeofenceRequester";
    private Activity mActivity;
    private int requestNum = 0;

    // Instantiate with "this" as the activityContext parameter from MainActivity.
    GeofenceRequester(Activity activityContext, GoogleApiClient apiClient) {
        mGoogleApiClient = apiClient;
        mActivity = activityContext;
    }

    public void addGeofence(LatLng origin, float radius) {

        // First, make the Geofence
        Geofence geofence;
        geofence = new Geofence.Builder().setRequestId("geofence-"+requestNum)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(origin.latitude, origin.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE).build();

        // Then, make the GeofencingRequest
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofence(geofence).setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();

        registerGeofence(geofencingRequest);
    }

    // Function to talk to Location Services and request (that is, add) a (list of) Geofences.
    // TODO: Error checking
    private void registerGeofence(GeofencingRequest geofencingRequest) {
        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG,"Google API Client not connected");
            return;
        }

        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, geofencingRequest, getGeofencePendingIntent()
        ).setResultCallback(this); //will be handled in onResult().
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {

        Intent intent = new Intent(mActivity, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(mActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {

        if (status.isSuccess()) {
            Log.d(TAG, "It worked?");
        } else {
            Log.e(TAG, "It failed?");
        }
    }
}
