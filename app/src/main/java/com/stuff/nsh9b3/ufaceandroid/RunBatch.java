package com.stuff.nsh9b3.ufaceandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;

public class RunBatch extends AppCompatActivity implements OnAsyncTaskComplete
{
    int count = 1;
    int origIndex = 0;
    int testIndex = 0;
    int endOrigIndex = Configurations.origImages.length;
    int endTextIndex = Configurations.testImages.length;

    WebService service;
    String serviceName;
    String serviceAddress;
    String userName;
    int userIndex;
    String origPassword;
    String testPassword;
    File timeSheet;
    long[] time = new long[5];

    TextView tvOrigImage;
    TextView tvTestImage;
    TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_batch);

        tvOrigImage = (TextView)findViewById(R.id.tv_orig_image);
        tvTestImage = (TextView)findViewById(R.id.tv_test_image);
        tvCount = (TextView)findViewById(R.id.tv_count_number);

        if(checkFiles())
        {
            try
            {
                timeSheet = Utilities.createTimeSheet(this);

                FileWriter fWriter;
                fWriter = new FileWriter(timeSheet, true);
                fWriter.write("Starting Tests!\n");
                fWriter.flush();
                fWriter.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            startRegistering();
        }
    }

    private void startRegistering()
    {
        Log.d("BATCH", "startRegistering");
        serviceName = "Bank";
        serviceAddress = "http://" + Configurations.UFACE_BANK_ADDRESS + "/";
        userIndex = -1;
        origPassword = generatePassword(Configurations.origImages[origIndex]);
        userName = Configurations.origImages[origIndex].split("/")[Configurations.origImages[origIndex].split("/").length - 1];
        tvOrigImage.setText(userName);
        /*Random rand = new Random();
        userName = userName + "-" + rand.nextInt();*/
        tvCount.setText("Count: " + count++);

        try
        {
            FileWriter fWriter;
            fWriter = new FileWriter(timeSheet, true);
            fWriter.write("\n\n------------------------------------------------------------------------------------\nUser: " + userName + "\n");
            fWriter.flush();
            fWriter.close();
        } catch(IOException e)
        {
            e.printStackTrace();
        }

        printTime();

        BeginRegistration beginRegistration = new BeginRegistration(this, serviceAddress, userName, serviceName, userIndex);
        beginRegistration.execute();
    }

    private void startAuthenticating()
    {
        Log.d("BATCH", "startAuthenticating");
        testPassword = generatePassword(Configurations.testImages[testIndex]);
        String testName = Configurations.testImages[testIndex].split("/")[Configurations.testImages[testIndex].split("/").length - 1];
        tvTestImage.setText(testName);

        try
        {
            FileWriter fWriter;
            fWriter = new FileWriter(timeSheet, true);
            fWriter.write("Test: " + testName + "\n");
            fWriter.flush();
            fWriter.close();
        } catch(IOException e)
        {
            e.printStackTrace();
        }

        printTime();

        BeginAuthentication beginAuthentication = new BeginAuthentication(this, service);
        beginAuthentication.execute();
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
                if(result)
                {
                    try
                    {
                        userIndex = jObject.getInt(AsyncTaskKeys.USER_INDEX);
                        service = new WebService(serviceName, serviceAddress, userName, userIndex);
                    } catch(JSONException e)
                    {
                        e.printStackTrace();
                    }
                    RegisterPassword registerPassword = new RegisterPassword(this, serviceName, userIndex, origPassword, Configurations.LABELS_IN_FEATURE_VECTOR);
                    registerPassword.execute();
                }
                break;
            case AsyncTaskKeys.REG_PASS:
                if(result)
                {
                    AwaitRegistrationResult awaitRegistrationResult = new AwaitRegistrationResult(this, serviceAddress, userName);
                    awaitRegistrationResult.execute();
                }
                break;
            case AsyncTaskKeys.AWAIT_REG_RESULT:
                if(result)
                {
                    startAuthenticating();
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
                        AwaitRegistrationResult awaitRegistrationResult = new AwaitRegistrationResult(this, serviceAddress, userName);
                        awaitRegistrationResult.execute();
                    }
                    else
                    {
                        // FAIL
                        tvCount.setText("Failed on " + tvCount);
                    }
                }
                break;
            case AsyncTaskKeys.AUTH_USER:
                if(result)
                {
                    testPassword = generatePassword(Configurations.testImages[testIndex]);

                    AuthenticatePassword authenticatePassword = new AuthenticatePassword(this, service, testPassword);
                    authenticatePassword.execute();
                }
                break;
            case AsyncTaskKeys.AUTH_PASS:
                if(result)
                {
                    AwaitAuthenticationResult awaitAuthenticationResult = new AwaitAuthenticationResult(this, service);
                    awaitAuthenticationResult.execute();
                }
                break;
            case AsyncTaskKeys.AWAIT_AUTH_RESULT:
                if(result)
                {
                    testIndex++;
                    if(testIndex == endTextIndex)
                    {
                        testIndex = 0;
                        origIndex++;
                        if(origIndex == endOrigIndex)
                        {
                            // DONE
                            tvCount.setText("Done");
                        }
                        else
                        {
                            // New start image
                            startRegistering();
                        }
                    }
                    else
                    {
                        tvCount.setText("Count: " + count++);
                        // New test image
                        startAuthenticating();
                    }
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
                        AwaitAuthenticationResult awaitAuthenticationResult = new AwaitAuthenticationResult(this, service);
                        awaitAuthenticationResult.execute();
                    }
                    else
                    {
                        testIndex++;
                        if(testIndex == endTextIndex)
                        {
                            testIndex = 0;
                            origIndex++;
                            if(origIndex == endOrigIndex)
                            {
                                // DONE
                                tvCount.setText("Done");
                            }
                            else
                            {
                                // New start image
                                startRegistering();
                            }
                        }
                        else
                        {
                            tvCount.setText("Count: " + count++);
                            // New test image
                            startAuthenticating();
                        }
                    }
                }
                break;
        }
    }

    private String generatePassword(String imagePath)
    {
        File file = new File(imagePath);
        String password = null;
        if(file.exists())
        {
            time[0] = System.currentTimeMillis();
            Bitmap image = Utilities.resizeImage(imagePath);
            int[][] splitImage = Utilities.splitImageIntoSections(image);
            time[1] = System.currentTimeMillis();
            int[][] intFV = LBP.generateFeatureVector(splitImage);
            time[2] = System.currentTimeMillis();
            int[][] splitFV = Utilities.splitFVForEncryption(intFV);
            byte[][] byteFV = Utilities.createByteFV(splitFV);
            time[3] = System.currentTimeMillis();
            password =  Utilities.encryptFV(byteFV);
            time[4] = System.currentTimeMillis();
        }
        return password;
    }

    private void printTime()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\tResize-Split: \t").append(time[1] - time[0])
                .append("\tLBP: \t").append(time[2] - time[1])
                .append("\tDataManip: \t").append(time[3] - time[2])
                .append("\tEncrypt: \t").append(time[4] - time[3])
                .append("\tTotal: \t").append(time[4] - time[0]).append("\n");
        try
        {
            FileWriter fWriter;
            fWriter = new FileWriter(timeSheet, true);
            fWriter.write(sb.toString());
            fWriter.flush();
            fWriter.close();
        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean checkFiles()
    {
        boolean isGood = true;
        for(String path : Configurations.origImages)
        {
            File file = new File(path);
            if(!file.exists())
            {
                Toast.makeText(this, path + " doesn't exists - in origImages!", Toast.LENGTH_LONG).show();
                isGood = false;
                break;
            }
        }
        if(isGood)
        {
            for(String path : Configurations.testImages)
            {
                File file = new File(path);
                if(!file.exists())
                {
                    Toast.makeText(this, path + " doesn't exists - in testImages!", Toast.LENGTH_LONG).show();
                    isGood = false;
                    break;
                }
            }
        }
        return isGood;
    }
}
