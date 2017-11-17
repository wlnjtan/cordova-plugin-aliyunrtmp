package org.apache.cordova.aliyunrtmp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ToggleButton;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.alibaba.livecloud.event.AlivcEvent;
import com.alibaba.livecloud.event.AlivcEventResponse;
import com.alibaba.livecloud.event.AlivcEventSubscriber;
import com.alibaba.livecloud.live.AlivcMediaFormat;
import com.alibaba.livecloud.live.AlivcRecordReporter;
import com.alibaba.livecloud.live.AlivcStatusCode;
import com.alibaba.livecloud.demo.DataStatistics;
import com.alibaba.livecloud.live.AudioAlivcMediaRecorder;
import com.alibaba.livecloud.live.OnLiveRecordErrorListener;
import com.alibaba.livecloud.live.OnNetworkStatusListener;
import com.alibaba.livecloud.live.OnRecordStatusListener;
//import com.alibaba.livecloud.model.AlivcWatermark;
import com.alibaba.livecloud.utils.ToastUtils;
import com.duanqu.qupai.jni.ApplicationGlue;

import com.duanqu.qupai.mediaplayer.DataSpec;
import com.duanqu.qupai.mediaplayer.QuPlayer;
import com.duanqu.qupai.mediaplayer.QuPlayerExt;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * author: wlnj_tan
 */
public class AliyunRTMP extends CordovaPlugin {


    private static final String TAG = "AliyunRTMP";
    private CallbackContext callbackContext;
    private Activity CurrentActivity = null;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] permissionManifest = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    private final int PERMISSION_DELAY = 100;
    private boolean mHasPermission = false;

    private SurfaceView _CameraSurface;
    private AudioAlivcMediaRecorder mMediaRecorder = null;
    private AlivcRecordReporter mRecordReporter;
    private ToggleButton mTbtnMute;

    private Surface mPreviewSurface;
    private Map<String, Object> mConfigure = new HashMap<String, Object>();
    private boolean isRecording = false;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private DataStatistics mDataStatistics = new DataStatistics(1000);

	private QuPlayerExt mQuPlayer = null;
	private boolean isPlaying = false;
  private boolean isPlayStartFlag = false;
	private Handler _PlayHandler;
  private long _PlayLastBufferQueueTime = 0;

    public AliyunRTMP(){
        super();

    }

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    MultiDex.install(this.cordova.getActivity().getApplication());
    System.loadLibrary("gnustl_shared");
//        System.loadLibrary("ijkffmpeg");//目前使用微博的ijkffmpeg会出现1K再换wifi不重连的情况
    System.loadLibrary("qupai-media-thirdparty");
