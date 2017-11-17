//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.alibaba.livecloud.live;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import com.alibaba.livecloud.beauty.AlivcBeautyRender;
import com.alibaba.livecloud.event.AlivcEventDispatcher;
import com.alibaba.livecloud.event.AlivcEventSubscriber;
import com.alibaba.livecloud.live.AlivcFlagAction;
import com.alibaba.livecloud.live.AlivcFlagController;
import com.alibaba.livecloud.live.AlivcFlagManager;
import com.alibaba.livecloud.live.AlivcLiveRecordReporter;
import com.alibaba.livecloud.live.AlivcMediaRecorder;
import com.alibaba.livecloud.live.AlivcRecordConfig;
import com.alibaba.livecloud.live.AlivcRecordReporter;
import com.alibaba.livecloud.live.OnLiveRecordErrorListener;
import com.alibaba.livecloud.live.OnNetworkStatusListener;
import com.alibaba.livecloud.live.OnRecordStatusListener;
import com.alibaba.livecloud.live.Resolution;
import com.alibaba.livecloud.model.AlivcWatermark;
import com.duanqu.qupai.android.camera.CameraClient;
import com.duanqu.qupai.android.camera.CameraSurfaceController;
import com.duanqu.qupai.android.camera.SessionRequest;
import com.duanqu.qupai.android.camera.CameraClient.Callback;
import com.duanqu.qupai.android.camera.CameraClient.OnErrorListener;
import com.duanqu.qupai.android.camera.CaptureRequest.OnCaptureRequestResultListener;
import com.duanqu.qupai.android.permission.PermissionChecker;
import com.duanqu.qupai.live.LiveAudioStream;
import com.duanqu.qupai.live.LiveVideoStream;
import com.duanqu.qupai.logger.DataStatistics;
import com.duanqu.qupai.logger.DataStatisticsModel;
import com.duanqu.qupai.logger.IOUtil;
import com.duanqu.qupai.logger.data.collect.DataCollectTrunk;
import com.duanqu.qupai.logger.data.collect.DataTrunkStatistics;
import com.duanqu.qupai.logger.data.collect.LoggerConstant;
import com.duanqu.qupai.logger.event.LiveEventBus;
import com.duanqu.qupai.quirks.Quirk;
import com.duanqu.qupai.quirks.QuirksStorage;
import com.duanqu.qupai.recorder.AudioPacketWriter;
import com.duanqu.qupai.recorder.RecorderTask;
import com.duanqu.qupai.recorder.RecorderTask.OnFeedbackListener;
import com.duanqu.qupai.utils.MathUtil;
import java.util.Map;

