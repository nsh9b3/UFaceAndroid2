package com.stuff.nsh9b3.ufaceandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnAsyncTaskComplete
{
    // List of Buttons (services) a user can select
    public static ArrayList<Button> buttonList;

    // List of layouts (rows of services) to place new services
    private ArrayList<LinearLayout> layoutList;

    private Button addButton;
    private Button batchButton;

    // These are offset values so btns and layouts have different IDs
    private final static int btnIDOffset = 1000;
    private final static int layIDOffset = 100;

    public static ArrayList<WebService> serviceList;

    public static Paillier paillier;

    private boolean gotKey = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the add button and set the listener to this activity (which is a clickListener)
        addButton = (Button)findViewById(R.id.btn_add);
        addButton.setOnClickListener(this);

        batchButton = (Button)findViewById(R.id.btn_run_batch);
        batchButton.setOnClickListener(this);

        getServices();

        try
        {
            File timeSheet = Utilities.createTimeSheet(this);

            FileWriter fWriter;
            fWriter = new FileWriter(timeSheet, true);
            fWriter.write("Starting Tests!\n");
            fWriter.flush();
            fWriter.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        paillier = null;
        GetPublicKey getPublicKey = new GetPublicKey(this, paillier);
        getPublicKey.execute();
    }

    @Override
    public void onClick(View view)
    {
        // First figure out what was pressed
        switch(view.getId())
        {
            // If the add button was pressed, register a new web service
            case R.id.btn_add:
                Intent newServiceIntent = new Intent(this, SelectNewService.class);
                startActivityForResult(newServiceIntent, IntentKeys.SELECT_NEW_SERVICE);
                break;
            case R.id.btn_run_batch:
                Intent batchIntent = new Intent(this, RunBatch.class);
                startActivity(batchIntent);
                break;
            // Otherwise, authenticate a user on a specific web service
            default:
                WebService selectedService = null;
                Button selectedButton = (Button)view;
                for(WebService service : serviceList)
                {
                    if(service.serviceName.compareTo(selectedButton.getText().toString()) == 0)
                    {
                        selectedService = service;
                        break;
                    }
                }
                Intent authServiceIntent = new Intent(this, AuthenticateWebService.class);
                authServiceIntent.putExtra(IntentKeys.SERVICE_NAME, selectedService.serviceName);
                authServiceIntent.putExtra(IntentKeys.SERVICE_ADDRESS, selectedService.serviceAddress);
                authServiceIntent.putExtra(IntentKeys.USER_NAME, selectedService.userName);
                authServiceIntent.putExtra(IntentKeys.USER_INDEX, selectedService.userIndex);
                startActivityForResult(authServiceIntent, IntentKeys.AUTH_SERVICE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IntentKeys.SELECT_NEW_SERVICE)
        {
            if(resultCode == RESULT_OK)
            {
                Bundle extras = data.getExtras();

                if(extras.getBoolean(IntentKeys.REGISTRATION_PASS))
                {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPref.edit();

                    WebService newService = new WebService(extras.getString(IntentKeys.SERVICE_NAME),
                            extras.getString(IntentKeys.SERVICE_ADDRESS),
                            extras.getString(IntentKeys.USER_NAME),
                            extras.getInt(IntentKeys.USER_INDEX));

                    // Add to list of services
                    serviceList.add(newService);

                    Gson gson = new Gson();
                    String json = gson.toJson(newService);
                    Set<String> servList = sharedPref.getStringSet(SharedPrefKeys.SERVICE_LIST, new HashSet<String>());
                    servList.add(json);
                    editor.putStringSet(SharedPrefKeys.SERVICE_LIST, servList);
                    editor.apply();

                    makeNewServiceIcon(extras.getString(IntentKeys.SERVICE_NAME));
                }
            }
        }
    }

    private void getServices()
    {
        buttonList = new ArrayList<>();
        layoutList = new ArrayList<>();
        serviceList = new ArrayList<>();

        /*
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> servList = sharedPref.getStringSet(SharedPrefKeys.SERVICE_LIST, new HashSet<String>());

        for (Iterator<String> it = servList.iterator(); it.hasNext(); ) {
            // Get the name
            String json = it.next();

            // Get the object from the name
            Gson gson = new Gson();
            WebService webService = gson.fromJson(json, WebService.class);

            // Add to list of services
            serviceList.add(webService);

            // Create an icon on the home screen
            makeNewServiceIcon(webService.serviceName);
        }
        */
    }

    private void makeNewServiceIcon(String serviceName)
    {
        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.ll_parent);

        int col = buttonList.size() % 3;

        LinearLayout childLayout;

        // Make a new row to place the button
        if(col == 0)
        {
            childLayout = new LinearLayout(this);
            childLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            childLayout.setLayoutParams(layoutParams);
            childLayout.setWeightSum(3);

            childLayout.setId(layoutList.size() + layIDOffset);

            parentLayout.addView(childLayout);
            layoutList.add(childLayout);
        }
        // Grab an existing location for the button
        else
        {
            childLayout = (LinearLayout)findViewById(layoutList.get((layoutList.size() - 1)).getId());
        }

        Button newServiceBtn = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(new GridView.LayoutParams(0, (int)(getResources().getDisplayMetrics().density * 100 + 0.5f)));
        btnParams.setMargins((int)getResources().getDimension(R.dimen.activity_horizontal_margin),
                (int)getResources().getDimension(R.dimen.activity_vertical_margin),
                (int)getResources().getDimension(R.dimen.activity_horizontal_margin),
                (int)getResources().getDimension(R.dimen.activity_vertical_margin));
        btnParams.weight = 1;

        newServiceBtn.setLayoutParams(btnParams);
        newServiceBtn.setText(serviceName);
        newServiceBtn.setId(buttonList.size() + btnIDOffset);

        if(gotKey)
            newServiceBtn.setEnabled(true);
        else
            newServiceBtn.setEnabled(false);
        newServiceBtn.setOnClickListener(this);

        childLayout.addView(newServiceBtn);
        buttonList.add(newServiceBtn);
    }

    @Override
    public void onTaskCompleted(Object obj)
    {
        this.paillier = (Paillier) obj;
        if(paillier != null)
        {
            // Allow user to add new Service or authenticate with existing
            addButton.setEnabled(true);
            batchButton.setEnabled(true);
            gotKey = true;

            for(Button btn : buttonList)
            {
                btn.setEnabled(true);
            }
        }
        else
        {
            Toast.makeText(getBaseContext(), "Could not get the public key!", Toast.LENGTH_LONG).show();
        }
    }
}