//        System.loadLibrary("alivc-media-jni");
    System.loadLibrary("qupai-media-jni");
    ApplicationGlue.initialize(this.cordova.getActivity().getApplication());
  }

  private void init() {

        mMediaRecorder = new AudioAlivcMediaRecorder();//AlivcMediaRecorderFactory.createMediaRecorder();
        mMediaRecorder.init(CurrentActivity);
       // mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
        mDataStatistics.setReportListener(mReportListener);

        /**
         * this method only can be called after mMediaRecorder.init(),
         * else will return null;
         */
        mRecordReporter = mMediaRecorder.getRecordReporter();

        mDataStatistics.start();
        mMediaRecorder.setOnRecordStatusListener(mRecordStatusListener);
        mMediaRecorder.setOnNetworkStatusListener(mOnNetworkStatusListener);
        mMediaRecorder.setOnRecordErrorListener(mOnErrorListener);

        mConfigure.put(AlivcMediaFormat.KEY_CAMERA_FACING, cameraFrontFacing);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_ZOOM_LEVEL, 3);
        mConfigure.put(AlivcMediaFormat.KEY_OUTPUT_RESOLUTION, resolution);
        mConfigure.put(AlivcMediaFormat.KEY_AUDIO_BITRATE, 32000);
        mConfigure.put(AlivcMediaFormat.KEY_AUDIO_SAMPLE_RATE, 44100);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_VIDEO_BITRATE, maxBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_BEST_VIDEO_BITRATE, bestBitrate *1000);
        mConfigure.put(AlivcMediaFormat.KEY_MIN_VIDEO_BITRATE, minBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_INITIAL_VIDEO_BITRATE, initBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_DISPLAY_ROTATION, screenOrientation ? AlivcMediaFormat.DISPLAY_ROTATION_90 : AlivcMediaFormat.DISPLAY_ROTATION_0);
        mConfigure.put(AlivcMediaFormat.KEY_EXPOSURE_COMPENSATION, -1);//曝光度
       // mConfigure.put(AlivcMediaFormat.KEY_WATERMARK, mWatermark);
        mConfigure.put(AlivcMediaFormat.KEY_FRAME_RATE, frameRate);
    }

    private String pushUrl;
	  private String playUrl;
    private String playMediaType = "stream"; //stream or mp3
    private long playStartPosition = 0;
    private String pushStatusString = "";
    private String playStatusString = "";
    private int resolution = AlivcMediaFormat.OUTPUT_RESOLUTION_240P;
    private boolean screenOrientation = false;
    private int cameraFrontFacing = AlivcMediaFormat.CAMERA_FACING_BACK;
    //private AlivcWatermark mWatermark;
    private int bestBitrate =1; //600;
    private int minBitrate = 1; //500;
    private int maxBitrate = 1; //800;
    private int initBitrate = 1; //600;
    private int frameRate = 1; //30;


    private void permissionCheck() {
        int permissionCheck = PackageManager.PERMISSION_GRANTED;
        for (String permission : permissionManifest) {
            if (PermissionChecker.checkSelfPermission(CurrentActivity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionCheck = PackageManager.PERMISSION_DENIED;
            }
        }
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CurrentActivity, permissionManifest, PERMISSION_REQUEST_CODE);
			mHasPermission = true;
        }else {
            mHasPermission = true;
        }
    }
    @Override
    public boolean execute(String action, JSONArray args,
                           final CallbackContext mcallbackContext) throws JSONException {
        PluginResult result = null;
        CurrentActivity = this.cordova.getActivity();
      CurrentActivity.getApplicationContext();

        callbackContext = mcallbackContext;
        if (Build.VERSION.SDK_INT >= 23 && !mHasPermission) {
            permissionCheck();
        }else {
            mHasPermission = true;
        }
        if ("start".equals(action)) {

            // avoid calling other phonegap apps
            //Intent intentScan = new Intent(SCAN_INTENT);
            //intentScan.addCategory(Intent.CATEGORY_DEFAULT);
            //intentScan.setPackage(this.cordova.getActivity().CurrentActivity.getApplicationContext().getPackageName());

            //this.cordova.startActivityForResult((CordovaPlugin) this, intentScan, REQUEST_CODE);
            //Intent intentEncode = new Intent(ENCODE_INTENT);
            //intentEncode.putExtra(ENCODE_TYPE, type);
            //intentEncode.putExtra(ENCODE_DATA, data);
            // avoid calling other phonegap apps
            //intentEncode.setPackage(this.cordova.getActivity().CurrentActivity.getApplicationContext().getPackageName());

            //this.cordova.getActivity().startActivity(intentEncode);
            //processPicture(intentEncode.getBitmap());

            final String urlStream = args.getString(0);
            final String videoResolution = args.getString(1);
            final Boolean filter = "1".equals(args.getString(2));
            final String outputStreamType = args.getString(3);

            if (urlStream == null || urlStream.equals("") || urlStream.equals("null")) {
                callbackContext.error("Please enter correct stream url");
                return true;
            }
            try
            {
                if(isRecording) {
                    Stop(); //先停止上次,以防重复调用崩溃
                }
                // callbackContext.success("直播失败");
                // return true;
                pushUrl = urlStream;
                /*if(false) {
                    ToastUtils.showToast(CurrentActivity, "直播开启失败，请仔细检查推流地址!");
                    callbackContext.error("直播开启失败，请仔细检查推流地址!");
                    return true;
                }*/
                Start();
            }
            catch(Exception e){
                callbackContext.error("直播失败了" + e.getMessage());
            }
            return true;
        } else if ("stop".equals(action)){
            //stop
            try{
                Stop();
            }
            catch(Exception e){
                callbackContext.error("停止失败");
            }
            //callbackContext.success("直播停止");
            return true;
        }
        else if ("pause".equals(action)){
            //stop
            try{
                Pause();
                callbackContext.success("直播静音成功");
            }
            catch(Exception e){
                callbackContext.error("失败");
            }
            return true;
        }
        else if ("resume".equals(action)){
            //stop
            try{
                Resume();
                callbackContext.success("续播开始");
            }
            catch(Exception e){
                callbackContext.error("失败");
            }
            //callbackContext.success("直播停止");
            return true;
        }
		else if ("play".equals(action)) {
           String urlStream = args.getString(0);
           playMediaType = args.getString(1);
           playStartPosition = args.getLong(2);

            if (playMediaType == null || playMediaType.equals("") || playMediaType.equals("null")) {
                playMediaType = "stream";
            }

          if (urlStream == null || urlStream.equals("") || urlStream.equals("null")) {
            callbackContext.error("Please enter correct stream url");
            return true;
          }
          try {
            if (mQuPlayer == null) {
              mQuPlayer = new QuPlayerExt();
              _PlayHandler = new Handler(Looper.myLooper());
            }
            if (isPlaying) {
              PlayStop(); //先停止上次,以防重复调用崩溃
            }
            playUrl = urlStream;
            Play();
          } catch (Exception e) {
            callbackContext.error("直播失败了" + e.getMessage());
          }
          return true;
        }
        else if("checkplaystatus".equals(action)) {
          try{
            String ret = CheckPlayStatus();
            callbackContext.success(ret);
          }
          catch(Exception e){
            callbackContext.error("状态检查失败");
          }
          return true;
        }
        else if("checkpushstatus".equals(action)) {
          try{
            String ret = CheckPushStatus();
            callbackContext.success(ret);
          }
          catch(Exception e){
            callbackContext.error("状态检查失败");
          }
          return true;
        }
		else if("playstop".equals(action)){
			try{
                PlayStop();
            }
            catch(Exception e){
                callbackContext.error("停止播放失败");
            }
            callbackContext.success("播放停止");
            return true;
		}
        else if("playpause".equals(action)){
            try{
                PlayPause();
            }
            catch(Exception e){
                callbackContext.error("暂停播放失败");
            }
            callbackContext.success("播放暂停");
            return true;
        }
        else if("playresume".equals(action)){
            try{
                PlayResume();
            }
            catch(Exception e){
                callbackContext.error("续播放失败");
            }
            callbackContext.success("续播开始");
            return true;
        }
        else if("getplayduration".equals(action)){
            try{
                String ret = GetPlayDuration();
                callbackContext.success(ret);
            }
            catch(Exception e){
                callbackContext.error("获取长度失败");
            }
            return true;
        }
        else if("playseek".equals(action)){
            try{
                final long seekPos = args.getLong(0);
                PlaySeek(seekPos);
            }
            catch(Exception e){
                callbackContext.error("Seek失败");
            }
            //callbackContext.success("Seek成功"); //回调中返回
            return true;
        }
        else if("getcurrentplayposition".equals(action)){
            try{
                String ret = GetCurrentPlayPosition();
                callbackContext.success(ret);
            }
            catch(Exception e){
                callbackContext.error("获取播放位置失败");
            }
            return true;
        }
        else{
            callbackContext.error("no such method:" + action);
            return false;
        }
    }

    /**
     * Called when the barcode scanner intent completes.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                       allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        /* if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, intent.getStringExtra("SCAN_RESULT"));
                    obj.put(FORMAT, intent.getStringExtra("SCAN_RESULT_FORMAT"));
                    obj.put(CANCELLED, false);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, "");
                    obj.put(FORMAT, "");
                    obj.put(CANCELLED, true);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else {
                //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
                this.callbackContext.error("Unexpected error");
            }
        } */
    }

    protected void Resume() {
		setMute(false);
    }

    protected void Pause() {
        setMute(true);
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {

    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause(boolean multitasking) {

    }

    /**
     * Called when the activity will start interacting with the user.
     */
    @Override
    public void onResume(boolean multitasking) {
        //Resume(); //onPause没有停,不需要Resume
    }

    public void onDestroy() {
      try {
        if (mMediaRecorder != null) {
          mDataStatistics.stop();
          mMediaRecorder.release();
        }
        if (mQuPlayer != null) {
          //mQuPlayer.dispose();
          mQuPlayer = null;
        }
      }
      catch(Exception e){

      }
    }


    private void setMute(boolean isON){
                    if(isON) {
                        mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_MUTE_ON);
                    }else {
                        mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_MUTE_ON);
                    }
            }

    private void Start() {
       try {
            if(mMediaRecorder == null){
              init();
              setMute(false);
            }
            pushStatusString = "";
            mMediaRecorder.prepare(mConfigure, null);

						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_BITRATE_DOWN, mBitrateDownRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_BITRATE_RAISE, mBitrateUpRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_SUCC, mAudioCaptureSuccRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_DATA_DISCARD, mDataDiscardRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_INIT_DONE, mInitDoneRes));
						//mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_SUCC, mVideoEncoderSuccRes));
						// mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_FAILED, mVideoEncoderFailedRes));
						// mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_VIDEO_ENCODED_FRAMES_FAILED, mVideoEncodeFrameFailedRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_ENCODED_FRAMES_FAILED, mAudioEncodeFrameFailedRes));
						mMediaRecorder.subscribeEvent(new AlivcEventSubscriber(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_FAILED, mAudioCaptureOpenFailedRes));

						mMediaRecorder.startRecord(pushUrl);
