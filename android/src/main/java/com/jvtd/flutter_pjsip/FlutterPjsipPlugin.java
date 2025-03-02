package com.jvtd.flutter_pjsip;

import static android.media.AudioManager.MODE_RINGTONE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
//import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jvtd.flutter_pjsip.entity.MSG_TYPE;
import com.jvtd.flutter_pjsip.entity.MyBuddy;
import com.jvtd.flutter_pjsip.entity.MyCall;
import com.jvtd.flutter_pjsip.interfaces.MyAppObserver;
import com.jvtd.flutter_pjsip.utils.SoundPoolUtil;

import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;
import org.pjsip.pjsua2.pjsip_status_code;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * FlutterPjsipPlugin
 */
public class FlutterPjsipPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler
{
  private static final String TAG = "FlutterPjsipPlugin";

  private static final String CHANNEL = "flutter_pjsip";
  private static final String METHOD_PJSIP_INIT = "method_pjsip_init";
  private static final String METHOD_PJSIP_LOGIN = "method_pjsip_login";
  private static final String METHOD_PJSIP_CALL = "method_pjsip_call";
  private static final String METHOD_PJSIP_LOGOUT = "method_pjsip_logout";
  private static final String METHOD_PJSIP_DEINIT = "method_pjsip_deinit";
  private static final String METHOD_PJSIP_RECEIVE = "method_pjsip_receive";
  private static final String METHOD_PJSIP_REFUSE = "method_pjsip_refuse";
  private static final String METHOD_PJSIP_HANDS_FREE = "method_pjsip_hands_free";
  private static final String METHOD_PJSIP_MUTE = "method_pjsip_mute";

  private static final String METHOD_PJSIP_LOGIN_WITH_INFO = "method_pjsip_login_with_info";
  private static final String METHOD_PJSIP_TERMINATE_ALL_CALLS = "method_pjsip_terminate_all_calls";
  private static final String METHOD_PJSIP_MUTE2 = "method_pjsip_mute2";

  private static final String METHOD_CALL_STATUS_CHANGED = "method_call_state_changed";
  private static final String METHOD_CALL_REGISTER_ANOTHER_ACCOUNT = "method_call_register_another_account";
  private static final String METHOD_CALL_REGISTER_SUCCESSFUL = "method_call_register_successful";


  private MethodChannel mChannel;
  private Activity mActivity;
  private Result mResult;
  private String mMethod;
  private MyBroadcastReceiver mReceiver;
  private String mIp;// sip服务器的IP
  private String mPort;// sip服务器的端口号
  private MyCall mCurrentCall;// 记录当前通话，若没有通话，为null

  private AudioManager mAudioManager;
  private MediaPlayer mRingerPlayer;
  private boolean mAudioFocused;

  private SoundPoolUtil mSoundPoolUtil;
  private int mSoundWaitId;
//  private TelephonyManager mTelephonyManager;
//  private SystemPhoneStateListener mSystemPhoneStateListener;
  private PowerManager.WakeLock mWakeLock;
  private SensorManager mSensorManager;
  private Vibrator mVibrator;

  private PjSipManagerState mPjSipManagerState = PjSipManagerState.STATE_UNDEFINED;
  private PjSipManager mPjSipManager = PjSipManager.getInstance();

