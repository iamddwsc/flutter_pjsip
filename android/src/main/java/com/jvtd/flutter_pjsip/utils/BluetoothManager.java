//package com.jvtd.flutter_pjsip.utils;
//
//
//import static android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED;
//
//import android.Manifest;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothAssignedNumbers;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothHeadset;
//import android.bluetooth.BluetoothProfile;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.media.AudioManager;
//import android.util.Log;
//
//import androidx.core.app.ActivityCompat;
//
//import java.util.List;
//
//public class BluetoothManager extends BroadcastReceiver {
//    private static BluetoothManager instance;
//    public int PLANTRONICS_BUTTON_PRESS = 1;
//    public int PLANTRONICS_BUTTON_LONG_PRESS = 2;
//    public int PLANTRONICS_BUTTON_DOUBLE_PRESS = 5;
//    public int PLANTRONICS_BUTTON_CALL = 2;
//    public int PLANTRONICS_BUTTON_MUTE = 3;
//    private Context mContext;
//    private AudioManager mAudioManager;
//    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothHeadset mBluetoothHeadset;
//    private BluetoothDevice mBluetoothDevice;
//    private BluetoothProfile.ServiceListener mProfileListener;
//    private boolean isBluetoothConnected;
//    private boolean isScoConnected;
//
//    final private String LOG_NAME = "BluetoothManager";
//
//    public BluetoothManager() {
//        isBluetoothConnected = false;
//        if (!ensureInit()) {
//            Log.w(LOG_NAME, "BluetoothManager tried to init but CloudcallphoneService not ready yet...");
//        }
//        instance = this;
//    }
//
//    public static BluetoothManager getInstance() {
//        if (instance == null) {
//            instance = new BluetoothManager();
//        }
//        return instance;
//    }
//
//    public void initBluetooth() {
//        if (!ensureInit()) {
//            Log.w(LOG_NAME, "BluetoothManager tried to init bluetooth but CloudcallphoneService not ready yet...");
//            return;
//        }
//
//        IntentFilter filter = new IntentFilter();
//        filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS);
//        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
//        filter.addAction(ACTION_STATE_CHANGED);
//        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
//        filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
//        mContext.registerReceiver(this, filter);
//        Log.d(LOG_NAME, "Bluetooth receiver started");
//
//        startBluetooth();
//    }
//
//    public void startBluetooth() {
//        if (isBluetoothConnected) {
//            Log.e(LOG_NAME, "Bluetooth already started");
//            return;
//        }
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
//            if (mProfileListener != null) {
//                Log.w(LOG_NAME, "Bluetooth headset profile was already opened, let's close it");
//                mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
//            }
//
//            mProfileListener = new BluetoothProfile.ServiceListener() {
//                public void onServiceConnected(int profile, BluetoothProfile proxy) {
//                    if (profile == BluetoothProfile.HEADSET) {
//                        Log.d(LOG_NAME, "Bluetooth headset connected");
//                        mBluetoothHeadset = (BluetoothHeadset) proxy;
//                        isBluetoothConnected = true;
//                        if (AudioSourceUtil.isSpeakerEnabled())
//                            ApplicationEx.getInstance().routeAudioToSpeaker();
//                        else
//                            ApplicationEx.getInstance().routeAudioToReceiver();
//                        if (CallActivity.isInstanced()) {
//                            CallActivity.getInstance().setSpeakerUI(AudioSourceUtil.isSpeakerEnabled());
//                        }
//                    }
//                }
//
//                public void onServiceDisconnected(int profile) {
//                    if (profile == BluetoothProfile.HEADSET) {
//                        mBluetoothHeadset = null;
//                        isBluetoothConnected = false;
//                        Log.d(LOG_NAME, "Bluetooth headset disconnected");
//
//                        if (AudioSourceUtil.isSpeakerEnabled())
//                            ApplicationEx.getInstance().routeAudioToSpeaker();
//                        else
//                            ApplicationEx.getInstance().routeAudioToReceiver();
//                        if (CallActivity.isInstanced()) {
//                            CallActivity.getInstance().setSpeakerUI(AudioSourceUtil.isSpeakerEnabled());
//                        }
//                    }
//                }
//            };
//            boolean success = mBluetoothAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET);
//            if (!success) {
//                Log.e(LOG_NAME, "Bluetooth getProfileProxy failed !");
//            }
//        } else {
//            Log.w(LOG_NAME, "Bluetooth interface disabled on device");
//        }
//    }
//
//    private boolean ensureInit() {
//        if (mBluetoothAdapter == null) {
//            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        }
//        if (mContext == null) {
//            if (ApplicationEx.getInstance() != null) {
//                mContext = ApplicationEx.getInstance().getApplicationContext();
//            } else {
//                return false;
//            }
//        }
//        if (mContext != null && mAudioManager == null) {
//            mAudioManager = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
//        }
//        return true;
//    }
//
//    public boolean routeAudioToBluetooth() {
//        ensureInit();
//
//        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall()) {
//            if (isBluetoothHeadsetAvailable()) {
//                if (mAudioManager != null && !mAudioManager.isBluetoothScoOn()) {
//                    Log.d(LOG_NAME, "Bluetooth sco off, let's start it");
//                    mAudioManager.startBluetoothSco();
//                    mAudioManager.setBluetoothScoOn(true);
//                }
//            } else {
//                return false;
//            }
//
//
//            boolean ok = isUsingBluetoothAudioRoute();
//            int retries = 0;
//            while (!ok && retries < 5) {
//                retries++;
//
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                }
//
//                if (mAudioManager != null) {
//                    mAudioManager.startBluetoothSco();
//                    mAudioManager.setBluetoothScoOn(true);
//                }
//
//                ok = isUsingBluetoothAudioRoute();
//            }
//            if (ok) {
//                if (retries > 0) {
//                    Log.d(LOG_NAME, "Bluetooth route ok after " + retries + " retries");
//                } else {
//                    Log.d(LOG_NAME, "Bluetooth route ok");
//                }
//            } else {
//                Log.d(LOG_NAME, "Bluetooth still not ok...");
//            }
//
//            return ok;
//        }
//
//        return false;
//    }
//
//    public boolean isUsingBluetoothAudioRoute() {
//        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//            return mBluetoothHeadset != null && mBluetoothHeadset.isAudioConnected(mBluetoothDevice) && isScoConnected;
//        }
//        return false;
//    }
//
//    public boolean isBluetoothHeadsetAvailable() {
//        ensureInit();
//        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall()) {
//                boolean isHeadsetConnected = false;
//                if (mBluetoothHeadset != null) {
//                    List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
//                    mBluetoothDevice = null;
//                    for (final BluetoothDevice dev : devices) {
//                        if (mBluetoothHeadset.getConnectionState(dev) == BluetoothHeadset.STATE_CONNECTED) {
//                            mBluetoothDevice = dev;
//                            isHeadsetConnected = true;
//                            break;
//                        }
//                    }
//                    Log.d(this, isHeadsetConnected ? "Headset found, bluetooth audio route available" : "No headset found, bluetooth audio route unavailable");
//                }
//                return isHeadsetConnected;
//            }
//        }
//
//        return false;
//    }
//
//    public void disableBluetoothSCO() {
//        if (mAudioManager != null && mAudioManager.isBluetoothScoOn()) {
//            mAudioManager.stopBluetoothSco();
//            mAudioManager.setBluetoothScoOn(false);
//
//
//            int retries = 0;
//            while (isScoConnected && retries < 5) {
//                retries++;
//
//                try {
//                    Thread.sleep(10);
//                } catch (InterruptedException e) {
//                }
//
//                mAudioManager.stopBluetoothSco();
//                mAudioManager.setBluetoothScoOn(false);
//            }
//            Log.w(LOG_NAME, "Bluetooth sco disconnected!");
//        }
//    }
//
//    public void stopBluetooth() {
//        Log.w(LOG_NAME, "Stopping bluetooth...");
//        isBluetoothConnected = false;
//
//        disableBluetoothSCO();
//
//        if (mBluetoothAdapter != null && mProfileListener != null && mBluetoothHeadset != null) {
//            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
//            mProfileListener = null;
//        }
//        mBluetoothDevice = null;
//
//        Log.w(LOG_NAME, "Bluetooth stopped!");
//
////        if (CloudcallManager.isInstanciated()) {
//        ApplicationEx.getInstance().routeAudioToReceiver();
////        }
//
//        if (CallActivity.isInstanced()) {
//            CallActivity.getInstance().setSpeakerUI(false);
//        }
//    }
//
//    public void destroy() {
//        try {
//            stopBluetooth();
//
//            try {
//                mContext.unregisterReceiver(this);
//                Log.d(LOG_NAME, "Bluetooth receiver stopped");
//            } catch (Exception e) {
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void onReceive(Context context, Intent intent) {
////        if (!CloudcallManager.isInstanciated())
////            return;
//
//        String action = intent.getAction();
//        if (AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED.equals(action)) {
//            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0);
//            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
//                Log.d(LOG_NAME, "Bluetooth sco state => connected");
//
//                isScoConnected = true;
//            } else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
//                Log.d(LOG_NAME, "Bluetooth sco state => disconnected");
//
//                isScoConnected = false;
//            } else {
//                Log.d(LOG_NAME, "Bluetooth sco state => " + state);
//            }
//        } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
//            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
//            if (state == 0) {
//                Log.d(LOG_NAME, "Bluetooth state => disconnected");
//                stopBluetooth();
//            } else if (state == 2) {
//                Log.d(LOG_NAME, "Bluetooth state => connected");
//                startBluetooth();
//            } else {
//                Log.d(LOG_NAME, "Bluetooth state => " + state);
//            }
//        } else if (intent.getAction().equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)) {
//            String command = intent.getExtras().getString(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
//
//
//            Object[] args = (Object[]) intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
//            String eventName = (String) args[0];
//
//            if (eventName.equals("BUTTON") && args.length >= 3) {
//                Integer buttonID = (Integer) args[1];
//                Integer mode = (Integer) args[2];
//                Log.d(LOG_NAME, "Bluetooth event: " + command + " : " + eventName + ", id = " + buttonID + " (" + mode + ")");
//            }
//        } else if (ACTION_STATE_CHANGED.equals(action)) {
//            int EXTRA_STATE = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//            Log.d(LOG_NAME, "EXTRA_STATE:" + EXTRA_STATE);
//            switch (EXTRA_STATE) {
//                case BluetoothAdapter.STATE_CONNECTED:
//                    Log.d(LOG_NAME, "BT State:STATE_CONNECTED");
//
//                    break;
//                case BluetoothAdapter.STATE_CONNECTING:
//                    Log.d(this,
//                            "BT State: STATE_CONNECTING");
//                    break;
//
//                case BluetoothAdapter.STATE_DISCONNECTED:
//                    Log.d(this,
//                            "BT State: STATE_DISCONNECTED");
//                    break;
//                case BluetoothAdapter.STATE_DISCONNECTING:
//                    Log.d(this,
//                            "BT State: STATE_DISCONNECTING");
//                    break;
//                case BluetoothAdapter.STATE_OFF:
//                    Log.d(this,
//                            "BT State: STATE_OFF");
//                    Log.d(this,
//                            "Re-enabling BT to Refresh...");
////                    mBluetoothAdapter.enable();
//                    break;
//                case BluetoothAdapter.STATE_ON:
//                    Log.d(this,
//                            "BT State: STATE_ON");
//
//                    break;
//                case BluetoothAdapter.STATE_TURNING_OFF:
//                    Log.d(this,
//                            "BT State: STATE_TURNING_OFF");
//                    break;
//                case BluetoothAdapter.STATE_TURNING_ON:
//                    Log.d(this,
//                            "BT State: STATE_TURNING_ON");
//                    break;
//            }
//        }
//    }
//}
