package com.example.eletrolock;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    //0 for Login and 1 for Register
    private int loginOrRegister;
    UserSessionManager session;

    String email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        session = new UserSessionManager(getApplicationContext());

        loginOrRegister = 0;

        getSupportActionBar().setTitle("Login");

        EditText emailEditText = findViewById(R.id.emailEditText);
        setTextChangeListener(emailEditText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_menu, menu);
        return true;
    }

    public void onRegisterClick(View view) {
        view.setVisibility(View.GONE);
        getSupportActionBar().setTitle("Register");

        TextView passwordText = findViewById(R.id.passwordText);
        passwordText.setVisibility(View.VISIBLE);

        EditText passwordEditText= findViewById(R.id.passwordEditText);
        passwordEditText.setVisibility(View.VISIBLE);

        loginOrRegister = 1;
    }

    private void setTextChangeListener(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(/*isValidEmail(charSequence.toString()) &&*/ loginOrRegister == 0) {
                    TextView passwordText = findViewById(R.id.passwordText);
                    passwordText.setVisibility(View.VISIBLE);

                    EditText passwordEditText = findViewById(R.id.passwordEditText);
                    passwordEditText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_login:
                EditText emailEditText = findViewById(R.id.emailEditText);
                email = emailEditText.getText().toString();

                EditText passwordEditText = findViewById(R.id.passwordEditText);
                password = passwordEditText.getText().toString();

                JSONObject body = new JSONObject();
                try {
                    body.put("email", email);
                    body.put("password", password);
                } catch (Exception e) {

                }

                //Do stuff on server to actually register;

                if(loginOrRegister == 0) {
                    String url = "https://bitlock-api.herokuapp.com/auth/token";

                    new AsyncHTTP().execute(url, "POST", body.toString());
                } else {
                    String url = "https://bitlock-api.herokuapp.com/auth/registration";

                    new AsyncHTTP().execute(url, "POST", body.toString());
                }
                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    public void handleResponse(JSONObject response) {
        try {
            if(response.getString("status").equals("success")) {
                session.login(email, password, response.getString("unique"));
                session.setToken(response.getString("token"));
                this.finish();
            }
        } catch(Exception e) {

        }
    }

    static public boolean isValidEmail(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.matches(regex);
    }

    public class AsyncHTTP extends AsyncTask<String, Void, JSONObject> {

        protected JSONObject doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestMethod(params[1]);
                con.setRequestProperty("Content-Type", "application/json");

                Log.d("httpShit", params[2]);

                int contentLength = params[2].length();
                con.setRequestProperty("Content-Length", String.valueOf(contentLength));

                OutputStream os = con.getOutputStream();
                os.write(params[2].getBytes("UTF-8"));
                os.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer buffer = new StringBuffer();

                while((inputLine = in.readLine()) != null) {
                    buffer.append(inputLine);
                }

                in.close();

                return new JSONObject(buffer.toString());

            } catch(Exception e) {
            }
            return new JSONObject();
        }

        protected void onPostExecute(JSONObject result) {
            handleResponse(result);
        }
    }

}
