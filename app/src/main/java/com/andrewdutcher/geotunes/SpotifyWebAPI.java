package com.andrewdutcher.geotunes;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.*;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.User;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SpotifyWebAPI {
    private String accessToken;
    private String userID;
    private SpotifyService spotify;

    // Constructor -- set access token and user ID, initialize service
    SpotifyWebAPI(String accessToken){
        this.accessToken = accessToken;

        SpotifyApi api = new SpotifyApi();
        spotify = api.setAccessToken(accessToken).getService();

        getUserID();    // has side effect of setting this.userID
    }

    // Get the User ID of the logged-in user.
    public void getUserID() {
        //return spotify.getMe().id;

        spotify.getMe(new Callback<User>() {
            @Override
            public void success(User user, Response response) {
                Log.d("Get user ID success", user.id);
                userID = user.id;
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("Get user ID failure", error.toString());
            }
        });
    }

    // Get a List of all Playlists for the user.
    private List<Playlist> mPlaylists= new ArrayList<Playlist>();
    public void getUserPlaylists(final Callback<List<Playlist>> callback) {
        //return spotify.getPlaylists(userID).items;

        if(mPlaylists.isEmpty()) {
            Log.d("SpotifyWebAPI", "Playlists is empty. Fetching.");
            spotify.getPlaylists("evdc", new Callback<Pager<Playlist>>() {
                @Override
                public void success(Pager<Playlist> playlists, Response response) {
                    Log.d("Get user playlists success", playlists.href);
                    mPlaylists = playlists.items;
                    callback.success(mPlaylists, response);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d("Get playlists failure", error.toString());
                    callback.failure(error);
                }
            });
        } else {
            callback.success(mPlaylists, null);
        }
    }

    // Get a List of Spotify IDs (as strings) of the tracks of a given playlist.
    // Is it better to get these from the Playlist object, or make another spotify request?
    public List<String> getPlaylistTrackIDs(Playlist playlist) {

        Log.d("SpotifyWebApi", "Getting Track IDs for Playlist: " + playlist.name);
        List<String> track_ids = new ArrayList<String>();

        Pager<PlaylistTrack> tracksPager = playlist.tracks;
        Log.d("SpotifyWebAPI", "Brace for null...");
        Log.d("SpotifyWebAPI", "Pager is " + tracksPager.toString());
        //tracksPager.
        Log.d("SpotifyWebAPI", "Pager items is " + tracksPager.items.toString());



        for(PlaylistTrack playlistTrack : tracksPager.items) {
            Track track = playlistTrack.track;
            String track_id = track.id;     // This is the Spotify ID for the track
            track_ids.add(track_id);
        }
        return track_ids;
    }
}
