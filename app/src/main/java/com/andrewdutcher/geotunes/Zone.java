package com.andrewdutcher.geotunes;

import com.google.android.gms.location.Geofence;

import kaaes.spotify.webapi.android.models.Playlist;

/**
 * Created by andrew on 1/31/15.
 */
public class Zone {
    private Playlist myPlaylist;
    private Geofence myGeofence;

    Zone(Playlist playlist, Geofence geofence) {
        myGeofence = geofence;
        myPlaylist = playlist;
    }
}
