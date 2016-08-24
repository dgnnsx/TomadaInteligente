package com.tomadas.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.tomadas.R;
import com.tomadas.domain.Variaveis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Diego on 25/03/2016.
 */
public class VerificaLoginActivity extends AppCompatActivity{
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verifica_login);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0); // 0 - for private mode
                if (pref.contains("login")) {
                    String email, senha;
                    email = pref.getString("email", "null");
                    senha = pref.getString("senha", "null");
                    buscaLogin(email, senha);
                } else {
                    abrirCriaCadastro();
                }
                finish();
            }
        }, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();

        if ((loadingDialog != null) && loadingDialog.isShowing())
            loadingDialog.dismiss();
        loadingDialog = null;
    }

    public void buscaLogin(final String email, final String senha) {

        class LoginAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(VerificaLoginActivity.this, "Aguarde", "Buscando suas informações...", true);
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                HashMap<String, String> values = new HashMap();
                values.put("email", email);
                values.put("senha", senha);

                try {
                    URL url = new URL(Variaveis.BASE_URL + "login.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //recebe os dados
                    OutputStream os = connection.getOutputStream();

                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(values));

                    writer.flush();
                    writer.close();
                    os.close();

                    InputStream is = connection.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    result = sb.toString();

                    JSONArray jsonArray = new JSONArray(result);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    boolean achou = jsonObject.getBoolean("achou");
                    if (!achou) {
                        result = "false";
                    }
                    else{
                        Variaveis.USER_NOME = jsonObject.getString("nome");
                        Variaveis.USER_ID = jsonObject.getInt("id");
                        Variaveis.USER_EXISTE_TOMADAS = jsonObject.getBoolean("achoutomadas");
                        Variaveis.USER_TOMADAS = jsonObject.getJSONArray("tomadas");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                catch (JSONException e) {
                }
                return result;
            }

            private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
                StringBuilder result = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (first)
                        first = false;
                    else
                        result.append("&");
                    result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
                return result.toString();
            }

            @Override
            protected void onPostExecute(String result) {
                if((loadingDialog != null) && loadingDialog.isShowing())
                    loadingDialog.dismiss();
                if(result.equals("false")) {
                    // ABRIR ACTIVITY DE CADASTRO - NAO EXISTE UMA CONTA CADASTRADA AINDA
                    abrirCriaCadastro();
                }
                else{ // ACHOU LOGIN
                    // ABRIR ACTIVITY PRINCIPAL - JA EXISTE UMA CONTA CADASTRADA
                    abrirMain();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void abrirMain(){
        Intent activity = new Intent(getApplicationContext(), MainActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // adiciona a flag para a intent
        startActivity(activity);
    }

    private void abrirCriaCadastro(){
        Intent activity = new Intent(getApplicationContext(), TelaInicialActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // adiciona a flag para a intent
        startActivity(activity);
    }
}
