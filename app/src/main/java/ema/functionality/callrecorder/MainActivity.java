package ema.functionality.callrecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    /****************Attributes and global variables declared here*****************/

    /***Application's main UI features***/
    private EditText ID;
    private Button BtnDaily;
    private TextView Status;

    /***Button manipulation variables***/
    private String id = "NOT_INITIALIZED";
    private String btnText = "Start Daily Tracker";

    /***A Shared Preferences to store information on user ID and button status to be restore on every launch***/
    private SharedPreferences mPreferences;
    private String sharedPrefFile = "ema.functionality.callrecorder";

    /***WorkManager variables that ensure automatic daily background task***/
    private WorkManager mWorkManager;
    private PeriodicWorkRequest workRequest;

    private static final int REQUEST_CODE = 0;
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /***Shared Preferences instantiated, user id and button status are updated here***/
        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        id = mPreferences.getString("ID", id);
        btnText = mPreferences.getString("BtnText", btnText);

        /***UI instantiation***/
        ID = findViewById(R.id.id);
        BtnDaily = findViewById(R.id.btn_daily);
        Status = findViewById(R.id.status);

        if (!id.equals("NOT_INITIALIZED"))
            ID.setText(id);

        /***A text changed listener with afterTextChanged() method implemented in order to automatically save ID right after the ID field is modified***/
        ID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                id = ID.getText().toString();
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                preferencesEditor.putString("ID",id);
                preferencesEditor.apply();
            }
        });

        try {
            // Initiate DevicePolicyManager.
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAdminName = new ComponentName(this, DeviceAdmin.class);

            if (!mDPM.isAdminActive(mAdminName)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Click on Activate button to secure your application.");
                startActivityForResult(intent, REQUEST_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /***WorkManager instantiated with a periodic work request that repeats everyday**
        mWorkManager = WorkManager.getInstance(MainActivity.this);
        workRequest = new PeriodicWorkRequest.Builder(CallRecorderWorker.class, 2, TimeUnit.MINUTES).build();
        mWorkManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    WorkInfo.State state = workInfo.getState();
                    Status.append(state.toString() + "\n");
                }
            }
        });*/

        /***Daily Tracker Button implemented here***/
        BtnDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mWorkManager.enqueueUniquePeriodicWork("Call Recorder", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
                Intent intent = new Intent(MainActivity.this, RecorderService.class);
                startService(intent);
                btnText = "Recorder Started";
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                preferencesEditor.putString("BtnText", btnText);
                preferencesEditor.apply();
                BtnDaily.setText(btnText);
                //finish();
            }
        });


    }
}
