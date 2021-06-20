package com.stc.CDVMagtekPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.magtek.mobile.android.mtlib.IMTCardData;
import com.magtek.mobile.android.mtlib.MTCardDataState;
import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

public class MagtekPlugin extends CordovaPlugin {

    private Activity activity;
    private CordovaPlugin cordovaPlugin;

    private static String TAG = "MagtekPlugin";
    AudioManager m_audioManager;
    private int m_audioVolume;
    private MTSCRA m_tscra;
    private boolean m_emvMessageFormatRequestPending = false;
    private boolean m_startTransactionActionPending;

    private final HeadSetBroadCastReceiver m_headsetReceiver = new HeadSetBroadCastReceiver();
    private final NoisyAudioStreamReceiver m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    private Handler m_tscraHandler = new Handler(new SCRAHandlerCallback());
    private MTConnectionType m_connectionType;
    private MTConnectionState m_connectionState = MTConnectionState.Disconnected;
    private String m_deviceAddress;
    public String m_deviceName;
    private String m_audioConfigType;

    private PermissionCallback permissionCallback;
    private OpenCallback openCallback;
    private CloseCallback closeCallback;
    private ReadCallback readCallback;

    public MagtekPlugin(Activity activity, CordovaPlugin cordovaPlugin) {
        this.activity = activity;
        this.cordovaPlugin = cordovaPlugin;

        m_connectionType = MTConnectionType.Audio;
        m_audioManager = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        m_tscra = new MTSCRA(activity.getApplicationContext(), m_tscraHandler);
    }

    public void open(OpenCallback openCallback) {
        this.openCallback = openCallback;

        Log.d(TAG, "Open");

        activity.getApplicationContext().registerReceiver(m_headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        activity.getApplicationContext().registerReceiver(m_noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        this.openDevice();
    }

    public void close(CloseCallback closeCallback) {
        this.closeCallback = closeCallback;

        Log.d(TAG, "Close");

        activity.getApplicationContext().unregisterReceiver(m_headsetReceiver);
        activity.getApplicationContext().unregisterReceiver(m_noisyAudioStreamReceiver);

        if(this.closeCallback != null) {
            this.closeCallback.onResult(closeDevice());
        }

        this.readCallback = null;
    }

    public void readCard(ReadCallback readCallback) {

        Log.d(TAG, "Read Card");

        this.readCallback = readCallback;
    }


    // PRIVATE FUNCTIONS

    private void setVolume(int volume) {
        int showUI = 0;
        //showUI = AudioManager.FLAG_SHOW_UI;
        m_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, showUI);
    }

    private void saveVolume() {
        m_audioVolume = m_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void restoreVolume() {
        setVolume(m_audioVolume);
    }

    private void setVolumeToMax() {
        saveVolume();

        int volume = m_audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        setVolume(volume);
    }

    private class SCRAHandlerCallback implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            try {
                Log.i(TAG, "*** Callback " + msg.what);
                switch (msg.what) {
                    case MTSCRAEvent.OnDeviceConnectionStateChanged:
                        OnDeviceStateChanged((MTConnectionState) msg.obj);
                        break;
                    case MTSCRAEvent.OnCardDataStateChanged:
                        OnCardDataStateChanged((MTCardDataState) msg.obj);
                        break;
                    case MTSCRAEvent.OnDataReceived:
                        OnCardDataReceived((IMTCardData) msg.obj);
                        break;
                   /* case MTSCRAEvent.OnDeviceResponse:
                        OnDeviceResponse((String) msg.obj);
                        break;
                           */
                }
            } catch (Exception ex) {
                Log.d(TAG, "Error:" + ex.getMessage());
            }

            return true;
        }
    }

    protected void OnDeviceStateChanged(MTConnectionState deviceState) {
        MTConnectionState tPrevState = m_connectionState;
        m_connectionState = deviceState;

        switch (deviceState) {
            case Disconnected:
                Log.i(TAG, "OnDeviceStateChanged=Disconnected");
                if (m_connectionType == MTConnectionType.Audio) {
                    restoreVolume();
                }
                break;
            case Connected:
                Log.i(TAG, "OnDeviceStateChanged=Connected");
                if (m_connectionType == MTConnectionType.Audio) {
                    setVolumeToMax();
                }
                break;
            case Error:
                Log.i(TAG, "OnDeviceStateChanged=Error");
                break;
            case Connecting:
                Log.i(TAG, "OnDeviceStateChanged=Connecting");
                break;
            case Disconnecting:
                Log.i(TAG, "OnDeviceStateChanged=Disconnecting");
                break;
        }
    }