//                            testPublish(true, pushUrl);
                    } catch (Exception e) {
                      ToastUtils.showToast(CurrentActivity, e.getMessage());
					  callbackContext.error(e.getMessage());
                    }
                    isRecording = true;
    }

    private void Stop() {
      if(mMediaRecorder != null) {
        if (isRecording) {
          mMediaRecorder.stopRecord();
        }
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_BITRATE_DOWN);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_BITRATE_RAISE);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_SUCC);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_DATA_DISCARD);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_INIT_DONE);
        //mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_SUCC);
        //mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODER_OPEN_FAILED);
        //mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_VIDEO_ENCODED_FRAMES_FAILED);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_ENCODED_FRAMES_FAILED);
        mMediaRecorder.unSubscribeEvent(AlivcEvent.EventType.EVENT_AUDIO_CAPTURE_OPEN_FAILED);
        mMediaRecorder.reset();
      }
        isRecording = false;
    }
  private String CheckPushStatus(){
    String ret = "";
    if(mMediaRecorder != null && isRecording){
      ret = "0|"+pushStatusString;
    }
    else{
      ret = "-1|" + pushStatusString;
    }
    return ret;
  }

public void testPublish(boolean isPublish, final String url) {
        if(isPublish) {
            mMediaRecorder.startRecord(url);
            Log.d(TAG, "Start Record Time:" + System.currentTimeMillis());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    testPublish(false, url);
                }
            }, 10000);
        }else {
            mMediaRecorder.stopRecord();
            Log.d(TAG, "Stop Record Time:" + System.currentTimeMillis());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    testPublish(true, url);
                }
            }, 500);
        }
    }

	private void PlayStop(){
		if(mQuPlayer != null){
			synchronized (mQuPlayer) {
				_PlayHandler.removeCallbacks(_PlayRestart);
                mQuPlayer.stop();
            }
		}
        _PlayLastBufferQueueTime = 0;
		isPlaying = false;
	}
    private void PlayPause(){
        if(mQuPlayer != null && isPlaying){
            mQuPlayer.pause();
            isPlaying = false;
        }
    }
    private void PlayResume(){
        if(mQuPlayer != null && !isPlaying){
            mQuPlayer.resume();
            isPlaying = true;
        }
    }

    private void PlaySeek(long seekPos){
        if(mQuPlayer != null){
            mQuPlayer.seekTo(seekPos);
        }
    }

    private  String GetPlayDuration(){
        long len = 0;
        if(mQuPlayer != null){
           len =  mQuPlayer.getDuration(); //ms
        }
        return len + "";
    }

    private  String GetCurrentPlayPosition(){
        long pos = 0;
        if(mQuPlayer != null){
            pos = mQuPlayer.getCurrentPosition();
        }
        return pos + "";
    }

	private Runnable _PlayRestart = new Runnable() {
        @Override
        public void run() {
            synchronized (mQuPlayer) {
                Log.d(TAG, "stop stream");
                PlayStop();
                //mQuPlayer.stop();
              //  Log.d(TAG, "start stream");
               // mQuPlayer.start();
            }
        }
    };

	private QuPlayer.OnErrorListener _PlayErrorListener = new QuPlayer.OnErrorListener() {
        @Override
        public void onError(int error) {
            String log = String.format("Error %d\n", error);
            Log.e(TAG, log);
            playStatusString = error + "|播放失败,可能直播还未开始或网络连接问题, 请稍候重试. ";
            callbackContext.error(playStatusString);
            _PlayHandler.postDelayed(_PlayRestart, 3000);
        }
    };
      private QuPlayer.OnSeekCompleteListener _PlaySeekCompleteListener = new QuPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekCompletion() {
            callbackContext.success("Seek成功");
            Log.d(TAG, "Seeking Completion.");
        }
      };

	private QuPlayer.OnInfoListener _PlayInfoListener = new QuPlayer.OnInfoListener() {
        @Override
        public void onStart() {
            Log.d(TAG, "starting ... ");
            //_TestStartTime = System.currentTimeMillis();
			      //callbackContext.success("播放开始");
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stoping ...");
        }

        @Override
        public void onVideoStreamInfo(int width, int height) {
            String log = String.format("Video Stream info width %d height %d \n", width, height);
            Log.d(TAG, log);
          //  Log.d(TAG, "onVideoStreamInfo spend time " + (System.currentTimeMillis() - _TestStartTime));
        }

        @Override
        public void onAndroidBufferQueueCount(int count) {
          if(!isPlayStartFlag) {
            isPlayStartFlag = true;//第一次进这函数, 表明播放已开始了.返回播放开始回调
            callbackContext.success("播放开始");
          }
          _PlayLastBufferQueueTime = System.currentTimeMillis();
          Log.d(TAG, "Android buffer Queue count " + count);
        }

        @Override
        public void onProgress(long progress) {
            String log = String.format("progress %d\n", progress);
            //Log.d(TAG, log); //"onProgress spend time " + (System.currentTimeMillis() - _TestStartTime));
           // callbackContext.success("播放开始_P");
        }
    };
    private String CheckPlayStatus(){
      String ret = "";
      if(mQuPlayer != null && isPlaying){
          ret = mQuPlayer.isPlaying() ? "0|":"-1|";
          if(mQuPlayer.isPlaying()) {
            long timespan = System.currentTimeMillis() - _PlayLastBufferQueueTime;
            if(timespan > 6000){ //已6秒未收到响应.
              ret = "-1|-106|网络错误或未知异常,未收到服务器响应,播放已停止.";
            }
          }
          ret += playStatusString;
      }
      else{
        ret = "-1|" + playStatusString;
      }
      return ret;
    }

	private void Play(){
		  if(mQuPlayer != null && !isPlaying){
            isPlayStartFlag = false;
            playStatusString = "";
            mQuPlayer.setErrorListener(_PlayErrorListener);
            mQuPlayer.setInfoListener(_PlayInfoListener);
            mQuPlayer.setSeekCompleteListener(_PlaySeekCompleteListener);
            //mQuPlayer.setSurface(_Surface);
              DataSpec spec = null;
              if("stream".equals(playMediaType) || "".equals(playMediaType)) {
                spec =  new DataSpec(playUrl, DataSpec.MEDIA_TYPE_STREAM);
              }
              else{
                  //String path, long position, long length, boolean seekable, boolean cacheable, boolean recyclable, int type
                  if(playStartPosition < 0){
                      playStartPosition = 0L;
                  }
                  spec = new DataSpec(playUrl, playStartPosition, 9223372036854775807L, true, true, false, 0);
              }

            _PlayLastBufferQueueTime = System.currentTimeMillis();
            mQuPlayer.setDataSource(spec);
            mQuPlayer.setLooping(true);
            mQuPlayer.prepare();
            mQuPlayer.start();
              isPlaying = true;
        }
	}

    private Handler mHandler = new Handler();

    private OnRecordStatusListener mRecordStatusListener = new OnRecordStatusListener() {
        @Override
        public void onDeviceAttach() {
//            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_AUTO_FOCUS_ON);
        }

        @Override
        public void onDeviceAttachFailed(int facing) {

        }

        @Override
        public void onSessionAttach() {
          //  if (isRecording && !TextUtils.isEmpty(pushUrl)) {
          //      mMediaRecorder.startRecord(pushUrl);
		//		callbackContext.success("续播开始");
          //  }
         //   mMediaRecorder.focusing(0.5f, 0.5f);
        }

        @Override
        public void onSessionDetach() {

        }

        @Override
        public void onDeviceDetach() {

        }

        @Override
        public void onIllegalOutputResolution() {
          //  Log.d(TAG, "selected illegal output resolution");
          //  ToastUtils.showToast(CurrentActivity, "selected illegal output resolution");
        }
    };


    private OnNetworkStatusListener mOnNetworkStatusListener = new OnNetworkStatusListener() {
        @Override
        public void onNetworkBusy() {
            Log.d("network_status", "==== on network busy ====");
            ToastUtils.showToast(CurrentActivity, "当前网络状态极差，已无法正常流畅直播");
            Stop();
            pushStatusString = "-55|当前网络状态极差，已无法正常流畅直播";
			      callbackContext.error(pushStatusString);
        }

        @Override
        public void onNetworkFree() {
            ToastUtils.showToast(CurrentActivity, "network free");
            Log.d("network_status", "===== on network free ====");
        }

        @Override
        public void onConnectionStatusChange(int status) {
            Log.d(TAG, "ffmpeg Live stream connection status-->" + status);

            switch (status) {
                case AlivcStatusCode.STATUS_CONNECTION_START:
                    ToastUtils.showToast(CurrentActivity, "Start live stream connection!");
                    Log.d(TAG, "Start live stream connection!");
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_ESTABLISHED:
                    Log.d(TAG, "Live stream connection is established!");
//                    showIllegalArgumentDialog("链接成功");
                    ToastUtils.showToast(CurrentActivity, "Live stream connection is established!");
					          callbackContext.success("开始直播");
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_CLOSED:
                    Log.d(TAG, "Live stream connection is closed!");
                    ToastUtils.showToast(CurrentActivity, "Live stream connection is closed!");
					          callbackContext.success("直播已停止");
                    break;
            }
        }

//        @Override
//        public void onFirstReconnect() {
//            ToastUtils.showToast(LiveCameraActivity.this, "首次重连");
//        }


        @Override
        public boolean onNetworkReconnectFailed() {
            Log.d(TAG, "Reconnect timeout, not adapt to living");
            ToastUtils.showToast(CurrentActivity, "长时间重连失败，已不适合直播，请退出");
            Stop();
            //showIllegalArgumentDialog("网络重连失败");
            pushStatusString = "-56|网络重连失败, 直播已停止";
			      callbackContext.error(pushStatusString);
            return false;
        }
    };

  /*   @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                boolean hasPermission = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        String toastTip = "";
                        if (Manifest.permission.CAMERA.equals(permissions[i])) {
                            toastTip = "no_camera_permission";
                        } else if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                            toastTip = "no_record_audio_permission";
                        }
                        if (!toastTip.equals("")) {
                            ToastUtils.showToast(CurrentActivity, toastTip);
                            hasPermission = false;
                        }
                    }
                }
                mHasPermission = hasPermission;
                break;
        }
    } */



    public void showIllegalArgumentDialog(String message) {
        if(illegalArgumentDialog == null) {
            illegalArgumentDialog = new AlertDialog.Builder(CurrentActivity)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            illegalArgumentDialog.dismiss();
                        }
                    })
                    .setTitle("提示")
                    .create();
        }
        illegalArgumentDialog.dismiss();
        illegalArgumentDialog.setMessage(message);
        illegalArgumentDialog.show();
    }
    AlertDialog illegalArgumentDialog = null;

    private OnLiveRecordErrorListener mOnErrorListener = new OnLiveRecordErrorListener() {
        @Override
        public void onError(int errorCode) {
            Log.d(TAG, "Live stream connection error-->" + errorCode);

            switch (errorCode) {
                case AlivcStatusCode.ERROR_ILLEGAL_ARGUMENT:
                   // showIllegalArgumentDialog("-22错误产生");
                case AlivcStatusCode.ERROR_SERVER_CLOSED_CONNECTION:
                case AlivcStatusCode.ERORR_OUT_OF_MEMORY:
                case AlivcStatusCode.ERROR_CONNECTION_TIMEOUT:
                case AlivcStatusCode.ERROR_BROKEN_PIPE:
                case AlivcStatusCode.ERROR_IO:
                case AlivcStatusCode.ERROR_NETWORK_UNREACHABLE:
                    ToastUtils.showToast(CurrentActivity, "Live stream connection error-->" + errorCode);

                    break;

                default:
            }
          if(errorCode < 0) {
            Stop();
            pushStatusString = errorCode + "|直播出现错误已停止";
            callbackContext.error(pushStatusString);
          }
        }
    };

    DataStatistics.ReportListener mReportListener = new DataStatistics.ReportListener() {
        @Override
        public void onInfoReport() {
            //runOnUiThread(mLoggerReportRunnable);
        }
    };

    private Runnable mLoggerReportRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRecordReporter != null) {
//                tv_video_capture_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_CAPTURE_FPS) + "fps");
//                tv_audio_encoder_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_ENCODER_FPS) + "fps");
//                tv_video_encoder_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_ENCODER_FPS) + "fps");
//
//                /**
//                 * OUTPUT_BITRATE的单位是byte / s，所以转换成bps需要要乘8
//                 */
//                tv_output_bitrate.setText(mRecordReporter.getLong(AlivcRecordReporter.OUTPUT_BITRATE) * 8 + "bps");
//
//                tv_av_output_diff.setText(mRecordReporter.getLong(AlivcRecordReporter.AV_OUTPUT_DIFF) + "microseconds");
//                tv_audio_out_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_OUTPUT_FPS) + "fps");
//                tv_video_output_fps.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_OUTPUT_FPS) + "fps");
////                tv_stream_publish_time = (TextView) findViewById(R.id.tv_video_capture_fps);
////                tv_stream_server_ip = (TextView) findViewById(R.id.tv_video_capture_fps);
//                tv_video_delay_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_DELAY_DURATION) + "microseconds");
//                tv_audio_delay_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_DELAY_DURATION) + "microseconds");
//                tv_video_cache_frame_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_CACHE_FRAME_CNT) + "");
//                tv_audio_cache_frame_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_CACHE_FRAME_CNT) + "");
//                tv_video_cache_byte_size.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_CACHE_BYTE_SIZE) + "byte");
//                tv_audio_cache_byte_size.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_CACHE_BYTE_SIZE) + "byte");
//                tv_video_frame_discard_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_FRAME_DISCARD_CNT) + "");
//                tv_audio_frame_discard_cnt.setText(mRecordReporter.getInt(AlivcRecordReporter.AUDIO_FRAME_DISCARD_CNT) + "");
//                tv_cur_video_bueaty_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.CUR_VIDEO_BEAUTY_DURATION) + "ms");
//                tv_cur_video_encoder_duration.setText(mRecordReporter.getLong(AlivcRecordReporter.CUR_VIDEO_ENCODER_DURATION) + "ms");
//                tv_cur_video_encode_birate.setText(mRecordReporter.getInt(AlivcRecordReporter.CUR_VIDEO_ENCODE_BITRATE) * 8 + "bps");
//
//                tv_video_output_frame_count.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_OUTPUT_FRAME_COUNT) + "");
//                tv_video_data.setText(mRecordReporter.getLong(AlivcRecordReporter.VIDEO_OUTPUT_DATA_SIZE) + "");
//                tv_video_buffer_count.setText(mRecordReporter.getInt(AlivcRecordReporter.VIDEO_BUFFER_COUNT) + "");
//                tv_audio_data.setText(mRecordReporter.getLong(AlivcRecordReporter.AUDIO_OUTPUT_DATA_SIZE) + "");
            }
        }
    };

    private AlivcEventResponse mBitrateUpRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int preBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_PRE_BITRATE);
            int currBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_CURR_BITRATE);
            Log.d(TAG, "event->up bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
            ToastUtils.showToast(CurrentActivity, "event->up bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
        }
    };
    private AlivcEventResponse mBitrateDownRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int preBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_PRE_BITRATE);
            int currBitrate = bundle.getInt(AlivcEvent.EventBundleKey.KEY_CURR_BITRATE);
            Log.d(TAG, "event->down bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
            ToastUtils.showToast(CurrentActivity, "event->down bitrate, previous bitrate is " + preBitrate +
                    "current bitrate is " + currBitrate);
        }
    };
    private AlivcEventResponse mAudioCaptureSuccRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->audio recorder start success");
            ToastUtils.showToast(CurrentActivity, "event->audio recorder start success");
        }
    };

    private AlivcEventResponse mVideoEncoderSuccRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encoder start success");
            ToastUtils.showToast(CurrentActivity, "event->video encoder start success");
        }
    };
    private AlivcEventResponse mVideoEncoderFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encoder start failed");
            ToastUtils.showToast(CurrentActivity, "event->video encoder start failed");
        }
    };
    private AlivcEventResponse mVideoEncodeFrameFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->video encode frame failed");
            ToastUtils.showToast(CurrentActivity, "event->video encode frame failed");
        }
    };


    private AlivcEventResponse mInitDoneRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event->live recorder initialize completely");
            ToastUtils.showToast(CurrentActivity, "event->live recorder initialize completely");
        }
    };

    private AlivcEventResponse mDataDiscardRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Bundle bundle = event.getBundle();
            int discardFrames = 0;
            if (bundle != null) {
                discardFrames = bundle.getInt(AlivcEvent.EventBundleKey.KEY_DISCARD_FRAMES);
            }
            Log.d(TAG, "event->data discard, the frames num is " + discardFrames);
            ToastUtils.showToast(CurrentActivity, "event->data discard, the frames num is " + discardFrames);
        }
    };

    private AlivcEventResponse mAudioCaptureOpenFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event-> audio capture device open failed");
            ToastUtils.showToast(CurrentActivity, "event-> audio capture device open failed");
			      callbackContext.error("打开麦克风失败, 请检查权限设置!");
        }
    };

    private AlivcEventResponse mAudioEncodeFrameFailedRes = new AlivcEventResponse() {
        @Override
        public void onEvent(AlivcEvent event) {
            Log.d(TAG, "event-> audio encode frame failed");
            ToastUtils.showToast(CurrentActivity, "event-> audio encode frame failed");
        }
    };
}
