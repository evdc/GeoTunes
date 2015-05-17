package com.evan.geotunes;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.*;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TracksPager;
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
    }

    // Get a List of all Playlists for the user.
    // Note: the getPlaylists call is wrapped in getMe to *ensure* userID exists at the right time,
    // because that shit kept failing previously. Callbacks on callbacks on callbacks.
    public void getUserPlaylists(final Callback<List<Playlist>> callback) { // Added a callback argument\
        spotify.getMe(new Callback<User>() {
            @Override
            public void success(User user, Response response) {
                userID = user.id;
                spotify.getPlaylists(userID, new Callback<Pager<Playlist>>() {
                    @Override
                    public void success(Pager<Playlist> playlists, Response response) {
                        Log.d("Got user playlists", playlists.href);
                        callback.success(playlists.items, response);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d("Get playlists failure", error.toString());
                        callback.failure(error);
                    }
                });
            }
            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e("SpotifyWebAPI", retrofitError.toString());
            }
        });
    }

    //Get a List of all Tracks in a Playlist.
    public void getPlaylistTracks(Playlist playlist, final Callback<List<Track>> callback) {
        final List<Track> tracks = new ArrayList<Track>();
        spotify.getPlaylistTracks(userID, playlist.id, new Callback<Pager<PlaylistTrack>>() {
            @Override
            public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                if(playlistTrackPager != null && playlistTrackPager.items != null) {
                    for(PlaylistTrack item : playlistTrackPager.items) {
                        tracks.add(item.track);
                    }
                    callback.success(tracks, null);
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {

            }
        });
    }
}