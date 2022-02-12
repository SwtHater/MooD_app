package com.example.mood.view;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mood.R;
import com.example.mood.adapter.SongsAdapter;
import com.example.mood.model.Song;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    //initialize variable
    ActivityResultLauncher<String> storagePermissionLauncher;
    RecyclerView recyclerView;
    SongsAdapter songsAdapter;
    ArrayList<Song> song;
    ConstraintLayout controlsWrapper;
    TextView playingSongNameView, skipPrevSongBtn, skipNextSongBtn;
    ImageButton playPauseBtn;
    ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         recyclerView = findViewById(R.id.recyclerview);
         playingSongNameView = findViewById(R.id.playingSongNameView);
         playingSongNameView.setSelected(true); // for marquee
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        skipPrevSongBtn = findViewById(R.id.prevBtn);
        skipNextSongBtn = findViewById(R.id.nextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        controlsWrapper = findViewById(R.id.controlsWrapper);


        //set toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("MooD");

        //assigning storage permission launcher
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result){
                //permission was granted
                //fetchSongs();
                song = getAllAudio(this);
            }
            else {
                //responding on users action
                respondOnUserPermissionActs();
            }
        });

        //launch the storage permission launcher
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);


        //player controls
        playerControls();
    }

    private void playerControls() {
        skipNextSongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.hasNextMediaItem()){
                    player.seekToNext();
                }
            }
        });

        skipPrevSongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.hasPreviousMediaItem()){
                    player.seekToPrevious();
                }
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player.isPlaying()){
                    player.pause();
                    playPauseBtn.setImageResource(R.drawable.ic_play_circle);
                    Toast.makeText(view.getContext(), "MainActivity if block execute", Toast.LENGTH_SHORT).show();

                }else{
                    if(player.getMediaItemCount()>0){
                        player.play();
                        playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                        Toast.makeText(view.getContext(), "MainActivity else if  block execute", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });

        //player listener
        playerListener();

    }

    private void playerListener() {
        player.addListener(new Player.Listener () {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                assert mediaItem != null;
                playingSongNameView.setText(mediaItem.mediaMetadata.title);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == ExoPlayer.STATE_READY){
                    playingSongNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    playPauseBtn.setImageResource(R.drawable.ic_pause_circle);
                    //player.play();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()){
            player.stop();
        }
        player.release();
    }

    private void respondOnUserPermissionActs() {
        // user respons deny
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            //permission granted
            //fetchSongs();
            song = getAllAudio(this);
        }
        else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            //show an educational UI dialog box to explaining why we need this permission
            new AlertDialog.Builder(this)
                    .setTitle("Requesting Permission")
                    .setMessage("Please allow us to fetch song on your device from your external storage")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //request permission again
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                    })
                    .setNegativeButton("dont Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "You denied to fetch song", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
        else {
            Toast.makeText(this, "You denied to fetch song", Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<Song> getAllAudio(Context context)
    {
        ArrayList<Song> songs = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

       // String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        Cursor cursor = context.getContentResolver().query(uri, projection, null,null,MediaStore.Audio.Media.DATE_ADDED + " DESC");
        if(cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                int duration = cursor.getInt(2);
                int size = cursor.getInt(3);
                long albumId = cursor.getLong(4);

                //song uri
                //Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);

                //album art uri
                Uri albumartUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                //remove .mp3 extension on song name
                name = name.substring(0, name.lastIndexOf("."));

                Song song = new Song(id, uri, name, duration, size, albumId, albumartUri);
                songs.add(song);

            }
            cursor.close();
        }
        showSongs(songs);
        return songs;
    }

    /*private void fetchSongs() {
        //define list to carry the song
        ArrayList<Song> songs = new ArrayList<>();
        Uri songLibraryUri ;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            songLibraryUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        else {
            songLibraryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        //projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        //Quering
        try(Cursor cursor = getContentResolver().query(songLibraryUri, projection, null, null, sortOrder)) {
            //cache the cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            //getting the values
            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                //song uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);

                //album art uri
                Uri albumartUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId);

                //remove .mp3 extension on song name
                name = name.substring(0,name.lastIndexOf("."));

                //song item
                Song song = new Song(id,uri,name,duration,size,albumId,albumartUri);

                //add song to songs list
                songs.add(song);

            }
            //show songs on recycler view
            showSongs(songs);
        }
    }*/

    private void showSongs(List<Song> songs) {
        //layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //adapter
        songsAdapter = new SongsAdapter(songs, player);
        recyclerView.setAdapter(songsAdapter);

    }

    // For apply search view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<Song> myFiles = new ArrayList<>();


        for (Song some : song)
        {
            if (some.getName().toLowerCase().contains(userInput))
            {
                myFiles.add(some);
            }
        }
        songsAdapter.updateList(myFiles);
        return true;
    }
}