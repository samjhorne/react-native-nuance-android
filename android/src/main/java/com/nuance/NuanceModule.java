package com.nuance;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.nuance.speechkit.DetectionType;
import com.nuance.speechkit.Interpretation;
import com.nuance.speechkit.Language;
import com.nuance.speechkit.Recognition;
import com.nuance.speechkit.RecognitionType;
import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NuanceModule extends ReactContextBaseJavaModule {
    private State state = State.IDLE;
    private Session speechSession;
    private Transaction recoTransaction;
    private String language;
    private Uri uri;
    private String contextTag;
    private RecognitionType recognitionType;
    private DetectionType detectionType;

    //constructor
    public NuanceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        // default to short detection type
        this.detectionType = DetectionType.Short;
        // default recognition
        this.recognitionType = RecognitionType.DICTATION;
        // default language
        this.language = "eng-USA";
        // make sure listener object is created
        InstantiateListener();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("STATE_IDLE", State.IDLE.toString());
        constants.put("STATE_LISTENING", State.LISTENING.toString());
        constants.put("STATE_PROCESSING", State.PROCESSING.toString());

        constants.put("DETECTION_TYPE_SHORT", DetectionType.Short.toString());
        constants.put("DETECTION_TYPE_LONG", DetectionType.Long.toString());
        constants.put("DETECTION_TYPE_NONE", DetectionType.None.toString());

        constants.put("RECOGNITION_TYPE_DICTATION", RecognitionType.DICTATION.toString());
        constants.put("RECOGNITION_TYPE_SEARCH", RecognitionType.SEARCH.toString());
        constants.put("RECOGNITION_TYPE_TV", RecognitionType.TV.toString());
        return constants;
    }

    //Mandatory function getName that specifies the module name
    @Override
    public String getName() {
        return "NuanceModule";
    }

    @ReactMethod
    public void CreateSession(String appId, String serverHost, String serverPort, String appKey){
        this.uri = Uri.parse("nmsps://" + appId + "@" + serverHost + ":" + serverPort);
        this.speechSession = Session.Factory.session(this.getReactApplicationContext(), uri, appKey);

        //loadEarcons();

        setState(State.IDLE);
    }

    @ReactMethod
    public void StartNlu(String contextTag, String language, String detectionType){
        this.language = language;
        this.detectionType = ThisDetectionToDetectionType(detectionType);
        this.contextTag = contextTag;
        Nlu();
    }

    private void Nlu(){
        switch (state) {
            case IDLE:
                recognizeWithService();
                break;
            case LISTENING:
                stopRecording();
                break;
            case PROCESSING:
                cancel();
                break;
        }
    }

    @ReactMethod
    public void StartAsr(String language, String detectionType, String recognitionType){
        this.language = language;
        this.recognitionType = ThisRecoTypeToRecoType(recognitionType);
        this.detectionType = ThisDetectionToDetectionType(detectionType);
        Asr();
    }
    private void Asr(){
        switch (state) {
            case IDLE:
                recognize();
                break;
            case LISTENING:
                stopRecording();
                break;
            case PROCESSING:
                cancel();
                break;
        }
    }
    /**
     * Start listening to the user and streaming their voice to the server.
     */
    private void recognize() {
        //Setup our Reco transaction options.
        Transaction.Options options = new Transaction.Options();
        options.setRecognitionType(this.recognitionType);
        options.setDetection(this.detectionType);
        options.setLanguage(new Language(this.language));
        // TODO: implement earcons
        //options.setEarcons(startEarcon, stopEarcon, errorEarcon, null);

        //Start listening
        recoTransaction = speechSession.recognize(options, recoListener);
    }

    /**
     * Start listening to the user and streaming their voice to the server.
     */
    private void recognizeWithService() {
        //Setup our Reco transaction options.
        Transaction.Options options = new Transaction.Options();
        options.setDetection(this.detectionType);
        options.setLanguage(new Language(this.language));
        //TODO: implement earcons
        //options.setEarcons(startEarcon, stopEarcon, errorEarcon, null);

        //Add properties to appServerData for use with custom service. Leave empty for use with NLU.
        JSONObject appServerData = new JSONObject();
        //Start listening
        recoTransaction = speechSession.recognizeWithService(contextTag, appServerData, options, recoListener);
    }

    private void InstantiateListener(){
        final ReactApplicationContext context  = this.getReactApplicationContext();
        recoListener = new Transaction.Listener() {
            @Override
            public void onStartedRecording(Transaction transaction) {
                //logs.append("\nonStartedRecording");

                //We have started recording the users voice.
                //We should update our state and start polling their volume.
                setState(State.LISTENING);
                //startAudioLevelPoll();
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onStartedRecording",null);
            }

            @Override
            public void onFinishedRecording(Transaction transaction) {
                //logs.append("\nonFinishedRecording");

                //We have finished recording the users voice.
                //We should update our state and stop polling their volume.
                setState(State.PROCESSING);
                //stopAudioLevelPoll();
                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onFinishedRecording",null);
            }

            @Override
            public void onServiceResponse(Transaction transaction, org.json.JSONObject response) {
                // We have received a service response. In this case it is our NLU result.
                // Note: this will only happen if you are doing NLU (or using a service)
                setState(State.IDLE);

                // Create map for params
                WritableMap payload = Arguments.createMap();

                try {
                    // Put data to map
                    payload.putMap("response", Helpers.convertJsonToMap(response));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onServiceResponse",payload);
            }

            @Override
            public void onRecognition(Transaction transaction, Recognition recognition) {
                //We have received a transcription of the users voice from the server.
                setState(State.IDLE);

                // Create map for params
                WritableMap payload = Arguments.createMap();

                // Put data to map
                payload.putString("recognition", recognition.getText());

                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onRecognition",payload);
            }

            @Override
            public void onInterpretation(Transaction transaction, Interpretation interpretation) {
                // We have received a service response. In this case it is our NLU result.
                // Note: this will only happen if you are doing NLU (or using a service)
                setState(State.IDLE);

                // Create map for params
                WritableMap payload = Arguments.createMap();

                try {
                    // Put data to map
                    payload.putMap("interpretation", Helpers.convertJsonToMap(interpretation.getResult()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onInterpretation",payload);

            }

            @Override
            public void onSuccess(Transaction transaction, String s) {
                //logs.append("\nonSuccess");

                //Notification of a successful transaction.
                // Create map for params
                WritableMap payload = Arguments.createMap();
                // Put data to map
                payload.putString("string", s);

                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onSuccess",payload);
            }

            @Override
            public void onError(Transaction transaction, String s, TransactionException e) {
                //logs.append("\nonError: " + e.getMessage() + ". " + s);

                //Something went wrong. Check your configuration to ensure that your settings are correct.
                //The user could also be offline, so be sure to handle this case appropriately.
                //We will simply reset to the idle state.
                setState(State.IDLE);
                // Create map for params
                WritableMap payload = Arguments.createMap();

                try {
                    // TODO: should be an easier way to flatten the exception
                    // Put data to map
                    JSONObject jsonObject = new JSONObject(Helpers.createDefaultGson().toJson(e));
                    payload.putString("errorMessage",e.getMessage());
                    StackTraceElement[] stackTraceELements = e.getStackTrace();
                    ArrayList<String> stackTraceElementsJson = new ArrayList<String>();
                    JSONArray stackTraceJsonArray = new JSONArray();
                    String stackTraceMessage = "";
                    for (int i = 0; i < stackTraceELements.length; i++){
                        stackTraceJsonArray.put(stackTraceELements[i].toString());
                    }
                    payload.putArray("stackTrace", Helpers.convertJsonToArray(stackTraceJsonArray));
                    payload.putMap("error", Helpers.convertJsonToMap(jsonObject));
                } catch (JSONException err) {
                    err.printStackTrace();
                }

                context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onError",payload);

            }
        };
    }

    private Transaction.Listener recoListener;

    /**
     * Stop recording the user
     */
    private void stopRecording() {
        recoTransaction.stopRecording();
    }

    /**
     * Cancel the Reco transaction.
     * This will only cancel if we have not received a response from the server yet.
     */
    private void cancel() {
        recoTransaction.cancel();
    }

    /* Audio Level Polling */

    //TODO: reimplement volume poller callback handler
    /* private Handler handler = new Handler();

     *//**
     * Every 50 milliseconds we should update the volume meter in our UI.
     *//*
    private Runnable audioPoller = new Runnable() {
        @Override
        public void run() {
            float level = recoTransaction.getAudioLevel();
            //volumeBar.setProgress((int)level);
            handler.postDelayed(audioPoller, 50);
        }
    };

    *//**
     * Start polling the users audio level.
     *//*
    private void startAudioLevelPoll() {
        audioPoller.run();
    }

    *//**
     * Stop polling the users audio level.
     *//*
    private void stopAudioLevelPoll() {
        handler.removeCallbacks(audioPoller);
        //volumeBar.setProgress(0);
    }
*/

    /* State Logic: IDLE -> LISTENING -> PROCESSING -> repeat */
    public enum State {
        IDLE,
        LISTENING,
        PROCESSING
    }
    /**
     * Set the state and update the button text.
     */
    private void setState(State newState) {
        state = newState;
        // Create map for params
        WritableMap payload = Arguments.createMap();
        // Put data to map
        payload.putString("state", newState.toString());

        this.getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onStateChange",payload);

    }

    /* Earcons */
    // TODO: implement earcons
    private void loadEarcons() {
        //Load all of the earcons from disk
       /* startEarcon = new Audio(this, R.raw.sk_start, Configuration.PCM_FORMAT);
        stopEarcon = new Audio(this, R.raw.sk_stop, Configuration.PCM_FORMAT);
        errorEarcon = new Audio(this, R.raw.sk_error, Configuration.PCM_FORMAT);*/
    }

    /* Helpers */
    private DetectionType ThisDetectionToDetectionType(String id) {
        if(id.toLowerCase().equals(DetectionType.Long.toString().toLowerCase())) {
            return DetectionType.Long;
        }
        if(id.toLowerCase().equals(DetectionType.Short.toString().toLowerCase())) {
            return DetectionType.Short;
        }
        if(id.toLowerCase().equals(DetectionType.None.toString().toLowerCase())) {
            return DetectionType.None;
        }
        return null;
    }

    private RecognitionType ThisRecoTypeToRecoType(String id) {
        if(id.toLowerCase().equals(RecognitionType.DICTATION.toString().toLowerCase())) {
            return RecognitionType.DICTATION;
        }
        if(id.toLowerCase().equals(RecognitionType.SEARCH.toString().toLowerCase())) {
            return RecognitionType.SEARCH;
        }
        if(id.toLowerCase().equals(RecognitionType.TV.toString().toLowerCase())) {
            return RecognitionType.TV;
        }
        return null;
    }
}