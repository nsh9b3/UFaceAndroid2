package com.stuff.nsh9b3.ufaceandroid;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by nick on 11/21/16.
 */

public class RegisterPassword extends AsyncTask
{
    OnAsyncTaskComplete listener;
    String serviceName;
    int userIndex;
    String password;
    int size;
    boolean result;

    public RegisterPassword(OnAsyncTaskComplete listener, String serviceName, int userIndex, String password, int size)
    {
        this.listener = listener;
        this.serviceName = serviceName;
        this.userIndex = userIndex;
        this.password = password;
        this.size = size;
        this.result = false;
    }

    @Override
    protected Object doInBackground(Object[] objects)
    {
        // Create the string to obtain the public key
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Configurations.UFACE_DATA_ADDRESS);
        sb.append("/");
        sb.append(Configurations.UFACE_REGISTRATION_PASSWORD);

        // Create a link between Client and UFace key server to obtain public key
        URL url;
        HttpURLConnection conn = null;
        try
        {
            // Connect to the data server to register password
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            // Create the json object to transfer
            JSONObject jObject = new JSONObject();
            jObject.accumulate(Configurations.SERVICE_USER_INDEX_KEY, userIndex);
            jObject.accumulate(Configurations.SERVICE_SERVICE_KEY, serviceName);
            jObject.accumulate(Configurations.PASSWORD_KEY, password);
            jObject.accumulate(Configurations.PASSWORD_SIZE_KEY, size);
            String jPass = jObject.toString();

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(jPass);
            writer.flush();

            writer.close();
            os.close();

            InputStream is = new BufferedInputStream(conn.getInputStream());
            String response = Utilities.convertStreamToString(is).replaceAll("\\\\", "");
            response = response.substring(1, response.length() - 2);
            JSONObject jResponse = new JSONObject(response);
            result = jResponse.getBoolean("Result");

        } catch (MalformedURLException e)
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
            jObject.accumulate(AsyncTaskKeys.GET_TASK, AsyncTaskKeys.REG_PASS);
            jObject.accumulate(AsyncTaskKeys.GET_RESULT, result);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        listener.onTaskCompleted(jObject);
    }
}
