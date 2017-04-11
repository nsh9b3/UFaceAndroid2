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
 * Created by nick on 11/22/16.
 */

public class BeginAuthentication extends AsyncTask
{
    OnAsyncTaskComplete listener;
    WebService webService;
    boolean result;

    public BeginAuthentication(OnAsyncTaskComplete listener, WebService webService)
    {
        this.listener = listener;
        this.webService = webService;
        this.result = false;
    }

    @Override
    protected Object doInBackground(Object[] objects)
    {
        // Create the string to contact the web service for authentication
        StringBuilder sb = new StringBuilder();
        sb.append(webService.serviceAddress);
        sb.append(Configurations.UFACE_AUTHENTICATE_USER);

        URL url;
        HttpURLConnection conn = null;

        try
        {
            // Connect to the web service to say you want to begin an authentication attempt
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            // Create the json object to transfer
            JSONObject jObject = new JSONObject();
            jObject.accumulate(Configurations.SERVICE_USER_KEY, webService.userName);
            jObject.accumulate(Configurations.SERVICE_SERVICE_KEY, webService.serviceName);
            String jUser = jObject.toString();

            // Send the json object to the web service
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jUser);
            writer.flush();
            writer.close();
            os.close();

            // Get the result from the web service for if authentication can begin
            InputStream is = new BufferedInputStream(conn.getInputStream());
            String response = Utilities.convertStreamToString(is).replaceAll("\\\\", "");
            response = response.substring(1, response.length() - 2);
            JSONObject jResponse = new JSONObject(response);
            result = jResponse.getBoolean("Result");
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
            jObject.accumulate(AsyncTaskKeys.GET_TASK, AsyncTaskKeys.AUTH_USER);
            jObject.accumulate(AsyncTaskKeys.GET_RESULT, result);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        listener.onTaskCompleted(jObject);
    }
}
