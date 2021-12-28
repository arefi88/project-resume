package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import com.example.musicplayer.databinding.ActivityMainBinding;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnMusicClickListener {
    private ActivityMainBinding mainBinding;
    private RecyclerView recyclerView;
    private List<Music> musicList=Music.getList();
    private MusicAdapter musicAdapter;
    private MediaPlayer mediaPlayer;
    private Timer timer;
    private boolean isDragging;
    private int cursor;
    private Random random;
    private MusicState musicState=MusicState.STOPED;

    @Override
    public void onClick(Music music, int position) {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        cursor=position;
        onMusicChange(musicList.get(cursor));
    }

    enum MusicState{
        PLAYING,PAUSED,STOPED;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Fresco.initialize(this);
        super.onCreate(savedInstanceState);
        mainBinding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        random=new Random();
        cursor=random.nextInt(musicList.size());
        recyclerView=findViewById(R.id.rv_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,RecyclerView.VERTICAL,false));
        musicAdapter=new MusicAdapter(musicList,this);
        recyclerView.setAdapter(musicAdapter);

        onMusicChange(musicList.get(cursor));
        mainBinding.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (musicState){
                    case PLAYING:
                        mediaPlayer.pause();
                        musicState=MusicState.PAUSED;
                        mainBinding.playBtn.setImageResource(R.drawable.ic_play_32dp);
                        break;
                    case PAUSED:
                    case STOPED:
                        mediaPlayer.start();
                        musicState=MusicState.PLAYING;
                        mainBinding.playBtn.setImageResource(R.drawable.ic_pause_24dp);
                        break;
                }
            }
        });
        mainBinding.musicSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {

                mainBinding.positionTv.setText(Music.convertMillisToString((long) value));
            }
        });
        mainBinding.musicSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                isDragging=true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
              isDragging=false;
              mediaPlayer.seekTo((int) slider.getValue());
            }
        });
        mainBinding.nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goNext();
            }
        });
        mainBinding.prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goPrev();
            }
        });

    }

    private void goPrev() {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        if (cursor==0){
            cursor=musicList.size()-1;
        }else {
            cursor--;
        }
        onMusicChange(musicList.get(cursor));
    }

    private void goNext() {
        timer.cancel();
        timer.purge();
        mediaPlayer.release();
        if (cursor<musicList.size()-1){
            cursor++;
        }else {
            cursor=0;
        }
        onMusicChange(musicList.get(cursor));
    }

    private void onMusicChange(Music music){
        musicAdapter.notifyMusicChange(music);
        mainBinding.musicSlider.setValue(0);
        mediaPlayer=MediaPlayer.create(this,music.getMusicFileResId());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                timer=new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isDragging)
                               // mainBinding.positionTv.setText(Music.convertMillisToString(mediaPlayer.getCurrentPosition()));
                                mainBinding.musicSlider.setValue(mediaPlayer.getCurrentPosition());
                            }
                        });

                    }
                },1000,1000);
                mainBinding.durationTv.setText(Music.convertMillisToString(mediaPlayer.getDuration()));
                mainBinding.musicSlider.setValueTo(mediaPlayer.getDuration());
                musicState=MusicState.PLAYING;
                mainBinding.playBtn.setImageResource(R.drawable.ic_pause_24dp);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        goNext();
                    }
                });
            }
        });

       mainBinding.coverIv.setActualImageResource(music.getCoverResId());
       mainBinding.artistIv.setActualImageResource(music.getArtistResId());
       mainBinding.artistTv.setText(music.getArtist());
       mainBinding.musicNameTv.setText(music.getName());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        mediaPlayer.release();
        mediaPlayer=null;
    }
}
