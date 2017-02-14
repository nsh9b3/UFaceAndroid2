package com.stuff.nsh9b3.ufaceandroid;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by nick on 11/21/16.
 */

public class GetPublicKey extends AsyncTask
{
    OnAsyncTaskComplete listener;
    Paillier paillier;

    public GetPublicKey(OnAsyncTaskComplete listener, Paillier paillier)
    {
        this.listener = listener;
        this.paillier = paillier;
    }
    @Override
    protected Paillier doInBackground(Object[] objects)
    {
        // Create the string to obtain the public key
        StringBuilder sb = new StringBuilder();
        sb.append("http://");
        sb.append(Configurations.UFACE_KEY_ADDRESS);
        sb.append("/");
        sb.append(Configurations.UFACE_PUBLIC_KEY);

        // Create a link between Client and UFace key server to obtain public key
        URL url;
        HttpURLConnection conn = null;
        try
        {
            // Open the URL for the public key
            url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
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
            JSONObject jPublicKey = jResponse.getJSONObject(Configurations.UFACE_PUBLIC_KEY_NAME);

            paillier = new Paillier(jPublicKey.getString("n"), jPublicKey.getString("g"), jPublicKey.getString("size"));

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
        listener.onTaskCompleted(paillier);
    }
}
