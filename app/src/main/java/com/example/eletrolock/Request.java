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
            URL uri = new URL(url);
            con = (HttpURLConnection) uri.openConnection();

            con.setRequestMethod(method);
            setHeaders();
            if(method.equals("POST")) {
               OutputStream os = con.getOutputStream();
               os.write(body.getBytes());
               os.close();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer buf = new StringBuffer();

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