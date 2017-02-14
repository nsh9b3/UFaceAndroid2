package com.stuff.nsh9b3.ufaceandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RegisterWebService extends AppCompatActivity implements TextWatcher, View.OnClickListener, OnAsyncTaskComplete
{
    private Button btnRegister;
    private Button btnValidate;
    private EditText etUserID;
    private ProgressBar pbValidMark;
    private ImageView ivValidMark;

    private String webServiceAddress;
    private String webServiceName;
    private Map<String, Integer> userIDs;
    private String userID;
    private int userIndex;

    String imagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_web_service);

        Intent passedIntent = getIntent();
        Bundle extras = passedIntent.getExtras();
        webServiceAddress = extras.getString(IntentKeys.SERVICE_ADDRESS);
        webServiceName = extras.getString(IntentKeys.SERVICE_NAME);

        userIDs = new HashMap<>();

        btnRegister = (Button)findViewById(R.id.btn_register_take_photo);
        btnRegister.setOnClickListener(this);

        btnValidate = (Button)findViewById(R.id.btn_register_check_name);
        btnValidate.setOnClickListener(this);

        TextView tvServiceName = (TextView)findViewById(R.id.tv_register_service_name);
        tvServiceName.setText(webServiceName);

        etUserID = (EditText)findViewById(R.id.et_register_user_id);
        etUserID.addTextChangedListener(this);

        pbValidMark = (ProgressBar)findViewById(R.id.pb_register_is_valid);
        ivValidMark = (ImageView)findViewById(R.id.iv_register_valid_mark);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {
        pbValidMark.setVisibility(View.GONE);
        ivValidMark.setVisibility(View.GONE);
        btnRegister.setEnabled(false);
    }

    @Override
    public void afterTextChanged(Editable editable)
    {

    }

    @Override
    public void onClick(View view)
    {
        switch(view.getId())
        {
            case R.id.btn_register_check_name:
                pbValidMark.setVisibility(View.VISIBLE);
                ivValidMark.setVisibility(View.GONE);
                userID = etUserID.getText().toString();

                // If it's in the list already, it's already been checked
                if (userIDs.containsKey(userID))
                {
                    btnRegister.setEnabled(true);
                    pbValidMark.setVisibility(View.GONE);
                    ivValidMark.setVisibility(View.VISIBLE);
                    Toast.makeText(getBaseContext(), String.format("The name %s has already been verified as valid.", userID), Toast.LENGTH_LONG).show();
                } else
                {
                    BeginRegistration beginRegistration = new BeginRegistration(this, webServiceAddress, userID, webServiceName, userIndex);
                    beginRegistration.execute();
                }
                break;
            case R.id.btn_register_take_photo:
                userIndex = userIDs.get(userID);
                imagePath = Utilities.takePhoto(this);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        long[] time = new long[5];
        if(requestCode == IntentKeys.REQUEST_TAKE_PHOTO)
        {
            if(resultCode == RESULT_OK)
            {
                time[0] = System.currentTimeMillis();
                Bitmap image = Utilities.resizeImage(imagePath);
                File file = new File(imagePath);
                if(file.exists())
                    file.delete();
                else
                    image = null;
                int[][] splitImage = Utilities.splitImageIntoSections(image);
                time[1] = System.currentTimeMillis();
                int[][] intFV = LBP.generateFeatureVector(splitImage);
                time[2] = System.currentTimeMillis();
                int[][] splitFV = Utilities.splitFVForEncryption(intFV);
                byte[][] byteFV = Utilities.createByteFV(splitFV);
                time[3] = System.currentTimeMillis();
                String password = Utilities.encryptFV(byteFV);
                time[4] = System.currentTimeMillis();
                RegisterPassword registerPassword = new RegisterPassword(this, webServiceName, userIndex, password, Configurations.LABELS_IN_FEATURE_VECTOR);
                registerPassword.execute();
            }
        }
    }


    @Override
    public void onTaskCompleted(Object obj)
    {
        JSONObject jObject = (JSONObject) obj;
        String task = "";
        boolean result = false;
        try
        {
            task = jObject.getString(AsyncTaskKeys.GET_TASK);
            result = jObject.getBoolean(AsyncTaskKeys.GET_RESULT);
        } catch(JSONException e)
        {
            e.printStackTrace();
        }
        switch(task)
        {
            case AsyncTaskKeys.REG_USER:
                try
                {
                    userIndex = jObject.getInt(AsyncTaskKeys.USER_INDEX);
                } catch(JSONException e)
                {
                    e.printStackTrace();
                }

                pbValidMark.setVisibility(View.GONE);
                if(result)
                {
                    btnRegister.setEnabled(true);
                    userIDs.put(userID, userIndex);
                    ivValidMark.setVisibility(View.VISIBLE);
                    ivValidMark.setBackgroundResource(R.drawable.check);
                }
                else
                {
                    userIDs.remove(userID);
                    ivValidMark.setVisibility(View.VISIBLE);
                    ivValidMark.setBackgroundResource(R.drawable.close);

                    Toast.makeText(getBaseContext(), String.format("The name %s is already in use.", userID), Toast.LENGTH_LONG).show();
                }

                break;

            case AsyncTaskKeys.REG_PASS:
                if(result)
                {
                    Toast.makeText(getBaseContext(), "Awaiting response!", Toast.LENGTH_LONG).show();

                    AwaitRegistrationResult awaitRegistrationResult = new AwaitRegistrationResult(this, webServiceAddress, userID);
                    awaitRegistrationResult.execute();
                }
                else
                {
                    Toast.makeText(getBaseContext(), "Couldn't transmit registration password!", Toast.LENGTH_LONG).show();
                }
                break;
            case AsyncTaskKeys.AWAIT_REG_RESULT:
                if(result)
                {
                    Toast.makeText(getBaseContext(), "Successfully registered!", Toast.LENGTH_LONG).show();

                    Intent doneRegistering = new Intent();
                    doneRegistering.putExtra(IntentKeys.USER_NAME, userID);
                    doneRegistering.putExtra(IntentKeys.USER_INDEX, userIndex);
                    doneRegistering.putExtra(IntentKeys.SERVICE_NAME, webServiceName);
                    doneRegistering.putExtra(IntentKeys.SERVICE_ADDRESS, webServiceAddress);
                    doneRegistering.putExtra(IntentKeys.REGISTRATION_PASS, result);
                    setResult(Activity.RESULT_OK, doneRegistering);
                    finish();
                }
                else
                {
                    boolean checkAgain = false;
                    try
                    {
                        checkAgain = jObject.getBoolean(AsyncTaskKeys.CHECK_AGAIN);
                    } catch(JSONException e)
                    {
                        e.printStackTrace();
                    }
                    if(checkAgain)
                    {
                        AwaitRegistrationResult awaitRegistrationResult = new AwaitRegistrationResult(this, webServiceAddress, userID);
                        awaitRegistrationResult.execute();
                    }
                    else
                    {
                        Toast.makeText(getBaseContext(), "Registration failed!", Toast.LENGTH_LONG).show();

                        Intent doneRegistering = new Intent();
                        doneRegistering.putExtra(IntentKeys.USER_NAME, userID);
                        doneRegistering.putExtra(IntentKeys.USER_INDEX, userIndex);
                        doneRegistering.putExtra(IntentKeys.SERVICE_NAME, webServiceName);
                        doneRegistering.putExtra(IntentKeys.SERVICE_ADDRESS, webServiceAddress);
                        doneRegistering.putExtra(IntentKeys.REGISTRATION_PASS, result);
                        setResult(Activity.RESULT_OK, doneRegistering);
                        finish();
                    }
                }
                break;
        }
    }

}