public class AudioAlivcMediaRecorder implements AlivcMediaRecorder {
    private static final byte STATUS_PREPARED = 1;
    private static final byte STATUS_INITIALIZED = 3;
    private static final byte STATUS_RELEASED = 4;
    private static final byte NETWORK_RECONNECTING = 1;
    private static final byte NETWORK_NOT_PERMIT_RECONNECT = 2;
    private static final byte NETWORK_NO_NEED_TO_RECONNECT = 3;
    private static final byte NETWORK_RECONNECT_FAILED = 4;
    private boolean mPendingReconnect = false;
    private CameraClient mCameraClient;
    private RecorderTask mRecorderTask;
    private LiveVideoStream mVideoStream;
    private LiveAudioStream mAudioStream;
    private CameraSurfaceController mSurfaceController;
    private OnLiveRecordErrorListener mRecordErrorListener;
    private OnNetworkStatusListener mNetworkStatusListener;
    private OnRecordStatusListener mRecordStatusListener;
    private AlivcRecordConfig mConfig;
    private AlivcBeautyRender mBeautyRenderer;
    private AudioPacketWriter mAudioWriter;
    private Surface mPreviewSurface;
    private final Resolution mPreviewResolution = new Resolution();
    private volatile int mStatus = 4;
    private boolean isRecording = false;
    private boolean mRecTimeoutCalculating = false;
    private boolean mPendingTimeout = false;
    private Context mContext;
    private IntentFilter mConnectIntentFilter;
    private byte mConnectionStatus = 3;
    private Object mConnectionStatusLock = new Object();
    private boolean mCameraOpen = false;
    private DataTrunkStatistics mDataTrunkStatistics;
    private DataStatistics mBitrateDataStatistics;
    private boolean isPreviewOpen = false;
    private AlivcRecordReporter mReporter;
    private boolean mCurrNetworkBusy = false;
    private long mStartTime;
    private AlivcFlagController mFlagController;
    private AlivcFlagManager mFlagManager;
    private AlivcEventDispatcher mEventDispatcher = new AlivcEventDispatcher();
    DataStatisticsModel mLogStatisticsModel;
    DataStatistics mLogDataStatistics;
    long videoFrameCount;
    long lastVideoFrameCount;
    private int mNetWorkBusyCount = 0;
    OnFeedbackListener mRecorderTaskFeedback = new OnFeedbackListener() {
        public void OnNetworkBandwidth(RecorderTask task, int status) {
            Log.d("LiveRecord", "====bufferTest,network band width status = " + status + "=======");
            if(status == 1 && AudioAlivcMediaRecorder.this.mVideoStream != null) {
                AudioAlivcMediaRecorder.this.mVideoStream.onNetworkFree();
                if(AudioAlivcMediaRecorder.this.mNetworkStatusListener != null && AudioAlivcMediaRecorder.this.mCurrNetworkBusy) {
                    AudioAlivcMediaRecorder.this.mNetworkStatusListener.onNetworkFree();
                    AudioAlivcMediaRecorder.this.mCurrNetworkBusy = false;
                }

                AudioAlivcMediaRecorder.this.mNetWorkBusyCount = 0;
            } else {
                if(AudioAlivcMediaRecorder.this.mVideoStream != null) {
                    AudioAlivcMediaRecorder.this.mVideoStream.onNetworkBusy(status);
                }

                if(status >= 3) {
                   // AudioAlivcMediaRecorder.access$308(AudioAlivcMediaRecorder.this);
                }

                if(AudioAlivcMediaRecorder.this.mNetWorkBusyCount >= 3 && !AudioAlivcMediaRecorder.this.mCurrNetworkBusy) {
                    if(AudioAlivcMediaRecorder.this.mNetworkStatusListener != null) {
                        AudioAlivcMediaRecorder.this.mNetworkStatusListener.onNetworkBusy();
                        AudioAlivcMediaRecorder.this.mCurrNetworkBusy = true;
                    }

                    AudioAlivcMediaRecorder.this.mNetWorkBusyCount = 0;
                }
            }

        }

        public void OnStatusChange(RecorderTask task, int status) {
            Log.d("LiveRecord", " ===== status = " + status + "======");
            if(AudioAlivcMediaRecorder.this.isRecording) {
                if(status > 0) {
                    switch(status) {
                        case 2:
                            synchronized(AudioAlivcMediaRecorder.this.mConnectionStatusLock) {
                                if(AudioAlivcMediaRecorder.this.mConnectionStatus == 1) {
                                    AudioAlivcMediaRecorder.this.mConnectionStatus = 3;
                                    AudioAlivcMediaRecorder.this.stopReconnectTimeoutCal();
                                }
                                break;
                            }
                        case 4:
                            this.checkReconnect();
                    }

                    if(AudioAlivcMediaRecorder.this.mNetworkStatusListener != null) {
                        AudioAlivcMediaRecorder.this.mNetworkStatusListener.onConnectionStatusChange(status);
                    }
                } else {
                    if(AudioAlivcMediaRecorder.this.mRecordErrorListener != null) {
                        AudioAlivcMediaRecorder.this.mRecordErrorListener.onError(status);
                    }

                    switch(status) {
                        case -1313558101:
                            break;
                        case -110:
                        case -104:
                        case -101:
                        case -32:
                        case -22:
                        case -12:
                        case -5:
                        case -1:
                        default:
                            this.checkReconnect();
                    }
                }
            }

        }

        private void checkReconnect() {
            if(AudioAlivcMediaRecorder.this.mConnectionStatus == 1) {
                AudioAlivcMediaRecorder.this.mConnectionStatus = 4;
                if(AudioAlivcMediaRecorder.this.mPendingTimeout) {
                    AudioAlivcMediaRecorder.this.mReconnectRun.run();
                    if(AudioAlivcMediaRecorder.this.mConnectionStatus != 2) {
                        if(AudioAlivcMediaRecorder.this.hasNetwork()) {
                            if(!AudioAlivcMediaRecorder.this.mRecTimeoutCalculating) {
                                AudioAlivcMediaRecorder.this.startReconnectTimeoutCal();
                            }

                            AudioAlivcMediaRecorder.this.mConnectionStatus = 1;
                            AudioAlivcMediaRecorder.this.reconnect();
                        } else {
                            AudioAlivcMediaRecorder.this.mPendingReconnect = true;
                        }
                    }
                } else if(AudioAlivcMediaRecorder.this.hasNetwork()) {
                    AudioAlivcMediaRecorder.this.mConnectionStatus = 1;
                    AudioAlivcMediaRecorder.this.reconnect();
                } else {
                    AudioAlivcMediaRecorder.this.mPendingReconnect = true;
                }
            } else if(AudioAlivcMediaRecorder.this.mConnectionStatus == 3) {
                if(AudioAlivcMediaRecorder.this.hasNetwork()) {
                    AudioAlivcMediaRecorder.this.startReconnectTimeoutCal();
                    AudioAlivcMediaRecorder.this.mConnectionStatus = 1;
                    AudioAlivcMediaRecorder.this.reconnect();
                } else {
                    AudioAlivcMediaRecorder.this.mPendingReconnect = true;
                }
            }

        }
    };
    OnErrorListener mCameraErrorListener = new OnErrorListener() {
        public void onError(CameraClient client, int id) {
            synchronized(this) {
                AudioAlivcMediaRecorder.this.mCameraOpen = false;
            }

            AudioAlivcMediaRecorder.this.reset();
            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onDeviceAttachFailed(id);
            }

        }