    protected void OnCardDataStateChanged(MTCardDataState cardDataState) {
        switch (cardDataState) {
            case DataNotReady:
                Log.i(TAG, "[Card Data Not Ready]");
                break;
            case DataReady:
                Log.i(TAG, "[Card Data Ready]");
                break;
            case DataError:
                Log.i(TAG, "[Card Data Error]");
                break;
        }

    }

    protected void OnCardDataReceived(IMTCardData cardData) {
        Log.d(TAG, "Result" + getCardInfo());

        if(this.readCallback != null) {
            this.readCallback.onResult(getCardInfoJSON());
        }
    }

  /*
 protected void OnDeviceResponse(String data) {

        if (m_emvMessageFormatRequestPending) {
            m_emvMessageFormatRequestPending = false;

        } else if (m_startTransactionActionPending) {
            m_startTransactionActionPending = false;

            startTransaction();
        }
    }


    private void startTransaction() {
        if (m_tscra != null) {
            byte timeLimit = 0x3C;
            //byte cardType = 0x02;  // Chip Only
            byte cardType = 0x03;  // MSR + Chip
            byte option = 0x00;
            byte[] amount = new byte[]{0x00, 0x00, 0x00, 0x00, 0x15, 0x00};
            byte transactionType = 0x00; // Purchase
            byte[] cashBack = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] currencyCode = new byte[]{0x08, 0x40};
            byte reportingOption = 0x02;  // All Status Changes

            int result = m_tscra.startTransaction(timeLimit, cardType, option, amount, transactionType, cashBack, currencyCode, reportingOption);
            Log.d(TAG, "[Start Transaction] (Result=" + result + ")");
        }
    }
*/

