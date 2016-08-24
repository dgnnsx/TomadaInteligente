package com.tomadas.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
public class TelaInicialActivity extends AppCompatActivity{
    private Dialog loadingDialog;
    RadioGroup groupLogin;
    TextView viewNomeCompleto;
    TextView viewEsqueciSenha;
    EditText editNomeCompleto;
    Button botaoCadastrar;
    Button botaoEntrar;
    EditText editEmail;
    EditText editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_inicial);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0);
        editEmail = (EditText) findViewById(R.id.editEmail);
        editSenha = (EditText) findViewById(R.id.editSenha);
        viewNomeCompleto = (TextView) findViewById(R.id.textNomeCompleto);
        editNomeCompleto = (EditText) findViewById(R.id.editNomeCompleto);
        botaoCadastrar = (Button) findViewById(R.id.buttonCadastrar);
        botaoEntrar = (Button) findViewById(R.id.buttonEntrar);
        viewEsqueciSenha = (TextView) findViewById(R.id.textEsqueciSenha);

        viewEsqueciSenha.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                abrirEsqueciSenha();
            }
        });

        groupLogin = (RadioGroup) findViewById(R.id.groupLogin);
        groupLogin.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) findViewById(checkedId);
                if (rb.getText().toString().compareTo("Quero me cadastrar") == 0) {
                    viewNomeCompleto.setVisibility(View.VISIBLE);
                    editNomeCompleto.setVisibility(View.VISIBLE);
                    botaoEntrar.setVisibility(View.GONE);
                    botaoCadastrar.setVisibility(View.VISIBLE);
                    viewEsqueciSenha.setVisibility(View.GONE);
                }
                if (rb.getText().toString().compareTo("Já possuo cadastro") == 0) {
                    viewNomeCompleto.setVisibility(View.GONE);
                    editNomeCompleto.setVisibility(View.GONE);
                    viewEsqueciSenha.setVisibility(View.VISIBLE);
                    botaoEntrar.setVisibility(View.VISIBLE);
                    botaoCadastrar.setVisibility(View.GONE);
                }
            }
        });

        botaoEntrar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String email = editEmail.getText().toString();
                String senha = editSenha.getText().toString();
                if ((email.length() == 0) || (senha.length() == 0)) {
                    Toast.makeText(getApplicationContext(), "Todos os campos devem ser preenchidos!", Toast.LENGTH_LONG).show();
                } else {
                    buscaLogin(email, senha);
                }
            }
        });

        botaoCadastrar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String email = editEmail.getText().toString();
                String pass = editSenha.getText().toString();
                String nome = editNomeCompleto.getText().toString();
                if ((email.length() == 0) || (pass.length() == 0) || (nome.length() == 0)) {
                    Toast.makeText(getApplicationContext(), "Todos os campos devem ser preenchidos!", Toast.LENGTH_LONG).show();
                } else {
                    criaCadastro(nome, email, pass);
                }
            }
        });
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
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TelaInicialActivity.this, "Aguarde", "Conectando...");
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

                    if (achou == false) {
                        result = "O email não está cadastrado ou a senha está incorreta!";
                    } else {
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0); // 0 - for private mode
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("login", true);
                        editor.putString("nome", jsonObject.getString("nome"));
                        editor.putString("email", email);
                        editor.putString("senha", senha);
                        editor.commit();
                        result = "true";

                        Variaveis.USER_NOME = jsonObject.getString("nome");
                        Variaveis.USER_ID = jsonObject.getInt("id");
                        Variaveis.USER_EXISTE_TOMADAS = jsonObject.getBoolean("achoutomadas");
                        if(jsonObject.getBoolean("achoutomadas"))
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
                if (result.equals("true")){
                    //ABRIR ACTIVITY PRINCIPAL
                    abrirMain();
                    finish();
                }
                else{
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    public void criaCadastro(final String nome, final String email, final String senha) {
        class LoginAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TelaInicialActivity.this, "Aguarde", "Conectando...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                HashMap<String, String> values = new HashMap();
                values.put("nome", nome);
                values.put("email", email);
                values.put("senha", senha);

                String result = null;
                try {
                    URL url = new URL(Variaveis.BASE_URL + "cadastro.php");
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
                    boolean achou = jsonObject.getBoolean("cadastro");

                    if (achou == false) {
                        result = "Erro! Email já cadastrado!";
                    } else {
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0); // 0 - for private mode
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("login", true);
                        editor.putString("nome", nome);
                        editor.putString("email", email);
                        editor.putString("senha", senha);
                        editor.commit();
                        result = "true";

                        Variaveis.USER_NOME = nome;
                        Variaveis.USER_ID = jsonObject.getInt("id");
                        Variaveis.USER_EXISTE_TOMADAS = false;
                        Variaveis.USER_TOMADAS = null;
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
                if (result.equals("true")){
                    //ABRIR ACTIVITY PRINCIPAL
                    abrirMain();
                    finish();
                }
                else{
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
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

    public void enviaEsqueciSenha(final String email) {
        class LoginAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TelaInicialActivity.this, "Aguarde", "Conectando...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;
                HashMap<String, String> values = new HashMap();
                values.put("email", email);
                try {

                    URL url = new URL(Variaveis.BASE_URL + "recupera_senha.php");

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
                        result = "Erro! O email não está cadastrado!";
                    } else {
                        result = "true";
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
                if (result.equals("true")){
                    Toast.makeText(getApplicationContext(), "Foram enviadas instruções de recuperação de conta para seu email!", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void abrirEsqueciSenha(){
        final Dialog dialog = new Dialog(this);
        LayoutInflater inflater;
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.alert_esqueci_senha,
                null);

        //aqui o conteudo
        Button enviar = (Button) layout.findViewById(R.id.buttonEnviarEsqueciSenha);
        final EditText email = (EditText) layout.findViewById(R.id.editTextEmail);

        enviar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (email.length() == 0)
                    Toast.makeText(getApplicationContext(), "Insira seu email!", Toast.LENGTH_LONG).show();
                else {
                    dialog.dismiss();
                    enviaEsqueciSenha(email.getText().toString());
                }
            }
        }

        );
        dialog.setContentView(layout);
        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
    }
}
