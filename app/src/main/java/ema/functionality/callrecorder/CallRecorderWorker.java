package ema.functionality.callrecorder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;

import static android.content.Context.MODE_PRIVATE;

public class CallRecorderWorker extends Worker {

    private String file_name = "Record";
    private String current_file_name;
    MediaRecorder recorder;
    File audiofile;
    String name, phonenumber;
    String audio_format;
    public String Audio_Type;
    private int audioSource;
    Context context;
    private Handler handler;
    Timer timer;
    Boolean offHook = false, ringing = false;
    Toast toast;
    Boolean isOffHook = false;
    private boolean recordstarted = false;
    private String ID;

    private IntentFilter filter;

    /***FireBase Storage Reference declaration***/
    private StorageReference mStorageRef;

    /***Shared Preferences called to get user ID from MainActivity***/
    private SharedPreferences prefs;
    private String sharedPrefFile = "ema.functionality.callrecorder";

    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";

    /***Work Result shown as a String***/
    private static final String WORK_RESULT = "work_result";

    private BroadcastReceiver br_call = new BroadcastReceiver() {

        Bundle bundle;
        String state;
        String inCall, outCall;
        public boolean wasRinging = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_IN)) {
                if ((bundle = intent.getExtras()) != null) {
                    state = bundle.getString(TelephonyManager.EXTRA_STATE);
                    if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        inCall = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        wasRinging = true;
                        Toast.makeText(context, "IN : " + inCall, Toast.LENGTH_LONG).show();
                    } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        if (wasRinging == true) {

                            Toast.makeText(context, "ANSWERED", Toast.LENGTH_LONG).show();

                            try {
                                String out = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss").format(Calendar.getInstance().getTime());
                                current_file_name = file_name + "_" + out;
                                audiofile = File.createTempFile(current_file_name, ".amr", getApplicationContext().getFilesDir());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String path = getApplicationContext().getFilesDir().toString();

                            recorder = new MediaRecorder();
//                          recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);

                            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            recorder.setOutputFile(audiofile.getAbsolutePath());
                            try {
                                recorder.prepare();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            recorder.start();
                            recordstarted = true;
                        }
                    } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        wasRinging = false;
                        Toast.makeText(context, "REJECT || DISCO", Toast.LENGTH_LONG).show();
                        if (recordstarted) {
                            recorder.stop();
                            recordstarted = false;
                            Uri file = Uri.fromFile(audiofile);
                            StorageReference fileRef = mStorageRef.child(ID + "/" + current_file_name);
                            Toast.makeText(getApplicationContext(), "URI : " + file, Toast.LENGTH_LONG).show();

                            /***The putFile() method used for file uploading***/
                            fileRef.putFile(file)
                                    .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                        @Override
                                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                            if (!task.isSuccessful()) {
                                                throw task.getException();
                                            }
                                            return mStorageRef.getDownloadUrl();
                                        }
                                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Upload complete!" , Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    }
                }
            } else if (intent.getAction().equals(ACTION_OUT)) {
                if ((bundle = intent.getExtras()) != null) {
                    outCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    Toast.makeText(context, "OUT : " + outCall, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    public CallRecorderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        /***Instantiate the Firebase Cloud Storage***/
        mStorageRef = FirebaseStorage.getInstance().getReference();

        /***Instantiate the Shared Preference and get the user ID from it***/
        prefs = getApplicationContext().getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        ID = prefs.getString("ID", ID);

        filter = new IntentFilter();
        filter.addAction(ACTION_OUT);
        filter.addAction(ACTION_IN);
        getApplicationContext().registerReceiver(br_call, filter);

        /***Build work result to return it - not very important aspect***/
        Data output = new Data.Builder().putString(WORK_RESULT, "Daily Tracker Started").build();
        return Result.success(output);
    }
}
