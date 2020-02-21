package example.com.rmp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ListView _lvAlbum;
    private List<Map<String, Object>> albumList;
    private static final String[] FROM = {"artist", "album", "title"};
    private static final int[] TO = {R.id.tvArtist, R.id.tvAlbum, R.id.tvTitle};
    private String musicPath = "";
    private int next_index;

    private MediaPlayer player;
    private ImageButton btback;
    private ImageButton btplay;
    private ImageButton btnext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _lvAlbum = findViewById(R.id.lvAlbum);
        // リストを生成し，SimpleAdapterでレイアウトに反映させる
        albumList = createAlbumList();
        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, albumList, R.layout.row, FROM, TO);
        _lvAlbum.setAdapter(adapter);
        _lvAlbum.setOnItemClickListener(new ListItemClickListener());

        // ボタンにリスナーをセット
        btback = findViewById(R.id.back);
        btplay = findViewById(R.id.play);
        btnext = findViewById(R.id.next);
        btback.setOnClickListener(new ButtonsClickListener());
        btplay.setOnClickListener(new ButtonsClickListener());
        btnext.setOnClickListener(new ButtonsClickListener());

        // MediaPlayerオブジェクトを生成
        next_index = new Random().nextInt(albumList.size());
        musicPath = (String) albumList.get(next_index).get("path");
        player = new MediaPlayer();
        Uri mediaFileUri = Uri.parse(musicPath);

        try{
            // メディアプレイヤーに音声ファイルを指定
            player.setDataSource(MainActivity.this, mediaFileUri);
            // 再生が終了した際のリスナをセット
            player.setOnCompletionListener(new PlayerCompletionListener());
            // 非同期で再生を準備
            player.prepareAsync();
        }
        catch(IOException  e) {
            e.printStackTrace();
        }
    }

    // アルバムリストを作成するメソッド
    private List<Map<String, Object>> createAlbumList(){
        // 音楽ファイルのデータをリストで取得
        List<MusicItem> mItems = MusicItem.getItems(getApplicationContext());

        //
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> items;
        for(MusicItem somethin :mItems) {
            items = new HashMap<>();
            items.put("artist", somethin.artist);
            items.put("album", somethin.album);
            items.put("title", somethin.title);
            items.put("path", somethin.path);
            list.add(items);
        }
        return list;
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            // タップされたアイテムのデータを取得
            Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            //次に再生する曲のインデックスとパスを格納
            next_index = albumList.indexOf(item);
            // itemの各要素はObjectなのでStringに変換
            musicPath = (String) item.get("path");
            Uri mediaFileUri = Uri.parse(musicPath);
            //プレイヤーをリセット
            btplay.setImageResource(android.R.drawable.ic_media_play);
            player.stop();
            player.release();
            player = new MediaPlayer();
            try{
                // メディアプレイヤーに音声ファイルを指定
                player.setDataSource(MainActivity.this, mediaFileUri);
                // 再生準備が完了した際のリスナをセット
                player.setOnPreparedListener(new PlayerPreparedListener());
                // 再生が終了した際のリスナをセット
                player.setOnCompletionListener(new PlayerCompletionListener());
                // 非同期で再生を準備
                player.prepareAsync();

            }
            catch(IOException  e) {
                e.printStackTrace();
            }
        }
    }

    private class ButtonsClickListener implements View.OnClickListener{

        @Override
        public void onClick(View view){
            // タップされた部品のidのR値を取得
            int id = view.getId();
            // トーストで表示
            switch (id) {
                case R.id.back:
                    play_next(1);
                    break;
                case R.id.play:
                    if(player.isPlaying()){
                        player.pause();
                        btplay.setImageResource(android.R.drawable.ic_media_play);
                    }else{
                        //再生する曲名を表示
                        String str = (String) albumList.get(next_index).get("title");
                        Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
                        player.start();
                        btplay.setImageResource(android.R.drawable.ic_media_pause);
                    }
                    break;
                case R.id.next:
                    play_next(0);
                    break;
            }

        }

    }

    // 音楽ファイルのデータを取得
    public static class MusicItem implements Comparable<Object> {
        private static final String TAG = "MusicItem";
        final long id;
        final String artist;
        final String title;
        final String album;
        final int truck;
        final long duration;
        final String path;

        public MusicItem(long id, String artist, String title, String album, int truck, long duration, String path) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.truck = truck;
            this.duration = duration;
            this.path = path;
        }

        public Uri getURI() {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }

        /**
         * 外部ストレージ上から音楽を探してリストを返す。
         * @param context コンテキスト
         * @return 見つかった音楽のリスト
         */
        public static List<MusicItem> getItems(Context context) {
            List<MusicItem> items = new LinkedList<MusicItem>();

            // ContentResolver を取得
            ContentResolver cr = context.getContentResolver();

            // 外部ストレージから音楽を検索
            Cursor cur = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);

            if (cur != null) {
                if (cur.moveToFirst()) {
                    Log.i(TAG, "Listing...");

                    // 曲情報のカラムを取得
                    int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
                    int idTruck = cur.getColumnIndex(MediaStore.Audio.Media.TRACK);
                    int dataColumn = cur.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

                    Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
                    Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

                    // リストに追加
                    do {
                        Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
                        items.add(new MusicItem(cur.getLong(idColumn),
                                cur.getString(artistColumn),
                                cur.getString(titleColumn),
                                cur.getString(albumColumn),
                                cur.getInt(idTruck),
                                cur.getLong(durationColumn),
                                cur.getString(dataColumn)));
                    } while (cur.moveToNext());

                    Log.i(TAG, "Done querying media. MusicRetriever is ready.");
                }
                // カーソルを閉じる
                cur.close();
            }

            // 見つかる順番はソートされていないため、アルバム単位でソートする
            Collections.sort(items);
            return items;
        }

        @Override
        public int compareTo(Object another) {
            if (another == null) {
                return 1;
            }
            MusicItem item = (MusicItem) another;
            int result = album.compareTo(item.album);
            if (result != 0) {
                return result;
            }
            return truck - item.truck;
        }
    }

    public Bitmap getArtWork(String filePath){

        Bitmap bm = null;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(filePath);

        byte[] data = mmr.getEmbeddedPicture();

        // 画像が無ければnullになる
        if (null != data) {
                bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return bm;
    }

    private class PlayerPreparedListener implements MediaPlayer.OnPreparedListener{

        @Override
        public void onPrepared(MediaPlayer mp){
            //再生する曲名を表示
            String str = (String) albumList.get(next_index).get("title");
            Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            mp.start();
            btplay.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private class PlayerCompletionListener implements MediaPlayer.OnCompletionListener{

        @Override
        public void onCompletion(MediaPlayer mp) {
            //音楽が終わった時の処理
            play_next(0);
        }
    }

    private void play_next(int flg){
        //次に再生する曲のインデックスとパスを格納
        if(flg == 0){
            //フラグが0ならインデックス更新
            next_index = new Random().nextInt(albumList.size());
        }
        musicPath = (String) albumList.get(next_index).get("path");
        Uri mediaFileUri = Uri.parse(musicPath);
        //プレイヤーをリセット
        player.stop();
        player.release();
        player = new MediaPlayer();
        try {
            // メディアプレイヤーに音声ファイルを指定
            player.setDataSource(MainActivity.this, mediaFileUri);
            // 再生準備が完了した際のリスナをセット
            player.setOnPreparedListener(new PlayerPreparedListener());
            // 再生が終了した際のリスナをセット
            player.setOnCompletionListener(new PlayerCompletionListener());
            // 非同期で再生を準備
            player.prepareAsync();
        }
        catch(IOException  e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(player.isPlaying()){
            player.stop();
        }
        player.release();
        player = null;
    }

}
