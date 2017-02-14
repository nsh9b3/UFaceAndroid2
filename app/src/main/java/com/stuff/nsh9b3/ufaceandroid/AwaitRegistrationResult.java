package com.stuff.nsh9b3.ufaceandroid;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by nick on 11/21/16.
 */

public class AwaitRegistrationResult extends AsyncTask
{
    OnAsyncTaskComplete listener;
    String serviceAddress;
    String userName;
    boolean result;
    boolean checkAgain;

    public AwaitRegistrationResult(OnAsyncTaskComplete listener, String serviceAddress, String userName)
    {
        this.listener = listener;
        this.serviceAddress = serviceAddress;
        this.result = false;
        this.checkAgain = false;
        this.userName = userName;
    }

    @Override
    protected Object doInBackground(Object[] objects)
    {
        // Create the string to obtain the public key
        StringBuilder sb = new StringBuilder();
        sb.append(serviceAddress);
        sb.append(Configurations.SERVICE_ADD_USER_RESULT);

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
            jObject.accumulate(Configurations.SERVICE_USER_KEY, userName);
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
            checkAgain = jResponse.getBoolean("Continue");

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
            jObject.accumulate(AsyncTaskKeys.GET_TASK, AsyncTaskKeys.AWAIT_REG_RESULT);
            jObject.accumulate(AsyncTaskKeys.GET_RESULT, result);
            jObject.accumulate(AsyncTaskKeys.CHECK_AGAIN, checkAgain);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        listener.onTaskCompleted(jObject);
    }
}