  private MyAppObserver mAppObserver = new MyAppObserver()
  {
    @Override
    public void notifyRegState(final pjsip_status_code code, String reason, final int expiration)
    {
      if (TextUtils.equals(mMethod, METHOD_PJSIP_LOGIN) || TextUtils.equals(mMethod, METHOD_PJSIP_LOGIN_WITH_INFO))
      {
//        String msg_str = "";
//        if (expiration == 0)// 注销
//          msg_str += "Unregistration";
//        else// 注册
//          msg_str += "Registration";
        boolean loginResult = code.swigValue() / 100 == 2;
        mMethod = "";

        Message m = Message.obtain(handler, MSG_TYPE.REG_STATE, loginResult);
        m.sendToTarget();
      }
    }

    @Override
    public void notifyIncomingCall(MyCall call)
    {
      Message m = Message.obtain(handler, MSG_TYPE.INCOMING_CALL, call);
      m.sendToTarget();
    }

    @Override
    public void notifyCallState(MyCall call)
    {
      if (mCurrentCall == null || call.getId() != mCurrentCall.getId()) return;
      CallInfo info = null;
      try
      {
        info = call.getInfo();
      } catch (Exception e)
      {
        e.printStackTrace();
      }

      if (info != null)
      {
        Message m = Message.obtain(handler, MSG_TYPE.CALL_STATE, info);
        m.sendToTarget();
      }
    }

    @Override
    public void notifyCallMediaState(MyCall call)
    {
      Message m = Message.obtain(handler, MSG_TYPE.CALL_MEDIA_STATE, null);
      m.sendToTarget();
    }

    @Override
    public void notifyBuddyState(MyBuddy buddy)
    {
      Message m = Message.obtain(handler, MSG_TYPE.BUDDY_STATE, buddy);
      m.sendToTarget();
    }

    @Override
    public void notifyChangeNetwork()
    {
      Message m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null);
      m.sendToTarget();
    }
  };

  private final Handler handler = new Handler(new Handler.Callback()
  {
    @Override
    public boolean handleMessage(Message msg)
    {
      if (mResult == null) return false;
      int what = msg.what;
      switch (what)
      {
        case MSG_TYPE.REG_STATE:
          boolean loginResult = (boolean) msg.obj;
          // if register successful, notify success via method channel
          if (loginResult) {
            mPjSipManagerState = PjSipManagerState.STATE_LOGINED;
            if (mChannel != null)
            {
              Log.i(TAG, "FlutterPjsipPlugin" + "REGISTER_SUCCESS");
              mChannel.invokeMethod(METHOD_CALL_REGISTER_SUCCESSFUL, buildArguments("REGISTER_SUCCESS", ""));
              if (mSoundPoolUtil == null) {
                pjsipHandsFree(true);
                mSoundPoolUtil = new SoundPoolUtil(mActivity, new SoundPool.OnLoadCompleteListener()
                {
                  @Override
                  public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
                  {
                    if (mSoundPoolUtil != null)
                      mSoundPoolUtil.play(mSoundWaitId);
                  }
                });
                int rawId = R.raw.ring_back_sound;
                mSoundWaitId = mSoundPoolUtil.load(rawId);
              }
            }
          } else {
            // if register failed, notify register another account via method channel
            // you can do next process on this state
            // ex: register new account or notify user and close the call view
            if (mChannel != null)
            {
              Log.i(TAG, "FlutterPjsipPlugin" + "register failed, notify REGISTER_ANOTHER_ACCOUNT state to flutter");
              mChannel.invokeMethod(METHOD_CALL_REGISTER_ANOTHER_ACCOUNT, buildArguments("REGISTER_ANOTHER_ACCOUNT", ""));
            }
          }
          mResult.success(loginResult);
          break;

        case MSG_TYPE.CALL_STATE:
          CallInfo callInfo = (CallInfo) msg.obj;
          if (mCurrentCall == null || callInfo == null || callInfo.getId() != mCurrentCall.getId())
          {
            System.out.println("Call state event received, but call info is invalid");
            return true;
          }
          pjsip_inv_state state = callInfo.getState();
          if (state == pjsip_inv_state.PJSIP_INV_STATE_CALLING)
          {
            if (mSoundPoolUtil == null) {
              pjsipHandsFree(true);
              mSoundPoolUtil = new SoundPoolUtil(mActivity, new SoundPool.OnLoadCompleteListener()
              {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
                {
                  if (mSoundPoolUtil != null)
                    mSoundPoolUtil.play(mSoundWaitId);
                }
              });
              int rawId = R.raw.ring_back_sound;
              mSoundWaitId = mSoundPoolUtil.load(rawId);
            }

            mPjSipManagerState = PjSipManagerState.STATE_CALLING;

          } else if (state == pjsip_inv_state.PJSIP_INV_STATE_EARLY) {
            pjsip_status_code statusCode = callInfo.getLastStatusCode();
            if (statusCode == pjsip_status_code.PJSIP_SC_RINGING && callInfo.getRole() == pjsip_role_e.PJSIP_ROLE_UAC) {
              // check and play ringing sound
              if (mSoundPoolUtil == null) {
                pjsipHandsFree(true);
                mSoundPoolUtil = new SoundPoolUtil(mActivity, new SoundPool.OnLoadCompleteListener()
                {
                  @Override
                  public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
                  {
                    if (mSoundPoolUtil != null) {
                      mSoundPoolUtil.play(mSoundWaitId);
                    }
                  }
                });
                int rawId = R.raw.ring_back_sound;
                mSoundWaitId = mSoundPoolUtil.load(rawId);
              }
            } else if (statusCode == pjsip_status_code.PJSIP_SC_PROGRESS) {
              stopRingBackSound();
            }
          } else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)
          {
            registerPhoneState();
            stopRingBackSound();
            mPjSipManagerState = PjSipManagerState.STATE_CONFIRMED;
            // 通话状态被确认，震动500ms
            if (mVibrator != null)
              mVibrator.vibrate(500);
//            if (mActivity != null)
//              mActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            if (mAudioManager != null)
            {
              if (mAudioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION)
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//              if (mAudioManager.isMicrophoneMute())
//                mAudioManager.setMicrophoneMute(false);
//              if (mAudioManager.isSpeakerphoneOn())
//                mAudioManager.setSpeakerphoneOn(false);
            }
          } else if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
          {
            mPjSipManagerState = PjSipManagerState.STATE_DISCONNECTED;
            mCurrentCall.delete();
            mCurrentCall = null;

            stopRingBackSound();
            unRegisterPhoneState();
//            pjsipHandsFree(false);

          }
          Map<String, Object> args;
          if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
            args = buildArgumentsWithReason(callInfo.getStateText(), callInfo.getLastReason());
          } else {
            args = buildArguments(callInfo.getStateText(), callInfo.getRemoteUri());
          }
          if (mChannel != null)
          {
            Log.i(TAG, "FlutterPjsipPlugin 接收到状态 ==== " + callInfo.getStateText());
            mChannel.invokeMethod(METHOD_CALL_STATUS_CHANGED, args);
          }
          break;

        case MSG_TYPE.CALL_MEDIA_STATE:
          // TODO 未实现视频通话，暂不用实现
          break;

        case MSG_TYPE.INCOMING_CALL:
          /* Incoming call */
          MyCall call = (MyCall) msg.obj;
          CallOpParam prm = new CallOpParam();
          /* Only one call at anytime */
          if (mCurrentCall != null)
          {
            try
            {
              // 设置StatusCode
              prm.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
              call.hangup(prm);
              call.delete();
            } catch (Exception e)
            {
              e.printStackTrace();
            }
            return true;
          } else
          {
            try
            {
              mSoundPoolUtil = new SoundPoolUtil(mActivity, new SoundPool.OnLoadCompleteListener()
              {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
                {
                  if (mSoundPoolUtil != null)
                    mSoundPoolUtil.play(mSoundWaitId);
                }
              });
              int rawId = R.raw.incoming_ring;
              mSoundWaitId = mSoundPoolUtil.load(rawId);

              /* Answer with ringing */
              prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
              call.answer(prm);
              mCurrentCall = call;

              mPjSipManagerState = PjSipManagerState.STATE_INCOMING;

              if (mChannel != null)
              {
                mChannel.invokeMethod(METHOD_CALL_STATUS_CHANGED, buildArguments("INCOMING", mCurrentCall.getInfo().getRemoteUri()));
              }
            } catch (Exception e)
            {
              e.printStackTrace();
            }
          }
          break;

        case MSG_TYPE.CHANGE_NETWORK:
          if (mPjSipManager != null)
            mPjSipManager.handleNetworkChange();
          break;
      }
      return false;
    }
  });


