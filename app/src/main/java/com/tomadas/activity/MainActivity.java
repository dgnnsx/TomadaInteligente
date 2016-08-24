package com.tomadas.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tomadas.fragment.MainFragment;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null){
            setSupportActionBar(toolbar);
        }
        toolbar.setTitle(getString(R.string.app_name));

        // Fragment
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        MainFragment frag = new MainFragment();
        ft.add(R.id.frag, frag);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_adicionar) {
                final Dialog dialog = new Dialog(this);
                LayoutInflater inflater;
                inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.alert_add_layout, null);

                //aqui o conteudo
                Button adicionar = (Button) layout.findViewById(R.id.botao_Add);
                final EditText nomeTomada = (EditText) layout.findViewById(R.id.nomeTomada);
                final EditText numeroSerie = (EditText) layout.findViewById(R.id.numero_serie);

                adicionar.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (nomeTomada.length() == 0)
                            Toast.makeText(getApplicationContext(), "Dê um nome para identificar sua tomada!", Toast.LENGTH_LONG).show();
                        else if (numeroSerie.length() == 0)
                            Toast.makeText(getApplicationContext(), "Insira o número de série da sua tomada!", Toast.LENGTH_LONG).show();
                        else
                            adicionar(Variaveis.USER_ID, nomeTomada.getText().toString(), numeroSerie.getText().toString());
                        dialog.dismiss();
                    }
                });
                dialog.setContentView(layout);
                dialog.show();
                Window window = dialog.getWindow();
                window.setLayout(AbsListView.LayoutParams.FILL_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
            return true;
        }
        else if(id == R.id.action_desconectar){
            desconectar();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void abrirCriaCadastro(){
        Intent activity = new Intent(getApplicationContext(), TelaInicialActivity.class);
        activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // adiciona a flag para a intent
        startActivity(activity);
    }

    public void adicionar(final int id, final String nome, final String serie){
        class LoginAsync extends AsyncTask<String, Void, String> {
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(MainActivity.this, "Aguarde", "Adicionando Tomada...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                HashMap<String, String> values = new HashMap();
                values.put("iduser", Integer.toString(id));
                values.put("nome", nome);
                values.put("serie", serie);

                try {
                    URL url = new URL(Variaveis.BASE_URL + "cadastro_tomada.php");
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
                    boolean cadastrou = jsonObject.getBoolean("cadastro");

                    if (!cadastrou) {
                        result = "Erro! Número de série já cadastrado!";
                    } else {
                        result = "true";
                        Variaveis.USER_EXISTE_TOMADAS = true;
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
                    MainFragment fragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.frag);
                    fragment.atualizarTomadas();
                    Toast.makeText(getApplicationContext(), "Tomada adicionada com sucesso!", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    public void desconectar(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.thomas);
        builder.setTitle("Sair");
        builder.setMessage("Tem certeza que quer se desconectar?");
        builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.clear();
                editor.commit();
                Variaveis.USER_NOME = null;
                Variaveis.USER_ID = 0;
                Variaveis.USER_EXISTE_TOMADAS = false;
                Variaveis.USER_TOMADAS = null;
                abrirCriaCadastro();
                finish();
                return;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
