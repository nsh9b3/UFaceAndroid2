package com.stuff.nsh9b3.ufaceandroid;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by nick on 11/21/16.
 */

public class GetServiceList extends AsyncTask
{
    OnAsyncTaskComplete listener;
    List<WebService> services;

    public GetServiceList(OnAsyncTaskComplete listener, List<WebService> services)
    {
        this.listener = listener;
        this.services = services;
    }

    @Override
    protected Object doInBackground(Object[] objects)
    {
        // Create the string to obtain the public key
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Configurations.UFACE_DATA_ADDRESS);
        sb.append("/");
        sb.append(Configurations.UFACE_SERVICE_LIST);

        // Create a link between Client and UFace key server to obtain public key
        URL url;
        HttpURLConnection conn = null;
        try
        {
            // Open the URL for the public key
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
            br.close();

            // Something is odd about the JSON and adds a whole bunch of '\'s
            String response = sb.toString().replaceAll("\\\\", "");
            response = response.substring(1, response.length() - 2);
            JSONObject jResponse = new JSONObject(response);
            JSONArray jServices = jResponse.getJSONArray(Configurations.UFACE_SERVICE_LIST_NAME);

            for(int i = 0; i < jServices.length(); i++)
            {
                JSONObject jService = jServices.getJSONObject(i);
                WebService service = new WebService(jService.getString("Name"), jService.getString("Url"), "", -1);
                services.add(service);
            }

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
        listener.onTaskCompleted(services);
    }
}
