package com.example.eletrolock;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeListActivity extends AppCompatActivity {

    UserSessionManager session;
    MasterFinder master;
    ExecutorService threadPool;
    Queue<Request> queue;

    String herokuURL = "https://bitlock-api.herokuapp.com";
    ArrayList<String> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        queue = new LinkedList<>();
        devices = new ArrayList<>();
        session = new UserSessionManager(getApplicationContext());
        threadPool = Executors.newSingleThreadExecutor();

        //Check if user is logged in, otherwise send him to login activity
        Boolean logged_in = session.checkIfLogin();

        if (!logged_in) {
            Intent triggerLogin = new Intent(this, LoginActivity.class);
            startActivity(triggerLogin);
        }

        setContentView(R.layout.activity_main);

        Handler refreshDevices = new Handler();
        refreshDevices.postDelayed(new Runnable() {
            @Override
            public void run() {
                queue.add(new HerokuRequest("GET", "/devices/" + session.getUnique(), session.getToken(), ""));
                threadPool.submit(queue.peek());
            }
        }, 5000);

        final Handler checkResponse = new Handler();
        checkResponse.postDelayed(new Runnable() {
            @Override
            public void run() {
                handleResponses();
                checkResponse.postDelayed(this, 500);
            }
        }, 500);

        master = new MasterFinder();
        Thread findMaster = new Thread(master);
        findMaster.start();
    }

    public void addDevice() {
        LayoutInflater inflater = getLayoutInflater();
        View popup = inflater.inflate(R.layout.activity_add_device, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        Button addButton = popup.findViewById(R.id.addDeviceButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(session.getMasterID() != null) {
                    queue.add(new MasterRequest("GET", "/devices", ""));
                    threadPool.submit(queue.peek());

                    ((AlertDialog)view.getParent()).cancel();
                }
            }
        });

        alertDialogBuilder.setTitle("Add device");
        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder.setView(popup);

        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    public void openDoor(View v) {
        String doorIdentifier = ((TextView)((View)v.getParent()).findViewById(R.id.deviceName)).getText().toString();

        queue.add(new HerokuRequest("PUT", "/devices/" + session.getUnique() + "/" + doorIdentifier, session.getToken(), ""));
        threadPool.submit(queue.peek());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_add_master:
                if(session.getMasterID().equals("")) {
                    queue.add(new MasterRequest("GET", "/", ""));
                    threadPool.submit(queue.peek());
                } else {
                    Toast.makeText(HomeListActivity.this, "Already registered master", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.action_add_device:
                addDevice();

                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    public void handleResponses() {
        if(!queue.isEmpty() && queue.peek().done) {
            Request request = queue.remove();
            JSONObject json = new JSONObject();
            if (request.getUrl().startsWith(herokuURL)) {
                try{
                    json = new JSONObject(request.response);
                } catch (JSONException e) {
                    Log.d("JSON", "ERROR: " + e);
                }
            }

            if(request.getUrl().equals(herokuURL + "/devices/" + session.getUnique() + "/master")) {
                try{
                    session.setMasterID(json.getString("master_id"));

                    queue.add(new MasterRequest("POST", "/", session.getMasterID()));
                    threadPool.submit(queue.peek());
                } catch (JSONException e) {
                    Log.d("JSON", "ERROR: " + e);
                }
            } else if (request.getUrl().equals(herokuURL + "/devices/" + session.getUnique())) {
                if(request.getMethod().equals("GET")) {
                    try {
                        JSONArray devices = json.getJSONArray("devices");
                        LinearLayout list = findViewById(R.id.homeListLayout);
                        LayoutInflater inflater = getLayoutInflater();

                        list.removeAllViews();

                        if(devices.length() == 0) {
                            TextView text = new TextView(HomeListActivity.this);
                            text.setText("No devices found");
                            list.addView(text);
                        } else {
                            for(int i = 0; i < devices.length(); i++) {
                                View temp = inflater.inflate(R.layout.element_devices, list, false);
                                ((TextView) temp.findViewById(R.id.deviceName)).setText(devices.getString(i));
                                ((TextView) temp.findViewById(R.id.statusTextChange)).setText("Open");
                                Button button = ((Button) temp.findViewById(R.id.openDoorButton));
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        openDoor(view);
                                    }
                                });
                                button.setText("Open");

                                list.addView(temp);
                            }
                        }
                    } catch (JSONException e) {
                        Log.d("JSON", "ERROR: " + e);
                    }
                } else {
                    queue.add(new HerokuRequest("GET", "/devices/" + session.getUnique(), session.getToken(), ""));
                    threadPool.submit(queue.peek());
                }
            } else if (request.getUrl().equals("http://" + master.getMasterIP() + "/")) {
                if(request.getMethod().equals("GET")) {
                    if(request.getResponse().equals("ITS A ME, BITLOCK!")) {
                        queue.add(new HerokuRequest("POST", "/devices/" + session.getUnique() + "/master", session.getToken(), ""));
                        threadPool.submit(queue.peek());
                    } else {
                        session.setMasterID(request.getResponse());
                    }

                } else {

                }
            } else if (request.getUrl().equals("http://" + master.getMasterIP() + "/devices")) {
                if(request.getMethod().equals("GET")) {
                    if(request.getResponse().equals("Device waiting to be registered")) {
                        queue.add(new MasterRequest("POST", "/devices", ((EditText) findViewById(R.id.editDeviceName)).getText().toString()));
                        threadPool.submit(queue.peek());
                    } else {
                        Toast.makeText(HomeListActivity.this, "No device to be registered", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if(request.getResponse().equals("DONE")) {
                        JSONObject body = new JSONObject();
                        try {
                            body.put("master_id", session.getMasterID());
                            body.put("identifier", ((EditText) findViewById(R.id.editDeviceName)).getText().toString());
                        } catch(Exception e) {
                            Log.d("JSON", "ERROR: " + e);
                        }

                        queue.add(new HerokuRequest("POST", "/devices/" + session.getUnique(), session.getToken(), body.toString()));
                        threadPool.submit(queue.peek());
                    }
                }
            }
        }
    }

    private class HerokuRequest extends Request {

        private String token;

        public HerokuRequest(String _method, String _file, String _token, String _body) {
            super(_method, herokuURL + _file, _body);
            token = _token;
            Log.d("token", token);
        }

        protected void setHeaders() {
            con.setRequestProperty("Authorization", token);
            if(method.equals("POST")) {
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", String.valueOf(body.length()));
            }
        }
    }

    private class MasterRequest extends Request {

        public MasterRequest(String method, String url, String body) {
            super(method, "http://" + master.getMasterIP() + url, body);
        }

        protected void setHeaders() {
            if(method.equals("POST")) {
                con.setRequestProperty("Content-Type", "text/plain");
                con.setRequestProperty("Content-Length", String.valueOf(body.length()));
            }
        }
    }

    private class MasterFinder implements Runnable {

        private String masterIP;

        @Override
        public void run() {
            masterIP = "";
            try {
                DatagramSocket soc = new DatagramSocket(2004);
                soc.setReuseAddress(true);
                soc.setBroadcast(true);
                InetAddress address = InetAddress.getByName("255.255.255.255");
                byte[] buf = ("ping").getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 2004);
                soc.send(packet);

                while(masterIP.equals("")) {
                    byte[] message = new byte[1000];
                    DatagramPacket recpacket = new DatagramPacket(message, message.length);
                    soc.setSoTimeout(10000);
                    soc.receive(recpacket);
                    String recmessage = new String(message, 0, recpacket.getLength());
                    Log.d("udp", recmessage);
                    if (recmessage.equals("acknowledged")) {
                        Log.d("udp", "success");
                        Log.d("udp", "ip: " + recpacket.getAddress().toString());
                        masterIP = recpacket.getAddress().toString().substring(1);
                    }
                }
            } catch(Exception e) {

            }
        }

        public String getMasterIP() {
            return masterIP;
        }
    }

}
