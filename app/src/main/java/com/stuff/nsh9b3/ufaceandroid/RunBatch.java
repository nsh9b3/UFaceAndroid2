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

import static com.stuff.nsh9b3.ufaceandroid.MainActivity.picPath;

public class RunBatch extends AppCompatActivity implements OnAsyncTaskComplete
{
    int count = 1;
    int origIndex = 0;
    int testIndex = 0;
    int endOrigIndex = 0;
    int endTextIndex = 0;

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

    String[] origImages;
    String[] testImages;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_batch);

        setupImages();

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
            Toast.makeText(this, "Starting Batch", Toast.LENGTH_LONG).show();
            startRegistering();
        }
    }

    private void startRegistering()
    {
        Log.d("BATCH", "startRegistering");
        serviceName = "Blank";
        serviceAddress = "http://" + Configurations.UFACE_BANK_ADDRESS + "/";
        userIndex = -1;
        origPassword = generatePassword(origImages[origIndex]);
        userName = origImages[origIndex].split("/")[origImages[origIndex].split("/").length - 1];
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
        testPassword = generatePassword(testImages[testIndex]);
        String testName = testImages[testIndex].split("/")[testImages[testIndex].split("/").length - 1];
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
                    testPassword = generatePassword(testImages[testIndex]);

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
        for(String path : origImages)
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
            for(String path : testImages)
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

    private void setupImages()
    {
        origImages = new String[]{
                picPath + "Abira-c-1.jpeg",
                picPath + "Adam-c-1.jpg",
                picPath + "Ande-c-1.jpeg",
                picPath + "Atoosa-c-1.jpg",
                picPath + "Ben-c-1.jpg",
                picPath + "Devin-c-1.jpg",
                picPath + "Doug-c-1.jpg",
                picPath + "Dude-c-1.jpg",
                picPath + "Hug-c-1.jpg",
                picPath + "Jess-c-1.jpg",
                picPath + "Jiang-c-1.jpg",
                picPath + "Kat-c-1.jpg",
                picPath + "Kyle-c-1.jpg",
                picPath + "Mel-c-1.jpg",
                picPath + "Mike-c-1.jpg",
                picPath + "Nick-c-1.jpg",
                picPath + "Rand-c-1.jpg",
                picPath + "Sahi-c-1.jpg",
                picPath + "Sam-c-1.jpg",
                picPath + "Snehi-c-1.jpg"
        };
        testImages = new String[]{
                picPath + "Abira-c-2.jpeg",
                picPath + "Abira-z-1.jpeg",
                picPath + "Adam-c-2.jpg",
                picPath + "Adam-z-1.jpg",
                picPath + "Ande-c-2.jpeg",
                picPath + "Ande-z-1.jpeg",
                picPath + "Atoosa-c-2.jpg",
                picPath + "Atoosa-z-1.jpg",
                picPath + "Ben-c-2.jpg",
                picPath + "Ben-z-1.jpg",
                picPath + "Devin-c-2.jpg",
                picPath + "Devin-z-1.jpg",
                picPath + "Doug-c-2.jpg",
                picPath + "Doug-z-1.jpg",
                picPath + "Dude-c-2.jpg",
                picPath + "Dude-z-1.jpg",
                picPath + "Hug-c-2.jpg",
                picPath + "Hug-z-1.jpg",
                picPath + "Jess-c-2.jpg",
                picPath + "Jess-z-1.jpg",
                picPath + "Jiang-c-2.jpg",
                picPath + "Jiang-z-1.jpg",
                picPath + "Kat-c-2.jpg",
                picPath + "Kat-z-1.jpg",
                picPath + "Kyle-c-2.jpg",
                picPath + "Kyle-z-1.jpg",
                picPath + "Mel-c-2.jpg",
                picPath + "Mel-z-1.jpg",
                picPath + "Mike-c-2.jpg",
                picPath + "Mike-z-1.jpg",
                picPath + "Nick-c-2.jpg",
                picPath + "Nick-z-1.jpg",
                picPath + "Rand-c-2.jpg",
                picPath + "Rand-z-1.jpg",
                picPath + "Sahi-c-2.jpg",
                picPath + "Sahi-z-1.jpg",
                picPath + "Sam-c-2.jpg",
                picPath + "Sam-z-1.jpg",
                picPath + "Snehi-c-2.jpg",
                picPath + "Snehi-z-1.jpg"
        };
        endOrigIndex = origImages.length;
        endTextIndex = testImages.length;
    }
}
