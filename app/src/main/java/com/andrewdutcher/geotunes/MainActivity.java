package com.andrewdutcher.geotunes;

import android.graphics.Point;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.Touch;
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
import com.google.android.gms.location.Geofence;
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
    private static Player mPlayer;
    private static SpotifyWebAPI spotifyWebAPI;
    private static List<Playlist> userPlaylists;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private GoogleMap mGoogleMap = null;
    private Button mRecordingButton;
    private TouchOverlayView mOverlayView;

    private GeofenceRequester mGeofenceRequester;

    private boolean isRecording = false;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
                new String[]{"user-read-private", "playlist-read-private", "streaming"}, null, this);

        this.mLatitudeText = (TextView) findViewById(R.id.latitudeText);
        this.mLongitudeText = (TextView) findViewById(R.id.longitudeText);
        this.mRecordingButton = (Button) findViewById(R.id.recordingButton);
        this.mOverlayView = (TouchOverlayView) (findViewById(R.id.touchOverlay));

        this.buildGoogleApiClient();

        this.mGeofenceRequester = new GeofenceRequester(this, mGoogleApiClient);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        //mapFragment.getView().setVisibility(View.INVISIBLE);
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
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }
    }

    // Start playing a playlist by name.
    // Nested callbacks ensure that things execute in the correct order.
    public static void startPlaylist(final String name) {
        spotifyWebAPI.getUserPlaylists(new Callback<List<Playlist>>() {
            @Override
            public void success(List<Playlist> playlists, Response response) {
                mPlayer.clearQueue(); // Clear the existing play queue first
                userPlaylists = playlists;
                Playlist toPlay = new Playlist();
                for(Playlist pl : userPlaylists) {
                    if(pl.name.equals(name)) { toPlay = pl; }
                }
                if(toPlay == null || toPlay.id == null) {
                    Log.e("MainActivity", "Playlist "+name+" not found.");
                }
                else {
                    mPlayer.play("spotify:user:"+toPlay.owner.id+":playlist:"+toPlay.id);
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) { }
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

            /*if (mGoogleMap != null && isRecording) {
                LatLng hereNow = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(hereNow));
                mGoogleMap.addMarker(new MarkerOptions()
                        .title(String.valueOf(Calendar.getInstance().get(Calendar.SECOND)))
                        .position(hereNow));
            }*/
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);
    }

    public void recordingButtonClick(final View view) {
        if (isRecording) {
            isRecording = false;
            mRecordingButton.setText(R.string.drawCirle);
            mOverlayView.unregisterCallback();
        } else {
            isRecording = true;
            mRecordingButton.setText(R.string.stopDrawing);
            /*if (mGoogleMap != null && mLastLocation != null) {
                mGoogleMap.clear();
                LatLng hereNow = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hereNow, 19));
            }*/
            mOverlayView.registerCallback(new TouchOverlayView.TouchCallback() {
                private boolean drawing = false;
                private LatLng origin, end;

                public void onTouchUp(double x, double y) {

                    if(!drawing) {
                        origin = mGoogleMap.getProjection().fromScreenLocation(new Point((int)x, (int)y));
                        drawing = true;
                        mRecordingButton.setText(R.string.stopDrawing);
                        mGoogleMap.addMarker(new MarkerOptions().position(origin).title("Origin"));
                    }
                    else {
                        end = mGoogleMap.getProjection().fromScreenLocation(new Point((int)x, (int)y));
                        drawing = false;
                        isRecording = false;    //also stop listening for touches
                        mOverlayView.unregisterCallback();
                        mOverlayView.setCaptureEnabled(false);
                        mRecordingButton.setText(R.string.drawCirle);

                        float[] results = new float[1]; // because distanceBetween has weird requirements
                        Location.distanceBetween(origin.latitude, origin.longitude, end.latitude, end.longitude, results); // it puts the result in results[0] because reasons
                        //mGoogleMap.addMarker(new MarkerOptions().position(origin).title("Origin"));
                        mGoogleMap.addMarker(new MarkerOptions().position(end).title("End"));
                        Circle circle = mGoogleMap.addCircle(new CircleOptions().center(origin).radius(results[0]));

                        // Bind the circles to Geofences!
                        mGeofenceRequester.addGeofence(origin, results[0]);
                    }
                }

                public void onTouchDown(double x, double y) {}
                public void onTouchMove(double x, double y) {}

                public void onTouchCancel() {
                    mOverlayView.unregisterCallback();
                    mOverlayView.setCaptureEnabled(false);
                    isRecording = false;
                    mRecordingButton.setText(R.string.drawCirle);
                }
            });
        }
        mOverlayView.setCaptureEnabled(isRecording);
        mOverlayView.setEnabled(true);
    }

}
