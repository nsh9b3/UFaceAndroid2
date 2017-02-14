package com.stuff.nsh9b3.ufaceandroid;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by nick on 11/21/16.
 */

public class BeginRegistration extends AsyncTask
{
    OnAsyncTaskComplete listener;
    String address;
    String userID;
    String serviceName;
    int userIndex;
    boolean result;

    public BeginRegistration(OnAsyncTaskComplete listener, String address, String userID, String serviceName, int userIndex)
    {
        this.listener = listener;
        this.address = address;
        this.userID = userID;
        this.serviceName = serviceName;
        this.userIndex = userIndex;
        this.result = false;
    }
    @Override
    protected Object doInBackground(Object[] objects)
    {
        // Create the string to obtain the public key
        StringBuilder sb = new StringBuilder();
        sb.append(address);
        sb.append(Configurations.SERVICE_ADD_USER);

        URL url;
        HttpURLConnection conn = null;

        try
        {
            // Connect to the data server to validate user name
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            // Create the json object to transfer
            JSONObject jObject = new JSONObject();
            jObject.accumulate(Configurations.SERVICE_USER_KEY, userID);
            jObject.accumulate(Configurations.SERVICE_SERVICE_KEY, serviceName);
            String jUser = jObject.toString();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jUser);
            writer.flush();

            writer.close();
            os.close();

            InputStream is = new BufferedInputStream(conn.getInputStream());
            String response = Utilities.convertStreamToString(is).replaceAll("\\\\", "");
            response = response.substring(1, response.length() - 2);
            JSONObject jResponse = new JSONObject(response);
            result = jResponse.getBoolean("Result");
            if(result)
            {
                userIndex = jResponse.getInt("Index");
            }
            else
            {
                userIndex = -1;
            }

        } catch(MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (JSONException e)
        {
            e.printStackTrace();
        } finally
        {
            conn.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o)
    {
        JSONObject jObject = new JSONObject();

        try
        {
            jObject.accumulate(AsyncTaskKeys.GET_TASK, AsyncTaskKeys.REG_USER);
            jObject.accumulate(AsyncTaskKeys.GET_RESULT, result);
            jObject.accumulate(AsyncTaskKeys.USER_INDEX, userIndex);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        listener.onTaskCompleted(jObject);
    }
}
