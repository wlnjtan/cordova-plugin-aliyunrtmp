package com.alibaba.livecloud.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.alibaba.livecloud.R;
import com.duanqu.qupai.mediaplayer.DataSpec;
import com.duanqu.qupai.mediaplayer.QuPlayer;
import com.duanqu.qupai.mediaplayer.QuPlayerExt;

public class LivePlayerActivity extends Activity
        implements CompoundButton.OnCheckedChangeListener , SurfaceHolder.Callback{
    private static final String TAG = "QuPlayer";
    private QuPlayerExt mQuPlayer;
    private EditText _PlayUrlText;
    private ToggleButton _PlayButton;
    private Surface _Surface;
    private SurfaceView _SurfaceView;
    private Handler _Handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_quliveplayer);

        mQuPlayer = new QuPlayerExt();
        _PlayUrlText = (EditText) findViewById(R.id.play_url);
        _PlayButton = (ToggleButton) findViewById(R.id.play);
        _PlayButton.setOnCheckedChangeListener(this);
        _SurfaceView = (SurfaceView) findViewById(R.id.surface);
        _SurfaceView.getHolder().addCallback(this);
        _Handler = new Handler(Looper.myLooper());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mQuPlayer.setErrorListener(_ErrorListener);
            mQuPlayer.setInfoListener(_InfoListener);
            mQuPlayer.setSurface(_Surface);
            String url = _PlayUrlText.getText().toString();
            if (url.isEmpty()) {
                Toast.makeText(this, "url is null", Toast.LENGTH_LONG).show();
                url = "rtmp://play.lss.qupai.me/qupai-live/wangty123";
            }

            DataSpec spec = new DataSpec(url, DataSpec.MEDIA_TYPE_STREAM);
            mQuPlayer.setDataSource(spec);
            mQuPlayer.setLooping(true);
            mQuPlayer.prepare();
            mQuPlayer.start();

        } else {
            _Handler.removeCallbacks(_Restart);
            synchronized (mQuPlayer) {
                mQuPlayer.stop();
            }
        }
    }

    private Runnable _Restart = new Runnable() {
        @Override
        public void run() {
            synchronized (mQuPlayer) {
                Log.d(TAG, "stop stream");
                mQuPlayer.stop();
                Log.d(TAG, "start stream");
                mQuPlayer.start();
            }
        }
    };

    private long _TestStartTime;

    private QuPlayer.OnInfoListener _InfoListener = new QuPlayer.OnInfoListener() {
        @Override
        public void onStart() {
            Log.d(TAG, "starting ... ");
            _TestStartTime = System.currentTimeMillis();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stoping ...");
        }

        @Override
        public void onVideoStreamInfo(int width, int height) {
            String log = String.format("Video Stream info width %d height %d \n", width, height);
            Log.d(TAG, log);
            Log.d(TAG, "onVideoStreamInfo spend time " + (System.currentTimeMillis() - _TestStartTime));
        }

        @Override
        public void onAndroidBufferQueueCount(int count) {
            Log.d(TAG, "Android buffer Queue count " + count);
        }

        @Override
        public void onProgress(long progress) {
            String log = String.format("progress %d\n", progress);
            Log.d(TAG,"onProgress spend time " + (System.currentTimeMillis() - _TestStartTime));
        }
    };

    private QuPlayer.OnErrorListener _ErrorListener = new QuPlayer.OnErrorListener() {
        @Override
        public void onError(int error) {
            String log = String.format("Error %d\n", error);
            Log.e(TAG, log);

            _Handler.postDelayed(_Restart, 3000);

        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        _Surface = holder.getSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mQuPlayer.dispose();
    }
}
