package com.stuff.nsh9b3.ufaceandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * This activity is called when a user wants to authenticate with an ALREADY registered web service
 * It simply waits for the user to take a picture and then does the processing on the image before sending it to the data server
 * Then it waits for a response from the web service
 */
public class AuthenticateWebService extends AppCompatActivity implements OnAsyncTaskComplete, View.OnClickListener
{
    Button btnAuthenticate;

    WebService webService;
    String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate_web_service);

        Intent passedIntent = getIntent();
        Bundle extras = passedIntent.getExtras();

        // Sets up the proper values to be shown to the user.
        webService = new WebService(extras.getString(IntentKeys.SERVICE_NAME),
                extras.getString(IntentKeys.SERVICE_ADDRESS),
                extras.getString(IntentKeys.USER_NAME),
                extras.getInt(IntentKeys.USER_INDEX));

        TextView textViewService = (TextView)findViewById(R.id.tv_registered_service_name);
        textViewService.setText(webService.serviceName);

        TextView textViewUser = (TextView)findViewById(R.id.tv_registered_username);
        textViewUser.setText(webService.userName);

        btnAuthenticate = (Button)findViewById(R.id.btn_auth_user);
        btnAuthenticate.setOnClickListener(this);

        // This aysncTask first checks to make sure the web service can be authenticated with
        BeginAuthentication beginAuthentication = new BeginAuthentication(this, webService);
        beginAuthentication.execute();
    }

    @Override
    public void onClick(View view)
    {
        imagePath = Utilities.takePhoto(this);
    }

    @Override
    public void onTaskCompleted(Object obj)
    {
        String task = "";
        boolean result = false;
        JSONObject jObject = (JSONObject) obj;
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
            // If the web service is trying to be communicated with
            case AsyncTaskKeys.AUTH_USER:
                if(result)
                {
                    // If the web service is available and has told the data server to get ready then enable authentication
                    btnAuthenticate.setEnabled(true);
                }
                else
                {
                    Toast.makeText(getBaseContext(), "Can't begin authentication!", Toast.LENGTH_LONG).show();
                }
                break;
            // If the user is trying to send UPass to the UFace data server
            case AsyncTaskKeys.AUTH_PASS:
                if(result)
                {
                    Toast.makeText(getBaseContext(), "Awaiting response!", Toast.LENGTH_LONG).show();
                    AwaitAuthenticationResult awaitAuthenticationResult = new AwaitAuthenticationResult(this, webService);
                    awaitAuthenticationResult.execute();
                }
                else
                {
                    Toast.makeText(getBaseContext(), "Couldn't transmit authentication password!", Toast.LENGTH_LONG).show();
                }
                break;
            // If the user is waiting for the authentication result
            case AsyncTaskKeys.AWAIT_AUTH_RESULT:
                // If successfully authenticated within 1 minute
                if(result)
                {
                    Toast.makeText(getBaseContext(), "Successfully authenticated!", Toast.LENGTH_LONG).show();

                    Intent doneAuthentication = new Intent();
                    doneAuthentication.putExtra(IntentKeys.AUTHENTICATION_PASS, result);
                    setResult(Activity.RESULT_OK, doneAuthentication);
                    finish();
                }
                // If not successful authenticated within 1 minute
                else
                {
                    // First check to see if the check should occur again (ie. a result hasn't been reached and it has been less than a minute)
                    // This should be updated in the future, but it currently just keeps calling awaitAuthentication Result asyncTask until a result has been obtained
                    // The server tells it whether to check again or not (the server keeps track of the time passed)
                    // Again, this is not efficient and just keeps calling the asynctask over and over again
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
                        // If less than a minute, then try again
                        AwaitAuthenticationResult awaitAuthenticationResult = new AwaitAuthenticationResult(this, webService);
                        awaitAuthenticationResult.execute();
                    }
                    // Otherwise, the authentication failed
                    else
                    {
                        Toast.makeText(getBaseContext(), "Authentication failed!", Toast.LENGTH_LONG).show();

                        Intent doneAuthentication = new Intent();
                        doneAuthentication.putExtra(IntentKeys.AUTHENTICATION_PASS, result);
                        setResult(Activity.RESULT_OK, doneAuthentication);
                        finish();
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IntentKeys.REQUEST_TAKE_PHOTO)
        {
            if(resultCode == RESULT_OK)
            {
                // Creates UPass from the taken image
                Bitmap image = Utilities.resizeImage(imagePath);
                File file = new File(imagePath);
                if(file.exists())
                    file.delete();
                else
                    image = null;
                int[][] splitImage = Utilities.splitImageIntoSections(image);
                int[][] intFV = LBP.generateFeatureVector(splitImage);
                int[][] splitFV = Utilities.splitFVForEncryption(intFV);
                byte[][] byteFV = Utilities.createByteFV(splitFV);
                String password = Utilities.encryptFV(byteFV);

                // Begin authentication attempt
                AuthenticatePassword authenticatePassword = new AuthenticatePassword(this, webService, password);
                authenticatePassword.execute();
            }
        }
    }
}
