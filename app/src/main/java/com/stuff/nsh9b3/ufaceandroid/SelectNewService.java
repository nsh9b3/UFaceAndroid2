package com.stuff.nsh9b3.ufaceandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class SelectNewService extends AppCompatActivity implements OnAsyncTaskComplete
{
    ListView listViewServices;
    TextView textViewSelectedService;
    ArrayAdapter<String> adapterServices;
    Button btnSaveSelection;
    List<WebService> services;
    int selectedServiceIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_new_service);

        listViewServices = (ListView)findViewById(R.id.lv_services);
        textViewSelectedService = (TextView)findViewById(R.id.tv_selectedService);
        btnSaveSelection = (Button)findViewById(R.id.btn_saveSelection);

        btnSaveSelection.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                WebService selectedService = services.get(selectedServiceIndex);
                Intent registerServiceIntent = new Intent(getBaseContext(), RegisterWebService.class);
                registerServiceIntent.putExtra(IntentKeys.SERVICE_NAME, selectedService.serviceName);
                registerServiceIntent.putExtra(IntentKeys.SERVICE_ADDRESS, selectedService.serviceAddress);
                startActivityForResult(registerServiceIntent, IntentKeys.REGISTER_NEW_SERVICE);
            }
        });

        services = new ArrayList<>();
        GetServiceList getServiceList = new GetServiceList(this, services);
        getServiceList.execute();
    }

    @Override
    public void onTaskCompleted(Object obj)
    {
        services = (ArrayList<WebService>) obj;
        // Remove any service from the list if it's already been registered with
        for(ListIterator<WebService> iter = MainActivity.serviceList.listIterator(); iter.hasNext();)
        {
            WebService savedService = iter.next();
            for(WebService service : services)
            {
                if(service.serviceName.compareTo(savedService.serviceName) == 0)
                {
                    services.remove(service);
                }
            }
        }

        List<String> serviceNames = new ArrayList<>();
        for(WebService service : services)
        {
            serviceNames.add(service.serviceName);
        }

        // Show the list to the user
        adapterServices = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, serviceNames);
        listViewServices.setAdapter(adapterServices);
        listViewServices.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                String serviceName = (String)adapterView.getItemAtPosition(i);
                selectedServiceIndex = i;
                textViewSelectedService.setText(serviceName);
                btnSaveSelection.setEnabled(true);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == IntentKeys.REGISTER_NEW_SERVICE)
        {
            if(resultCode == RESULT_OK)
            {
                Bundle extras = data.getExtras();

                Intent doneRegistering = new Intent();
                if(extras.getBoolean(IntentKeys.REGISTRATION_PASS))
                {
                    doneRegistering.putExtra(IntentKeys.USER_NAME, extras.getString(IntentKeys.USER_NAME));
                    doneRegistering.putExtra(IntentKeys.USER_INDEX, extras.getInt(IntentKeys.USER_INDEX));
                    doneRegistering.putExtra(IntentKeys.SERVICE_NAME, extras.getString(IntentKeys.SERVICE_NAME));
                    doneRegistering.putExtra(IntentKeys.SERVICE_ADDRESS, extras.getString(IntentKeys.SERVICE_ADDRESS));
                    doneRegistering.putExtra(IntentKeys.REGISTRATION_PASS, extras.getBoolean(IntentKeys.REGISTRATION_PASS));
                }
                else
                {
                    doneRegistering.putExtra(IntentKeys.REGISTRATION_PASS, extras.getBoolean(IntentKeys.REGISTRATION_PASS));
                }
                setResult(Activity.RESULT_OK, doneRegistering);
                finish();
            }
        }
    }

}
