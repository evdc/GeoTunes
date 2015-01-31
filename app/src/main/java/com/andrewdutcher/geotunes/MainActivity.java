package com.andrewdutcher.geotunes;

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
import java.util.List;
import android.view.View;

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

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "54d4b45fd6604dd3952c0f0f3d0c5530";
    private static final String REDIRECT_URI = "geotunes-login://callback";

    private Player mPlayer;
    private SpotifyWebAPI spotifyWebAPI;


    @Override
    // Authenticate the user with Spotify.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
                new String[]{"user-read-private", "playlist-read-private", "streaming"}, null, this);
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
}