//  private FlutterPjsipPlugin(final MethodChannel channel, Activity activity)
//  {
//    this.mChannel = channel;
//    this.mChannel.setMethodCallHandler(this);
//    this.mActivity = activity;
//
//    registerAudioManager();
//  }

  /**
   * Plugin registration.
   */
//  public static void registerWith(Registrar registrar)
//  {
//    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
//    //setMethodCallHandler在此通道上接收方法调用的回调
//    channel.setMethodCallHandler(new FlutterPjsipPlugin(channel, registrar.activity()));
//  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
      // Handle method calls (onMethodCall())
    this.mChannel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
    this.mChannel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (mChannel != null)
    {
      mChannel.setMethodCallHandler(null);
      mChannel = null;
    }
  }

  /**
   * Plugin's activity registration.
   */
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    mActivity = binding.getActivity();
    registerAudioManager();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result)
  {
    try
    {
      handleMethodCall(call, result);
    } catch (Exception e)
    {
      result.error("Unexpected error!", e.getMessage(), e);
    }
  }

  private void handleMethodCall(MethodCall call, Result result)
  {
    mMethod = call.method;
    mResult = result;
    if (mMethod == null || mResult == null) return;
    if (mActivity == null)
    {
      mResult.success(false);
      return;
    }
    switch (mMethod)
    {
      case METHOD_PJSIP_INIT:
        pjsipInit();
        break;

      case METHOD_PJSIP_LOGIN:
        String username = call.argument("username");
        String password = call.argument("password");
        mIp = call.argument("ip");
        mPort = call.argument("port");
        pjsipLogin(username, password, mIp, mPort);
        break;

      case METHOD_PJSIP_CALL:
        String toUsername = call.argument("username");
        String toIp = call.argument("ip");
        String toPort = call.argument("port");
        pjsipCall(toUsername, TextUtils.isEmpty(toIp) ? mIp : toIp, TextUtils.isEmpty(toPort) ? mPort : toPort);
        break;

      case METHOD_PJSIP_LOGOUT:
        pjsipLogout();
        break;

      case METHOD_PJSIP_DEINIT:
        pjsipDeinit();
        break;

      case METHOD_PJSIP_RECEIVE:
        pjsipReceive();
        break;

      case METHOD_PJSIP_REFUSE:
        pjsipRefuse();
        unRegisterPhoneState();
        break;

      case METHOD_PJSIP_HANDS_FREE:
        boolean speakerOn = Boolean.TRUE.equals(call.argument("speakerOn"));
        boolean speakerResult = pjsipHandsFree(speakerOn);
        result.success(speakerResult);
        break;

      case METHOD_PJSIP_MUTE:
        pjsipMute();
        break;

        // Additional case for my app, you can adjust what ever you want
      case METHOD_PJSIP_LOGIN_WITH_INFO:
        int serverId = call.argument("server_id");
        int serverPort = call.argument("server_port");
        String serverUrl = call.argument("server_url");
        String serverType= call.argument("server_type");
        String phoneLine= call.argument("phone_line");
        String phoneLinePassword= call.argument("phone_line_password");
        String outboundProxy= call.argument("outbound_proxy");

        pjsipLoginWithInfo(serverId, serverPort, serverUrl, serverType, phoneLine, phoneLinePassword, outboundProxy);
        break;

      case METHOD_PJSIP_TERMINATE_ALL_CALLS:
        break;
      case METHOD_PJSIP_MUTE2:
        boolean mute = Boolean.TRUE.equals(call.argument("mute"));
        pjsipMute2(mute);
        break;

      default:
        result.notImplemented();
        break;
    }
  }

  /**
   * PjSip初始化方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 23:37
   */
  private void pjsipInit()
  {
    if (mPjSipManagerState.getCode() > PjSipManagerState.STATE_UNDEFINED.getCode())
      mResult.success(false);
    else
    {
      mPjSipManager.init(mAppObserver);

      if (mReceiver == null)
      {
        mReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mActivity.getApplication().registerReceiver(mReceiver, intentFilter);
      }
      mPjSipManagerState = PjSipManagerState.STATE_INITED;
      mResult.success(true);
    }
  }

  /**
   * PjSip登录方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 23:38
   */
  private void pjsipLogin(String username, String password, String ip, String port)
  {
    if (mPjSipManagerState.getCode() == PjSipManagerState.STATE_INITED.getCode())
      mPjSipManager.login(username, password, ip, port);
    else
      mResult.success(false);
  }

  /** PjSip login with info (map)
   * @author ddwsc
   * create at 2025/02/23 17:08
   */
  private void pjsipLoginWithInfo(int serverId, int serverPort, String serverUrl, String serverType, String phoneLine, String phoneLinePassword, String outboundProxy) {
    if (mPjSipManagerState.getCode() == PjSipManagerState.STATE_INITED.getCode()) {
      mPjSipManager.loginWithInfo(serverId, serverPort, phoneLine, phoneLinePassword, serverUrl, serverType, outboundProxy);
//      mResult.success(true);
    }
    else {
      mResult.success(false);
    }
  }

  /**
   * PjSip打电话方法
   *
   * @author Jack Zhang
   * create at 2019-08-12 23:45
   */
  private void pjsipCall(String username, String ip, String port)
  {
    if (mCurrentCall != null)
      mResult.success(false);
    else
    {
      MyCall call = null;
      if (mPjSipManagerState.getCode() >= PjSipManagerState.STATE_LOGINED.getCode())
        call = mPjSipManager.call(username, ip, port);
      else
        mResult.success(false);

      if (call == null)
        mResult.success(false);
      else
      {
        mCurrentCall = call;
        mResult.success(true);
      }
    }
  }

  /**
   * PjSip登出方法
   *
   * @author Jack Zhang
   * create at 2019-08-22 00:02
   */
  private void pjsipLogout()
  {
    if (mPjSipManagerState.getCode() > PjSipManagerState.STATE_LOGINED.getCode())
    {
      mPjSipManager.logout();
      mPjSipManagerState = PjSipManagerState.STATE_INITED;
      mResult.success(true);
    } else
      mResult.success(false);
  }

  /**
   * PjSip销毁方法
   *
   * @author Jack Zhang
   * create at 2019-08-22 00:05
   */
  private void pjsipDeinit()
  {
    if (mPjSipManagerState.getCode() > PjSipManagerState.STATE_INITED.getCode())
    {
      mPjSipManager.deinit();
      if (mReceiver != null)
        mActivity.getApplication().unregisterReceiver(mReceiver);
      mReceiver = null;
      mPjSipManagerState = PjSipManagerState.STATE_UNDEFINED;
      mResult.success(true);
    } else
      mResult.success(false);
  }

  /**
   * PjSip接听电话
   *
   * @author Jack Zhang
   * create at 2019-08-22 11:45
   */
  private void pjsipReceive()
  {
    if (mCurrentCall == null)
      mResult.success(false);
    else
      try
      {
        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
        mCurrentCall.answer(prm);
        CallInfo callInfo = mCurrentCall.getInfo();
        if (mChannel != null)
        {
          mChannel.invokeMethod(METHOD_CALL_STATUS_CHANGED, buildArguments(callInfo.getStateText(), callInfo.getRemoteUri()));
          mResult.success(true);
        } else
          mResult.success(false);
      } catch (Exception e)
      {
        e.printStackTrace();
        mResult.success(false);
      }
  }

  /**
   * PjSip拒接/挂断
   *
   * @author Jack Zhang
   * create at 2019-08-22 16:32
   */
  private void pjsipRefuse()
  {
    if (mCurrentCall == null)
      mResult.success(false);
    else
    {
      try
      {
        CallOpParam prm = new CallOpParam();
        prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
        mCurrentCall.hangup(prm);
        mResult.success(true);
      } catch (Exception e)
      {
        e.printStackTrace();
        mResult.success(false);
      } finally
      {
        mCurrentCall = null;
        stopRingBackSound();
        if (mChannel != null)
        {
          mChannel.invokeMethod(METHOD_CALL_STATUS_CHANGED, buildArguments("DISCONNCTD", null));
        }
        mPjSipManagerState = PjSipManagerState.STATE_DISCONNECTED;
      }
    }
  }

  /**
   * PjSip免提功能
   *
   * @author Jack Zhang
   * create at 2019-08-22 17:23
   */
  private boolean pjsipHandsFree(boolean speakerOn)
  {
    try {
      if (speakerOn) {
        if (mActivity != null)
          mActivity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mAudioManager.setSpeakerphoneOn(true);
      } else {
        mAudioManager.setSpeakerphoneOn(false);
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
//      throw new RuntimeException(e);
//      mResult.success(false);
      return false;
    }

  }

//  private void requestAudioFocus() {
//    if (!mAudioFocused) {
//      int res = mAudioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//      Log.d(null, "Audio focus requested: " + (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted" : "Denied"));
//      if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) mAudioFocused = true;
//    }
//  }
//
//  public void disableAudioFocus() {
//    if (mAudioFocused) {
//      int res = mAudioManager.abandonAudioFocus(null);
//      Log.d(null, "Audio focus released a bit later: " + (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ? "Granted" : "Denied"));
//      mAudioFocused = false;
//    }
//  }
//
//  public synchronized void startRinging() {
////        speaker(true);
//
//    mAudioManager.setMode(MODE_RINGTONE);
//
//    try {
//      if ((mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) && mVibrator != null) {
//        long[] patern = {0, 1000, 1000};
//        mVibrator.vibrate(patern, 1);
//      }
//
//      if (mRingerPlayer == null) {
//        requestAudioFocus();
//        mRingerPlayer = new MediaPlayer();
//        mRingerPlayer.setAudioStreamType(AudioManager.STREAM_RING);
//
//
//        onRingerPlayerCreated(mRingerPlayer);
//        mRingerPlayer.prepare();
//        mRingerPlayer.setLooping(true);
//        mRingerPlayer.start();
//      } else {
//        Log.w(null, "already ringing");
//      }
//    } catch (Exception e) {
//      Log.e(null, e + "cannot handle incoming call_button_config");
//    }
////    isRinging = true;
//  }
//
//  void onRingerPlayerCreated(MediaPlayer mRingerPlayer) {
////    String uriString = getPref().getString(getString(R.string.pref_audio_ringtone), android.provider.Settings.System.DEFAULT_RINGTONE_URI.toString());
//    try {
//      AssetFileDescriptor afd = mActivity.getApplicationContext().getAssets().openFd("ring_back_sound.mp3");
//      mRingerPlayer.setDataSource(afd.getFileDescriptor(),
//              afd.getStartOffset(),
//              afd.getLength());
//      afd.close();
//      mRingerPlayer.prepare();
//      mRingerPlayer.start();
//
//    } catch (Exception e) {
//      Log.e("onRingerPlayerCreated", "Cannot set ringtone: " + e.getMessage());
//    }
//  }
//
//  private synchronized void stopRinging() {
//    if (mRingerPlayer != null) {
//      mRingerPlayer.stop();
//      mRingerPlayer.release();
//      mRingerPlayer = null;
//    }
//    if (mVibrator != null) {
//      mVibrator.cancel();
//    }
//
//
//    mAudioManager.setMode(AudioManager.MODE_NORMAL);
//
////    isRinging = false;
////
////    if (!BluetoothManager.getInstance().isBluetoothHeadsetAvailable()) {
////      speaker(true);
////    }
//  }

//  private void speaker(boolean speakerOn) {
//
//    if (mAudioManager != null) {
//      if (mCurrentCall == null) mAudioManager.setMode(MODE_NORMAL);
//      else mAudioManager.setMode(MODE_IN_COMMUNICATION);
//      Log.d("FlutterPjsipPlugin", "audioMode:" + mAudioManager.getMode());
//      if (!speakerOn && mCurrentCall != null && BluetoothManager.getInstance().isBluetoothHeadsetAvailable() && AudioSourceUtil.isBluetoothEnabled()) {
////                app.bluetooth();
////                Application.app.setPlaybackDev();
//        requestAudioFocus(AudioManager.STREAM_VOICE_CALL);
//        BluetoothManager.getInstance().routeAudioToBluetooth();
//      } else {
//        BluetoothManager.getInstance().disableBluetoothSCO();
//      }
////            if (speakerOn)
////                app.speaker();
//      AudioSourceUtil.routeAudioToSpeakerHelper(mAudioManager, speakerOn);
//    }
//  }

  /**
   * PjSip静音功能
   *
   * @author Jack Zhang
   * create at 2019-08-22 18:00
   */
  private void pjsipMute()
  {
    if (mPjSipManagerState == PjSipManagerState.STATE_CONFIRMED)
    {
      mAudioManager.setMicrophoneMute(!mAudioManager.isMicrophoneMute());
      mResult.success(true);
    } else
      mResult.success(false);
  }

  private void pjsipMute2(boolean mute)
  {
    if (mPjSipManagerState == PjSipManagerState.STATE_CONFIRMED)
    {
      mAudioManager.setMicrophoneMute(mute);
      mResult.success(true);
    } else
      mResult.success(false);
  }

  private class MyBroadcastReceiver extends BroadcastReceiver
  {
    private String conn_name = "";

    @Override
    public void onReceive(Context context, Intent intent)
    {
      if (isNetworkChange(context))
      {
        Message m = Message.obtain(handler, MSG_TYPE.CHANGE_NETWORK, null);
        m.sendToTarget();
      }
    }

    private boolean isNetworkChange(Context context)
    {
      boolean network_changed = false;
      ConnectivityManager connectivity_mgr = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
      if (connectivity_mgr != null)
      {
        NetworkInfo net_info = connectivity_mgr.getActiveNetworkInfo();
        if (net_info != null && conn_name != null)
        {
          if (net_info.isConnectedOrConnecting() && !conn_name.equalsIgnoreCase(""))
          {
            String new_con = net_info.getExtraInfo();
            if (new_con != null && !new_con.equalsIgnoreCase(conn_name))
              network_changed = true;
            conn_name = (new_con == null) ? "" : new_con;
          } else
          {
            if (conn_name.equalsIgnoreCase(""))
              conn_name = net_info.getExtraInfo();
          }
        }
      }
      return network_changed;
    }
  }

  private Map<String, Object> buildArguments(String status, Object remoteUri)
  {
    Map<String, Object> result = new HashMap<>();
    result.put("call_state", status);
    result.put("remote_uri", remoteUri != null ? remoteUri : "");
    return result;
  }
  private Map<String, Object> buildArgumentsWithReason(String status, String reason)
  {
    Map<String, Object> result = new HashMap<>();
    result.put("call_state", status);
    if (reason != null) {
      result.put("reason", reason);
    }
    return result;
  }

  /**
   * 注册基本监听
   *
   * @author Jack Zhang
   * create at 2019-08-22 17:41
   */
  private void registerAudioManager()
  {
    mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
  }

  /**
   * 注册相关监听
   *
   * @author Jack Zhang
   * create at 2019-08-20 23:37
   */
  private void registerPhoneState()
  {
    PowerManager powerManager = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
    // 距离感应器的电源锁
    // PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK 值为 32
    mWakeLock = powerManager.newWakeLock(32, getClass().getName());
    mWakeLock.setReferenceCounted(false); // 设置不启用引用计数

    // 传感器管理对象,调用距离传感器，控制屏幕
    mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
    mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);

//    mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
//    mSystemPhoneStateListener = new SystemPhoneStateListener();
//    mTelephonyManager.listen(mSystemPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

    mVibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
  }

  /**
   * 取消相关监听
   *
   * @author Jack Zhang
   * create at 2019-08-20 23:37
   */
  private void unRegisterPhoneState()
  {
//    if (mSystemPhoneStateListener != null && mTelephonyManager != null)
//    {
//      mTelephonyManager.listen(mSystemPhoneStateListener, PhoneStateListener.LISTEN_NONE);
//      mSystemPhoneStateListener = null;
//      mTelephonyManager = null;
//    }
    if (mSensorManager != null)
    {
      mSensorManager.unregisterListener(mSensorEventListener);
      mSensorManager = null;
    }
    if (mWakeLock != null)
    {
      mWakeLock.release();// 释放电源锁
      mWakeLock = null;
    }
    if (mVibrator != null)
    {
      mVibrator.cancel();
      mVibrator = null;
    }
    if (mAudioManager != null)// 还原系统音频设置
    {
      try
      {
        if (mActivity != null)
          mActivity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        if (mAudioManager.getMode() != AudioManager.MODE_NORMAL)
          mAudioManager.setMode(AudioManager.MODE_NORMAL);
        if (mAudioManager.isMicrophoneMute())
          mAudioManager.setMicrophoneMute(false);
        if (mAudioManager.isSpeakerphoneOn())
          mAudioManager.setSpeakerphoneOn(false);
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * 销毁SoundPoolUtil
   *
   * @author Jack Zhang
   * create at 2019-08-22 19:43
   */
  private void stopRingBackSound()
  {
//    stopRinging();
    if (mSoundPoolUtil != null && mSoundWaitId != 0)
    {
      mSoundPoolUtil.stop(mSoundWaitId);
      mSoundWaitId = 0;
      mSoundPoolUtil.destroy();
      mSoundPoolUtil = null;
    }
  }

  private SensorEventListener mSensorEventListener = new SensorEventListener()
  {
    /**
     * 距离传感器监听
     *
     * @author Jack Zhang
     * create at 2019-08-13 14:49
     */
    @Override
    public void onSensorChanged(SensorEvent event)
    {
      float[] its = event.values;
      if (its != null && event.sensor.getType() == Sensor.TYPE_PROXIMITY)
      {
        // 经过测试，当手贴近距离感应器的时候its[0]返回值为0.0，当手离开时返回1.0
        if (its[0] == 0.0f)
        {
          // 贴近手机
          if (!mWakeLock.isHeld())
            mWakeLock.acquire();// 申请设备电源锁
        } else
        {
          // 远离手机
          if (mWakeLock.isHeld())
          {
            mWakeLock.setReferenceCounted(false);
            mWakeLock.release(); // 释放设备电源锁
          }
        }
      }
    }

    /**
     * 精度传感器监听
     *
     * @author Jack Zhang
     * create at 2019-08-13 14:50
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
  };

  private class SystemPhoneStateListener extends PhoneStateListener
  {
    @Override
    public void onCallStateChanged(int state, String incomingNumber)
    {
      switch (state)
      {
        case TelephonyManager.CALL_STATE_RINGING:
          //等待接电话
          break;
        case TelephonyManager.CALL_STATE_IDLE:
          //电话挂断
          break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
          //通话中
          //挂断网络电话
          pjsipRefuse();
          break;
      }
      super.onCallStateChanged(state, incomingNumber);
    }
  }
}