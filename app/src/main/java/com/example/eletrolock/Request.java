package com.example.eletrolock;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Request implements Runnable {

    protected String method, url, body, response;
    protected HttpURLConnection con;
    public boolean done = false;

    protected Request(String _method, String _url, String _body) {
        method = _method;
        url = _url;
        body = _body;
    }

    protected abstract void setHeaders();

    public final void run() {
        try {
            Log.d("httpreq", "openning connection");
            URL uri = new URL(url);
            con = (HttpURLConnection) uri.openConnection();

            Log.d("httpreq", "setting up");
            con.setRequestMethod(method);
            setHeaders();

            Log.d("httpreq", "sending body if appropriate");
            if(method.equals("POST")) {
               OutputStream os = con.getOutputStream();
               os.write(body.getBytes());
               os.close();
            }

            Log.d("httpreq", "reading response");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer buf = new StringBuffer();

            Log.d("httpreq", "merda");
            while((line = in.readLine()) != null) {
               buf.append(line);
            }

            response = buf.toString();

        } catch (Exception e) {
            response = "";
            Log.d("httpreq", "from " + url + " e: " + e);
        }
        done = true;
    }

    public String getResponse() {
        return response;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }
}