    private String getCardInfo() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("Track1.Masked=%s \n", m_tscra.getTrack1Masked()));
        stringBuilder.append(String.format("Track2.Masked=%s \n", m_tscra.getTrack2Masked()));
        stringBuilder.append(String.format("Track3.Masked=%s \n", m_tscra.getTrack3Masked()));

        return stringBuilder.toString();
    }

    private JSONObject getCardInfoJSON() {
        JSONObject data = null;

        try {
            data = new JSONObject();
            data.put("Track1", m_tscra.getTrack1Masked());
            data.put("Track2", m_tscra.getTrack2Masked());
            data.put("Track3", m_tscra.getTrack3Masked());
        }
        catch(JSONException jsonEX) {}
        catch(Exception ex) {}

        return data;
    }

    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (m_connectionType == MTConnectionType.Audio) {
                    if (m_tscra.isDeviceConnected()) {
                        closeDevice();
                    }
                }
            }
        }
    }

    private class HeadSetBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String action = intent.getAction();

                if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0)   //if the action match a headset one
                {
                    int headSetState = intent.getIntExtra("state", 0);      //get the headset state property
                    int hasMicrophone = intent.getIntExtra("microphone", 0);//get the headset microphone property

                    if ((headSetState == 1) && (hasMicrophone == 1))        //headset was unplugged & has no microphone
                    {
                    } else {
                        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                            if (m_connectionType == MTConnectionType.Audio) {
                                if (m_tscra.isDeviceConnected()) {
                                    closeDevice();
                                }
                            }
                        }
                    }

                }

            } catch (Exception ex) {

            }
        }
    }

    private int closeDevice() {
        Log.i(TAG, "SCRADevice closeDevice");
        int result = -1;
        if (m_tscra != null) {
            m_tscra.closeDevice();

            result = 0;
        }

        return result;
    }

    private void openDevice() {
        Log.d(TAG, "OpenDevice");

        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS};

        /*askPermissions(permissions, new PermissionCallback() {
            @Override
            public void onGranted() {*/
                if (m_connectionType == MTConnectionType.BLEEMV) {
                    if (m_connectionState != MTConnectionState.Disconnected) {
                        Log.i(TAG, "SCRADevice openDevice:Device Not Disconnected");

                    }
                }
                if (m_tscra != null) {
                    m_tscra.setConnectionType(m_connectionType);
                    m_tscra.setAddress(m_deviceAddress);

                    boolean enableRetry = false;

                    m_tscra.setConnectionRetry(enableRetry);

                    if (m_connectionType == MTConnectionType.Audio) {
                        m_tscra.setDeviceConfiguration(getManualAudioConfig());
                    }

                    m_tscra.openDevice();

                    /*int result = -1;
                    if (m_tscra.isDeviceConnected()) {
                        result = 0;
                    }

                    if (openCallback != null) {
                        openCallback.onResult(result);
                    }*/
                }

                openCallback.onResult(0);
            /*}

            @Override
            public void onDenied() {
                if (openCallback != null) {
                    openCallback.onResult(-1);
                }
            }
        });*/
    }


    private boolean isDeviceOpened() {
        Log.i(TAG, "SCRADevice isDeviceOpened");

        return (m_connectionState == MTConnectionState.Connected);
    }

    private String getManualAudioConfig()
    {
        String config = "";

        try
        {
            String model = android.os.Build.MODEL.toUpperCase();

            if(model.contains("DROID RAZR") || model.toUpperCase().contains("XT910"))
            {
                config = "INPUT_SAMPLE_RATE_IN_HZ=48000,";
            }
            else if ((model.equals("DROID PRO"))||
                    (model.equals("MB508"))||
                    (model.equals("DROIDX"))||
                    (model.equals("DROID2"))||
                    (model.equals("MB525")))
            {
                config = "INPUT_SAMPLE_RATE_IN_HZ=32000,";
            }
            else if ((model.equals("GT-I9300"))||//S3 GSM Unlocked
                    (model.equals("SPH-L710"))||//S3 Sprint
                    (model.equals("SGH-T999"))||//S3 T-Mobile
                    (model.equals("SCH-I535"))||//S3 Verizon
                    (model.equals("SCH-R530"))||//S3 US Cellular
                    (model.equals("SAMSUNG-SGH-I747"))||// S3 AT&T
                    (model.equals("M532"))||//Fujitsu
                    (model.equals("GT-N7100"))||//Notes 2
                    (model.equals("GT-N7105"))||//Notes 2
                    (model.equals("SAMSUNG-SGH-I317"))||// Notes 2
                    (model.equals("SCH-I605"))||// Notes 2
                    (model.equals("SCH-R950"))||// Notes 2
                    (model.equals("SGH-T889"))||// Notes 2
                    (model.equals("SPH-L900"))||// Notes 2
                    //(model.equals("SM-T211"))||//Galaxy Tab 3
                    (model.equals("GT-P3113")))//Galaxy Tab 2, 7.0

            {
                config = "INPUT_AUDIO_SOURCE=VRECOG,";
            }
            else if ((model.equals("XT907")))
            {
                config = "INPUT_WAVE_FORM=0,";
            }
            else
            {
                config = "INPUT_AUDIO_SOURCE=VRECOG,";
                //config += "PAN_MOD10_CHECKDIGIT=FALSE";
            }

        }
        catch (Exception ex)
        {
            Log.d(TAG,"Error:"+ex.getMessage());
        }

        return config;
    }

    private interface PermissionCallback
    {
        void onGranted();
        void onDenied();
    }
    public interface OpenCallback
    {
        void onResult(int result);
    }
    public interface CloseCallback
    {
        void onResult(int result);
    }
    public interface ReadCallback
    {
        //void onResult(String data);
        void onResult(JSONObject data);
    }

    private void askPermissions(String[] permissions, PermissionCallback cb)
    {
        this.permissionCallback = cb;

        Log.d(TAG, "Ask Permission");

        //cordova.requestPermissions(this.cordovaPlugin, 1, permissions);
    }

    /*@Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if(this.permissionCallback == null) {
            return;
        }        

        if (permissions != null && permissions.length > 0) {
            boolean hasAllPermissions = hasAllPermissions(permissions);
            if(hasAllPermissions) {
                this.permissionCallback.onGranted();
            }
            else {
                this.permissionCallback.onDenied();
            }
        } else {
            this.permissionCallback.onDenied();
        }

        this.permissionCallback = null;
    }*/
    private boolean hasAllPermissions(String[] permissions) throws JSONException {

        for (String permission : permissions) {
            //if(!cordova.hasPermission(permission)) {
                return false;
            //}
        }

        return true;
    }
}