        public void onIllegalOutputResolution(int maxSupportedWidth, int maxSupportedHeight) {
            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onIllegalOutputResolution();
            }

        }
    };
    Callback mCameraCallback = new Callback() {
        public void onDeviceAttach(CameraClient client) {
            SessionRequest request = client.getSessionRequest();
            if(AudioAlivcMediaRecorder.this.mConfig.getExposureCompensation() != -1) {
                if(AudioAlivcMediaRecorder.this.mConfig.getExposureCompensation() < 0) {
                    AudioAlivcMediaRecorder.this.mConfig.mExposureCompensation = 0;
                } else if(AudioAlivcMediaRecorder.this.mConfig.getExposureCompensation() > 100) {
                    AudioAlivcMediaRecorder.this.mConfig.mExposureCompensation = 100;
                }

                request.mExposureCompensation = client.getCharacteristics().maxExposureCompensation * AudioAlivcMediaRecorder.this.mConfig.getExposureCompensation() / 100;
            }

            request.previewFrameRate = AudioAlivcMediaRecorder.this.mConfig.mFrameRate;
            AudioAlivcMediaRecorder.this.mCameraClient.setFlashMode(AudioAlivcMediaRecorder.this.flashMode);
            synchronized(this) {
                AudioAlivcMediaRecorder.this.mCameraOpen = true;
            }

            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onDeviceAttach();
            }

        }

        public void onSessionAttach(CameraClient client) {
            AudioAlivcMediaRecorder.this.mStatus = 1;
            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onSessionAttach();
            }

        }

        public void onCaptureUpdate(CameraClient client) {
        }

        public void onSessionDetach(CameraClient client) {
            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onSessionDetach();
            }

        }

        public void onDeviceDetach(CameraClient client) {
            AudioAlivcMediaRecorder.this.mStatus = 3;
            synchronized(this) {
                AudioAlivcMediaRecorder.this.mCameraOpen = false;
            }

            if(AudioAlivcMediaRecorder.this.mRecordStatusListener != null) {
                AudioAlivcMediaRecorder.this.mRecordStatusListener.onDeviceDetach();
            }

        }

        public void onFrameBack(CameraClient client) {
        }
    };
    AlivcFlagAction beautyAction = new AlivcFlagAction() {
        public Object doAction(boolean flag) {
            if(AudioAlivcMediaRecorder.this.mStatus == 3 || AudioAlivcMediaRecorder.this.mStatus == 1) {
                AudioAlivcMediaRecorder.this.mBeautyRenderer.switchBeauty(flag);
            }

            return null;
        }
    };
    private String flashMode = "off";
    AlivcFlagAction flashModeAction = new AlivcFlagAction() {
        public Object doAction(boolean flag) {
            if(AudioAlivcMediaRecorder.this.mStatus == 1) {
                if(!AudioAlivcMediaRecorder.this.mCameraOpen) {
                    return null;
                }

                if(flag) {
                    AudioAlivcMediaRecorder.this.flashMode = "torch";
                    AudioAlivcMediaRecorder.this.mCameraClient.setFlashMode(AudioAlivcMediaRecorder.this.flashMode);
                } else {
                    AudioAlivcMediaRecorder.this.flashMode = "off";
                    AudioAlivcMediaRecorder.this.mCameraClient.setFlashMode(AudioAlivcMediaRecorder.this.flashMode);
                }
            }

            return null;
        }
    };
    AlivcFlagAction muteAction = new AlivcFlagAction() {
        public Object doAction(boolean flag) {
            if(AudioAlivcMediaRecorder.this.mAudioStream != null) {
                AudioAlivcMediaRecorder.this.mAudioStream.setMute(flag);
            }

            return null;
        }
    };
    private Handler mHandler = new Handler();
    private Runnable mReconnectRun = new Runnable() {
        public void run() {
            AudioAlivcMediaRecorder.this.mPendingTimeout = false;
            if(AudioAlivcMediaRecorder.this.mConnectionStatus == 1) {
                AudioAlivcMediaRecorder.this.mPendingTimeout = true;
            } else if(AudioAlivcMediaRecorder.this.mConnectionStatus == 4) {
                AudioAlivcMediaRecorder.this.mRecTimeoutCalculating = false;
                if(AudioAlivcMediaRecorder.this.mNetworkStatusListener != null && !AudioAlivcMediaRecorder.this.mNetworkStatusListener.onNetworkReconnectFailed()) {
                    AudioAlivcMediaRecorder.this.mConnectionStatus = 2;
                    AudioAlivcMediaRecorder.this.mPendingTimeout = false;
                    AudioAlivcMediaRecorder.this.mPendingReconnect = false;
                }
            }

        }
    };
    AlivcFlagAction autoFocusAction = new AlivcFlagAction() {
        public Object doAction(boolean flag) {
            if(AudioAlivcMediaRecorder.this.mCameraClient != null) {
                SessionRequest req = AudioAlivcMediaRecorder.this.mCameraClient.getSessionRequest();
                if(!flag) {
                    AudioAlivcMediaRecorder.this.mCameraClient.setFocusMode("continuous-video", (OnCaptureRequestResultListener)null);
                    if(req != null) {
                        req.mFocusMode = "continuous-video";
                    }
                } else {
                    if(req != null) {
                        req.mFocusMode = "auto";
                    }

                    AudioAlivcMediaRecorder.this.mCameraClient.setFocusMode("auto", (OnCaptureRequestResultListener)null);
                }
            }

            return null;
        }
    };
    BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if(AudioAlivcMediaRecorder.this.hasNetwork() && AudioAlivcMediaRecorder.this.isRecording && AudioAlivcMediaRecorder.this.mPendingReconnect) {
                synchronized(AudioAlivcMediaRecorder.this.mConnectionStatusLock) {
                    AudioAlivcMediaRecorder.this.startReconnectTimeoutCal();
                    AudioAlivcMediaRecorder.this.reconnect();
                    AudioAlivcMediaRecorder.this.mPendingReconnect = false;
                }
            }

        }
    };

    public AudioAlivcMediaRecorder() {
    }

    public void init(Context context) {
        if(this.mStatus == 4) {
            LiveEventBus.getInstance().specifyDispatcher(this.mEventDispatcher);
            LoggerConstant.DEBUG = IOUtil.readBooleanMetaData(context, "ALIVC_DEBUG", false);
            if(!LoggerConstant.DEBUG) {
                this.mReporter = new AlivcLiveRecordReporter();
                this.mDataTrunkStatistics = new DataTrunkStatistics(1000);
                this.mDataTrunkStatistics.start();
            }

            this.mContext = context.getApplicationContext();
            this.mCameraClient = null; //new CameraClient();
//            this.mCameraClient.setCallback(this.mCameraCallback);
//            this.mCameraClient.setOnErrorListener(this.mCameraErrorListener);
//            this.mBeautyRenderer = new AlivcBeautyRender();
//            this.mBeautyRenderer.initRenderer(context.getAssets(), this.mCameraClient);
//            this.mBeautyRenderer.switchBeauty(false);
            this.mFlagController = new AlivcFlagController();
            this.mFlagManager = new AlivcFlagManager();
//            this.mFlagController.put(1, this.beautyAction);
//            this.mFlagController.put(8, this.flashModeAction);
//            this.mFlagController.put(4, this.autoFocusAction);
            this.mFlagController.put(16, this.muteAction);
            this.mFlagManager.setFlagController(this.mFlagController);
            this.mConnectIntentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            this.mContext.registerReceiver(this.mConnectionReceiver, this.mConnectIntentFilter);
            this.mStatus = 3;
            LiveEventBus.getInstance().dispatchEvent(4, (Bundle)null);
        }

    }

    public void prepare(Map<String, Object> params, Surface previewSurface) {
        synchronized(this) {
            if(this.mStatus == 1) {
                this.reset();
            }

            if(this.mStatus == 3) {
//                this.mPreviewSurface = previewSurface;
//
//                try {
//                    this.mSurfaceController = this.mCameraClient.addSurface(previewSurface);
//                } catch (Exception var9) {
//                    ;
//                }

//                if(this.mPreviewResolution.size() > 0) {
//                    this.mSurfaceController.setResolution(this.mPreviewResolution.getWidth(), this.mPreviewResolution.getHeight());
//                }

//                this.mSurfaceController.setDisplayMethod(32);
//                int displayMethod = this.mSurfaceController.getDisplayMethod();
//                if(this.mCameraClient.isFrontCamera() && QuirksStorage.getBoolean(Quirk.FRONT_CAMERA_PREVIEW_DATA_MIRRORED)) {
//                    this.mSurfaceController.setDisplayMethod(displayMethod | 128);
//                } else if((displayMethod & 128) != 0) {
//                    this.mSurfaceController.setDisplayMethod(displayMethod ^ 128);
//                }

               // this.mSurfaceController.setVisible(true);
                WindowManager wm = (WindowManager)this.mContext.getSystemService("window");
                Point size = new Point();
                wm.getDefaultDisplay().getSize(size);
                this.mConfig = AlivcRecordConfig.newInstance(params, new Resolution(size.x, size.y), wm.getDefaultDisplay().getRotation());
//                AlivcWatermark watermark = this.mConfig.getWatermark();
//                if(watermark != null) {
//                    this.mBeautyRenderer.setWatermark(watermark.getWatermarkUrl(), watermark.getPaddingX(), watermark.getPaddingY(), watermark.getSite());
//                }

//                this.mCameraClient.setDisplayRotation(this.mConfig.getDisplayRotation());
//                this.mCameraClient.setContentSize(this.mConfig.getOutputSize().getWidth(), this.mConfig.getOutputSize().getHeight());
//                this.mCameraClient.setCameraFacing(this.mConfig.getCameraInitialFacing());
//                this.mCameraClient.startPreview();
//                if(this.mVideoStream != null) {
//                    this.mVideoStream.setMirrored(this.mCameraClient.isFrontCamera());
//                }
            } else if(this.mStatus != 1) {
                throw new IllegalStateException("This method could be called after init()");
            }

        }
    }

    private void internalStart(String outputUrl, boolean isReconnect) {
        synchronized(this) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var6) {
                var6.printStackTrace();
            }

            Log.d("FFMPEG_NULL", "=== enter startRecord, isRecording = " + this.isRecording + "====");
            if(!this.isRecording) {
                Log.d("AlivcMediaRecorder", "====== start record =======");
                this.mConfig.setOutputUrl(outputUrl);
                this.mRecorderTask = new RecorderTask(true);
                String qpUrl = this.concatQpUrl(outputUrl);
                this.mRecorderTask.setOutPut(qpUrl, "flv");
                this.mRecorderTask.setMetaData("qpl-version-android", "qpl-version-160519");
                this.mRecorderTask.setThreshhold(15, 5, 90);
                this.mRecorderTask.setFeedbackListener(this.mRecorderTaskFeedback);
//                this.mVideoStream = new LiveVideoStream(this.mRecorderTask);
//                this.mVideoStream.init(this.mConfig.getOutputSize().getWidth(), this.mConfig.getOutputSize().getHeight(), this.mConfig.getInitVideoBitRate(), this.mConfig.getFrameRate(), this.mConfig.getIFrameInternal(), this.mCameraClient, 1);
//                this.mVideoStream.setMirrored(this.mCameraClient.isFrontCamera());
//                this.mVideoStream.setInput(this.mCameraClient);
//                this.mVideoStream.setBitRateRange(this.mConfig.getBestVideoBitrate(), this.mConfig.getMinVideoBitrate(), this.mConfig.getMaxVideoBitRate());
                this.mAudioStream = new LiveAudioStream((PermissionChecker)null);
                this.mAudioStream.init(this.mConfig.getAudioSampleRate(), this.mConfig.getAudioBitrate(), this.mFlagManager.isContainFlag(16));
                this.mStartTime = System.nanoTime() / 1000L;
//                this.mVideoStream.start(this.mStartTime);
                this.mAudioWriter = new AudioPacketWriter(this.mRecorderTask);
                this.mAudioStream.start(this.mAudioWriter, this.mStartTime);
                this.mRecorderTask.start((String)null);
                this.isRecording = true;
                this.startPushLogStores(qpUrl);
                if(!isReconnect) {
                    this.mConnectionStatus = 3;
                    this.mPendingReconnect = false;
                    this.mPendingTimeout = false;
                }
            }

        }
    }

    public void startRecord(String outputUrl) {
        this.internalStart(outputUrl, false);
    }

    private void startPushLogStores(String outputUrl) {
    }

    private void stopPushLogStores() {
        this.mLogStatisticsModel = null;
        this.mLogDataStatistics.stop();
    }

    public int switchCamera() {
        synchronized(this) {
            if(this.mStatus == 1) {
                if(this.isRecording) {
                    this.mVideoStream.stopMediaCodec();
                }

                this.mCameraClient.nextCamera();
                if(this.isRecording) {
                    this.mVideoStream.setMirrored(this.mCameraClient.isFrontCamera());
                    this.mVideoStream.startMediaCodec(this.mCameraClient);
                }

                int displayMethod = this.mSurfaceController.getDisplayMethod();
                if(this.mCameraClient.isFrontCamera() && QuirksStorage.getBoolean(Quirk.FRONT_CAMERA_PREVIEW_DATA_MIRRORED)) {
                    this.mSurfaceController.setDisplayMethod(displayMethod | 128);
                } else if((displayMethod & 128) != 0) {
                    this.mSurfaceController.setDisplayMethod(displayMethod ^ this.mSurfaceController.getDisplayMethod());
                }
            }

            return this.mCameraClient.getCameraID();
        }
    }

    public void stopRecord() {
        this.internalStop(false);
    }

    private void internalStop(boolean isReconnect) {
        synchronized(this) {
            if(this.isRecording) {
                Log.d("FFMPEG_NULL", " ===== stop record ======");
                if(this.mVideoStream != null) {
                    this.mVideoStream.stop();
                    this.mVideoStream = null;
                }

                if(this.mAudioStream != null) {
                    this.mAudioStream.stop();
                    this.mAudioStream = null;
                }

                if(this.mAudioWriter != null) {
                    this.mAudioWriter.writeEOS();
                    this.mAudioWriter = null;
                }

                if(this.mRecorderTask != null) {
                    this.mRecorderTask.stop();
                    this.mRecorderTask.dispose();
                    this.mRecorderTask = null;
                }

                this.isRecording = false;
                if(!isReconnect) {
                    DataCollectTrunk.getInstance().reset();
                    this.mDataTrunkStatistics.reset();
                    this.stopReconnectTimeoutCal();
                }
            }

        }
    }

    public void focusing(float xRatio, float yRatio) {
        if(this.mStatus == 1) {
            this.mCameraClient.autoFocus(xRatio, yRatio, this.mSurfaceController);
        }

    }

    public void setZoom(float scaleFactor, OnCaptureRequestResultListener listener) {
        if(this.mStatus == 1) {
            float zoom = this.mCameraClient.getZoomRatio() * scaleFactor;
            this.mCameraClient.setZoom(MathUtil.clamp(zoom, 1.0F, (float)this.mConfig.getMaxZoomLevel()));
        }

    }

    public void setZoom(float scaleFactor) {
        if(this.mStatus == 1) {
            float zoom = this.mCameraClient.getZoomRatio() * scaleFactor;
            this.mCameraClient.setZoom(MathUtil.clamp(zoom, 1.0F, (float)this.mConfig.getMaxZoomLevel()));
        }

    }

    public void setPreviewSize(int width, int height) {
        if(this.mSurfaceController != null && this.mContext != null) {
            switch(this.mConfig.getDisplayRotation()) {
                case 0:
                case 180:
                default:
                    this.mPreviewResolution.setWidth(width);
                    this.mPreviewResolution.setHeight(height);
                    this.mSurfaceController.setResolution(width, height);
                    break;
                case 90:
                case 270:
                    WindowManager wm = (WindowManager)this.mContext.getSystemService("window");
                    switch(wm.getDefaultDisplay().getRotation()) {
                        case 0:
                        case 2:
                            this.mPreviewResolution.setWidth(height);
                            this.mPreviewResolution.setHeight(width);
                            this.mSurfaceController.setResolution(height, width);
                            break;
                        case 1:
                        case 3:
                            this.mPreviewResolution.setWidth(width);
                            this.mPreviewResolution.setHeight(height);
                            this.mSurfaceController.setResolution(width, height);
                    }
            }
        }

    }

    public void addFlag(int flag) {
        if(this.mFlagManager != null) {
            this.mFlagManager.addFlag(flag);
        }

    }

    public boolean isFlagSupported(int flag) {
        if(this.mCameraOpen) {
            switch(flag) {
                case 1:
                    return true;
                case 2:
                    return false;
                case 3:
                case 5:
                case 6:
                case 7:
                default:
                    return false;
//                case 4:
//                    return this.mCameraClient.isFocusModeSupported("auto");
//                case 8:
//                    return this.mCameraClient.isFlashSupported("torch");
            }
        } else {
            throw new IllegalStateException("This method could be called after device attached");
        }
    }

    public void removeFlag(int flag) {
        if(this.mStatus == 1) {
            this.mFlagManager.removeFlag(flag);
        }

    }

    public void reset() {
        synchronized(this) {
            if(this.mStatus == 1) {
                Log.d("AlivcMediaRecorder", "====== reset =======");
//                if(this.mSurfaceController != null) {
//                    this.mSurfaceController.setVisible(false);
//                }
//
//                if(this.mCameraClient != null) {
//                    this.mCameraClient.removeSurface(this.mPreviewSurface);
//                    this.mCameraClient.stopPreview();
//                }
            }

        }
    }

    public void release() {
        synchronized(this) {
            LiveEventBus.getInstance().unSpecifyDispatcher();
            this.mFlagManager.removeAllFlag();
            if(this.mDataTrunkStatistics != null) {
                this.mDataTrunkStatistics.cancel();
            }

            if(this.isRecording) {
                this.stopRecord();
            }

            if(this.mStatus == 1) {
                this.reset();
            }

            if(this.mStatus == 3) {
                this.mContext.unregisterReceiver(this.mConnectionReceiver);
//                this.mCameraClient.setCallback((Callback)null);
//                this.mCameraClient.setOnErrorListener((OnErrorListener)null);
//                this.mCameraClient.onDestroy();
                this.mCameraClient = null;
                this.mStatus = 4;
            }

            this.mReporter = null;
            this.mFlagController.clear();
            this.mFlagController = null;
        }
    }

    public void setOnRecordErrorListener(OnLiveRecordErrorListener listener) {
        this.mRecordErrorListener = listener;
    }

    public void setOnRecordStatusListener(OnRecordStatusListener listener) {
        this.mRecordStatusListener = listener;
    }

    public void setOnNetworkStatusListener(OnNetworkStatusListener listener) {
        this.mNetworkStatusListener = listener;
    }

    public AlivcRecordReporter getRecordReporter() {
        return this.mReporter;
    }

    public void subscribeEvent(AlivcEventSubscriber subscriber) {
        this.mEventDispatcher.subscribe(subscriber);
    }

    public void unSubscribeEvent(int eventType) {
        this.mEventDispatcher.unSubscribe(eventType);
    }

    public String getVersionName() {
        return "1.2.0";
    }

    private void reconnect() {
        Log.d("LiveRecord", "======do reconnect =====");
        if(this.mStatus == 1) {
            this.internalStop(true);
            this.internalStart(this.mConfig.getOutputUrl(), true);
        }

    }

    private String concatQpUrl(String originalUrl) {
        if(!TextUtils.isEmpty(originalUrl)) {
            String qpUrl;
            if(originalUrl.indexOf("?") != -1) {
                qpUrl = originalUrl + "&from=QP";
            } else {
                qpUrl = originalUrl + "?from=QP";
            }

            return qpUrl;
        } else {
            return null;
        }
    }

    private void startReconnectTimeoutCal() {
        if(!this.mRecTimeoutCalculating && this.mConfig != null) {
            this.mRecTimeoutCalculating = true;
            this.mHandler.postDelayed(this.mReconnectRun, this.mConfig.getReconnectTimeout());
        }

    }

    private void stopReconnectTimeoutCal() {
        this.mHandler.removeCallbacks(this.mReconnectRun);
        this.mRecTimeoutCalculating = false;
        this.mPendingTimeout = false;
    }

    private boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager)this.mContext.getSystemService("connectivity");
        NetworkInfo network = cm.getActiveNetworkInfo();
        return network != null && network.isConnected();
    }
}
