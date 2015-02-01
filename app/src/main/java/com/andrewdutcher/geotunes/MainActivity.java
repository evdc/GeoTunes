package com.andrewdutcher.geotunes;

import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import kaaes.spotify.webapi.android.models.Playlist;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {

    private static final String CLIENT_ID = "54d4b45fd6604dd3952c0f0f3d0c5530";
    private static final String REDIRECT_URI = "geotunes-login://callback";

    private Player mPlayer;
    private SpotifyWebAPI spotifyWebAPI;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private GoogleMap mGoogleMap = null;
    private Button mRecordingButton;

    private boolean isRecording = false;

    @Override
    // Authenticate the user with Spotify.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.mLatitudeText = (TextView) findViewById(R.id.latitudeText);
        this.mLongitudeText = (TextView) findViewById(R.id.longitudeText);
        this.mRecordingButton = (Button) findViewById(R.id.recordingButton);

        this.buildGoogleApiClient();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        //SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
         //       new String[]{"user-read-private", "playlist-read-private", "streaming"}, null, this);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    // Called by the authorization service when finished with authentication.
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        setupSpotify(uri);
        //playPlaylist(0);    // Just use the first playlist for testing
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    /* ======= Spotify helper functions ======= */

    // Extracts the OAuth token and uses it to create the Player.
    // Also uses it to set up the Web API handler.
    private void setupSpotify(Uri uri) {
        if (uri != null) {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            String oAuthToken = response.getAccessToken();
            this.spotifyWebAPI = new SpotifyWebAPI(oAuthToken);
            Config playerConfig = new Config(this, oAuthToken, CLIENT_ID);
            Spotify spotify = new Spotify();
            this.mPlayer = spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    mPlayer.addConnectionStateCallback(MainActivity.this);
                    mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    //mPlayer.play("spotify:track:2TpxZ7JUBn3uw46aR7qd6V");
                    playPlaylist(0);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                }
            });

            // This object will handle making requests to the Web API
            // (for playlists, tracks and all)
        }
    }

    // Use the Web API to request a list of Spotify IDs, as strings,
    // from a playlist. Then add them all to the player queue.
    private void playPlaylist(final int id) {
        this.spotifyWebAPI.getUserPlaylists(new Callback<List<Playlist>>() {
            @Override
            public void success(List<Playlist> playlists, Response response) {
                Log.d("MainActiviy", "Got " + playlists.size() + " playlists");
                Playlist playlist = playlists.get(id);
                List<String> track_ids = spotifyWebAPI.getPlaylistTrackIDs(playlist);
                Log.d("MainActivity", "Track ids dump:");
                for (String trackId : track_ids) {
                    Log.d("MainActivity", trackId);
                }
                Log.d("MainActivity", "End Track ids dump");
                mPlayer.play(track_ids);
            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("MainActivity", "Google Play Services connection failed D:");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        this.onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5*1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MainActivity", "Google Play Services connection suspended D:");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLastLocation = location;
            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));

            if (mGoogleMap != null && isRecording) {
                LatLng hereNow = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(hereNow));
                mGoogleMap.addMarker(new MarkerOptions()
                        .title(String.valueOf(Calendar.getInstance().get(Calendar.SECOND)))
                        .position(hereNow));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);
    }

    public void recordingButtonClick(View view) {
        if (isRecording) {
            isRecording = false;
        } else {
            isRecording = true;
            if (mGoogleMap != null && mLastLocation != null) {
                mGoogleMap.clear();
                LatLng hereNow = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hereNow, 19));
            }
        }
    }
}
