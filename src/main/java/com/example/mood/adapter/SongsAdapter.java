package com.example.mood.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mood.R;
import com.example.mood.model.Song;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SongsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //String queryText = "";  // text highlight testing

    //member variable
    List<Song> songs;
    ExoPlayer player;

    //constructor
    public SongsAdapter(List<Song> songs, ExoPlayer player) {
        this.songs = songs;
        this.player = player;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.song_row_item,parent,false);

        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        //current song and view holder
        int pos = position;
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        //set values to views
        viewHolder.titleHolder.setText(song.getName());
        viewHolder.sizeHolder.setText(getSize(song.getSize()));
        viewHolder.durationHolder.setText(getDuration(song.getDuration()));

        //album art
        Uri albumartUri = song.getAlbumartUri();
        if(albumartUri != null){
            viewHolder.albumartHolder.setImageURI(albumartUri);

            if(viewHolder.albumartHolder.getDrawable() == null){
                viewHolder.albumartHolder.setImageResource(R.drawable.default_albumart);
            }
        }
        else {
            viewHolder.albumartHolder.setImageResource(R.drawable.default_albumart);
        }

        //on click item
        viewHolder.rowItemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //media item
                MediaItem mediaItem = getMediaItem(song);
                if (!player.isPlaying()){
                    player.setMediaItems(getMediaItems(),pos,0);
                    Toast.makeText(v.getContext(), "Running if block of songsAdapter", Toast.LENGTH_SHORT).show();
                }else{
                    player.pause();
                    player.seekTo(pos,0);
                }
               player.prepare();
                player.play();

                Toast.makeText(v.getContext(), "Selected song: "+ song.getName(), Toast.LENGTH_SHORT).show();

                //playPauseBtn.setImageResource(R.drawable.ic_pause_circle);

            }
        });

    }

    private List<MediaItem> getMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs){
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();
            mediaItems.add(mediaItem);
        }
        return  mediaItems;
    }

    private MediaItem getMediaItem(Song song) {
        return new MediaItem.Builder()
                .setUri(song.getUri())
                .setMediaMetadata(getMetadata(song))
                .build();
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getName())
                .setArtworkUri(song.getAlbumartUri())
                .build();
    }


    @Override
    public int getItemCount() {
        return songs.size();
    }


    //view holder
    public static class SongViewHolder extends RecyclerView.ViewHolder{

        //member variable
        ConstraintLayout rowItemLayout;
        ImageView albumartHolder;
        TextView  titleHolder, durationHolder, sizeHolder;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            rowItemLayout = itemView.findViewById(R.id.rowItemLayout);
            albumartHolder = itemView.findViewById(R.id.albumart);
            titleHolder = itemView.findViewById(R.id.title);
            sizeHolder = itemView.findViewById(R.id.size);
            durationHolder = itemView.findViewById(R.id.duration);
        }
    }


    @SuppressLint("DefaultLocale")
    private String getDuration(int totalDuration){
        String totalDurationText;
        int hrs = totalDuration/(1000*60*60);
        int min = (totalDuration%(1000*60*60))/(1000*60);
        int secs = (((totalDuration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;

        if (hrs<1){ totalDurationText = String.format("%02d:%02d", min, secs); }
        else{
            totalDurationText = String.format("%1d:%02d", hrs, min, secs);
        }
        return totalDurationText;
    }

    private String getSize(long bytes){
        String hrSize;

        double k = bytes/1024.0;
        double m = ((bytes/1024.0)/1024.0);
        double g = (((bytes/1024.0)/1024.0)/1024.0);
        double t = ((((bytes/1024.0)/1024.0)/1024.0)/1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");

        if ( t>1 ) {
            hrSize = dec.format(t).concat(" TB");
        }
        else if ( g>1 ) {
            hrSize = dec.format(g).concat(" GB");
        }
        else if ( m>1 ) {
            hrSize = dec.format(m).concat(" MB");
        }
        else if ( k>1 ) {
            hrSize = dec.format(k).concat(" KB");
        }
        else {
            hrSize = dec.format((double) bytes).concat(" Bytes");
        }

        return  hrSize;
    }

    // for apply search view
    public void updateList (List<Song> songList)
    {

        /*queryText = songList.toString();   //text highlight testing

        int startPos = queryText.toLowerCase().indexOf(queryText.toLowerCase());
        int endPos = startPos + queryText.length();
        if (startPos != -1){
            Spannable spannable = new SpannableString(queryText);
            ColorStateList colorStateList = new ColorStateList(new int[][]{new int[]{}},new int [] {android.R.color.holo_blue_bright});
            TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(null, Typeface.BOLD,-1,colorStateList,null);
            spannable.setSpan(textAppearanceSpan.startPos,endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            SongViewHolder
        }else{
            songs.addAll(songList);
        }*/

        songs = new ArrayList<>();
        songs.addAll(songList);
        notifyDataSetChanged();
    }

}
