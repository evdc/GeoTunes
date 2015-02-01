package com.andrewdutcher.geotunes;


import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import java.util.List;

import kaaes.spotify.webapi.android.models.Playlist;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SpotifyPlayer implements PlayerNotificationCallback {

    private SpotifyWebAPI spotifyWebAPI;
    private Player mPlayer;
    private Activity mActivity;
    private List<Playlist> userPlaylists;
    private String clientID;

    SpotifyPlayer(Activity activityContext, String clientID) {
        this.clientID = clientID;
        mActivity = activityContext;
    }

    // Extracts the OAuth token and uses it to create the Player.
    // Also uses it to set up the Web API handler.
    public void setupSpotify(Uri uri) {
        if (uri != null) {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            String oAuthToken = response.getAccessToken();
            this.spotifyWebAPI = new SpotifyWebAPI(oAuthToken);
            Config playerConfig = new Config(mActivity, oAuthToken, clientID);
            Spotify spotify = new Spotify();
            this.mPlayer = spotify.getPlayer(playerConfig, mActivity, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player player) {
                    mPlayer.addPlayerNotificationCallback(SpotifyPlayer.this);
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
    public void startPlaylist(final String name) {
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

    public void pause() {
        mPlayer.pause();
    }

    public void resume() {
        mPlayer.resume();
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("SpotifyPlayer", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("SpotifyPlayer", "Playback error received: " + errorType.name());
    }

}
