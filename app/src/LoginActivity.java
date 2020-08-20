package com.example.toyoapp;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import com.example.toyoapp.Models.RespuestaLogin;
import com.example.toyoapp.ui.main.LoginActivityViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends FragmentActivity implements AsyncLoginResponse{

    LoginFragment fragment;
    Intent i;
    LoginActivityViewModel loginActivityViewModel;
    public static final String MY_PREFS_NAME = "MisPrefs";
    private String [] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_PHONE_STATE", "android.permission.SYSTEM_ALERT_WINDOW","android.permission.CAMERA"};


    public static final String CHECKPOINT = "CHECKPOINT";

    boolean gate = true;
    boolean proceedMessage = true;

    @Override
    protected void onStart() {
        super.onStart();
        fragment = (LoginFragment) getSupportFragmentManager().findFragmentById(R.id.frame_one);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_one, new LoginFragment())
                    .commit();
        }
        i = new Intent(getBaseContext(), MainActivity.class);
        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }
        loginActivityViewModel = ViewModelProviders.of(this).get(LoginActivityViewModel.class);

    }

    @Override
    public void onBackPressed() {
        finishAffinity();

    }

    public void sendData(View view){

        if(loginActivityViewModel.getUser() == null | loginActivityViewModel.getPass() == null){}else {
            fragment.tryLogin(this, loginActivityViewModel.getUser(), loginActivityViewModel.getPass());
        }

    }

    @Override
    public void processFinish(ArrayList<String> incomingToken) {

        if(incomingToken.get(0) != "FAIL" && gate == true) {

            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("Id", incomingToken.get(0));
            editor.putString("Nombre", incomingToken.get(1));
            editor.putString("Email", incomingToken.get(2));
            editor.putString("TokenAccess", incomingToken.get(3));
            editor.commit();

            Log.d(CHECKPOINT,incomingToken.get(0));
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            gate = false;
            proceedMessage = false;
            finish();
        }else{
            if(proceedMessage) {
                Toast toast = Toast.makeText(this, "Usuario o Contraseña Incorrecta", Toast.LENGTH_SHORT);
                toast.setMargin(0, 50);
                toast.show();
            }
        }
    }


    public static class LoginFragment extends Fragment {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        EditText mail,pass;
        MaterialButton button;
        LoginActivityViewModel insideloginActivityViewModel;


        private final TextWatcher textWatcherMail = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                insideloginActivityViewModel.setUser( mail.getText().toString());
            }
        };
        private final TextWatcher textWatcherPass = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
//
                    insideloginActivityViewModel.setPass( pass.getText().toString());
            }
        };

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            insideloginActivityViewModel = ViewModelProviders.
                    of(getActivity()).get(LoginActivityViewModel.class);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);


        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_login, container, false);  //USING FRAGMENTS. viewmodel relation

               button = rootView.findViewById(R.id.loginButton);
               mail =  rootView.findViewById(R.id.mailField);
               pass =  rootView.findViewById(R.id.passwordField);

               mail.addTextChangedListener(textWatcherMail);
               pass.addTextChangedListener(textWatcherPass);


            return rootView;
        }

        public void tryLogin(AsyncLoginResponse delegate,String email, String password) {
            CheckLoginTask loginTask = new CheckLoginTask(delegate);
            loginTask.execute(email,password);
        }

        public class CheckLoginTask extends AsyncTask<String, Void, ArrayList<String>> {

            public AsyncLoginResponse delegate = null;
            RespuestaLogin respuestaLogin;
            ArrayList<String> respuestaSerial;
            public CheckLoginTask(AsyncLoginResponse delegate){
                respuestaLogin = new RespuestaLogin();
                this.delegate = delegate;
            }

            @Override
            protected ArrayList<String> doInBackground(String... params) {

                builder.connectTimeout(30, TimeUnit.SECONDS);
                builder.readTimeout(30, TimeUnit.SECONDS);
                builder.writeTimeout(30, TimeUnit.SECONDS);
                OkHttpClient okHttpClient = builder.build();
                respuestaSerial = new ArrayList<String>();
                try {
                    OkHttpClient client = new OkHttpClient();

                    MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

                    String email = "\"email\"\r\n\r\n"+params[0]+"\n";
                    String password = "\"password\"\r\n\r\n"+params[1]+"\n";
                    RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name="+email+"------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name="+password+"------WebKitFormBoundary7MA4YWxkTrZu0gW--");
                    Log.d("email",params[0]);
                    Log.d("password",params[1]);

                    Request request = new Request.Builder()
                            .url("API REST")
                            .post(body)
                            .addHeader("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader("User-Agent", "PostmanRuntime/7.16.3")
                            .addHeader("Accept", "*/*")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("Host", "DOMAIN)
                            .addHeader("Accept-Encoding", "gzip, deflate")
                            .addHeader("Content-Length", "396")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("cache-control", "no-cache")
                            .build();

                    Gson gson = new Gson();
                    Response response = okHttpClient.newCall(request).execute();
                    try {
                        respuestaLogin = gson.fromJson(response.body().string(), RespuestaLogin.class);
                        respuestaSerial.add( respuestaLogin.getUser().getId() );
                        respuestaSerial.add( respuestaLogin.getUser().getName() );
                        respuestaSerial.add( respuestaLogin.getUser().getEmail());
                        respuestaSerial.add( respuestaLogin.getAccessToken() );

                    } catch (JsonSyntaxException e) {
                        respuestaSerial.add("FAIL");
                        e.printStackTrace();
                    } catch (IOException e) {
                        respuestaSerial.add("FAIL");
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    ArrayList<String> IOExceptionUser = new ArrayList<String>();
                    IOExceptionUser.add("FAIL");
                    return IOExceptionUser;
                } finally {
                    if (okHttpClient != null) {
                        respuestaSerial.add("FAIL");
                    }
                }

                return respuestaSerial;
            }

            @Override
            protected void onPostExecute(ArrayList<String> accessToken) {
                super.onPostExecute(accessToken);
                delegate.processFinish(accessToken);
            }
        }

    }

}