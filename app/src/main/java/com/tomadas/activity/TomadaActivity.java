package com.tomadas.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tomadas.R;
import com.tomadas.domain.Tomada;
import com.tomadas.domain.Variaveis;
import com.tomadas.fragment.MainFragment;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by PC on 12/06/2016.
 */
public class TomadaActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, DialogInterface.OnCancelListener {
    private Switch aSwitch;
    private TextView textData;
    private Button botaoData;
    private Button botaoEnviarProg;
    private Button botaoRemoverProg;
    private int year, month, day, hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tomada);

        Bundle bundle = getIntent().getExtras();
        final Tomada t = (Tomada) bundle.getSerializable("tomada");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null){
            setSupportActionBar(toolbar);
        }
        // Titulo da toolbar e botão up navigation
        getSupportActionBar().setTitle(t.nome);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textData = (TextView) findViewById(R.id.data_hora);
        botaoEnviarProg = (Button) findViewById(R.id.confirm_program);
        botaoRemoverProg = (Button) findViewById(R.id.remove_program);
        aSwitch = (Switch) findViewById(R.id.switchTomada);
        botaoData = (Button) findViewById(R.id.programarData);
        botaoData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schedule(v);
            }
        });

        botaoRemoverProg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removerProgramacao(t.id);
            }
        });

        botaoEnviarProg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (t.status == 1)
                    enviarProgramacao(t.id, 0);
                else
                    enviarProgramacao(t.id, 1);
            }
        });

        if(t.status == 1)
            aSwitch.setChecked(true);
        else
            aSwitch.setChecked(false);

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    t.status = 1;
                    atualizarStatus(t.id, 1);
                    Toast.makeText(getBaseContext(), "Tomada Ligada!", Toast.LENGTH_SHORT).show();
                } else {
                    t.status = 0;
                    atualizarStatus(t.id, 0);
                    Toast.makeText(getBaseContext(), "Tomada Desligada!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void removerProgramacao(final int id){
        class LoginAsync extends AsyncTask<String, Void, String> {
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TomadaActivity.this, "Aguarde", "Removendo programação da tomada...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                HashMap<String, String> values = new HashMap();
                values.put("idtomada", Integer.toString(id));

                try {
                    URL url = new URL(Variaveis.BASE_URL + "remove_programacao.php");
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

                    Log.e("TESTE DO RESULT", result);

                    JSONArray jsonArray = new JSONArray(result);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    boolean deletou = jsonObject.getBoolean("deletou");

                    if (!deletou) {
                        result = "Ocorreu algum erro ao remover a programação da tomada!";
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
                if(result.equals("true")){
                    textData.setText( "" );
                    botaoEnviarProg.setVisibility(View.GONE);
                    botaoRemoverProg.setVisibility(View.GONE);
                    Toast.makeText(getBaseContext(), "Programação removida com sucesso!", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void enviarProgramacao(final int id, final int status){
        class LoginAsync extends AsyncTask<String, Void, String> {
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TomadaActivity.this, "Aguarde", "Atualizando status da tomada...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                HashMap<String, String> values = new HashMap();
                values.put("idtomada", Integer.toString(id));
                values.put("dia", Integer.toString(day));
                values.put("mes", Integer.toString(month + 1));
                values.put("ano", Integer.toString(year));
                values.put("hora", Integer.toString(hour));
                values.put("minuto", Integer.toString(minute));
                values.put("status", Integer.toString(status));

                try {
                    URL url = new URL(Variaveis.BASE_URL + "cria_programacao.php");
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

                    Log.e("TESTE DO RESULT", result);

                    JSONArray jsonArray = new JSONArray(result);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    boolean programou = jsonObject.getBoolean("cadastro");

                    if (!programou) {
                        result = "Ocorreu algum erro ao programar a tomada!";
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
                if(result.equals("true")){
                    textData.setText( "Tomada programada para " +(day < 10 ? "0"+day : day)+"/"+
                            (month+1 < 10 ? "0"+(month+1) : month+1)+"/"+
                            year + " às " +
                            (hour < 10 ? "0"+hour : hour)+"h"+
                            (minute < 10 ? "0"+minute : minute) +"?");
                    botaoEnviarProg.setVisibility(View.GONE);
                    botaoRemoverProg.setVisibility(View.VISIBLE);
                    Toast.makeText(getBaseContext(), "Tomada programada para o horário inserido!", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void atualizarStatus(final int id, final int status){
        class LoginAsync extends AsyncTask<String, Void, String> {
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(TomadaActivity.this, "Aguarde", "Atualizando status da tomada...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                HashMap<String, String> values = new HashMap();
                values.put("idtomada", Integer.toString(id));
                values.put("novostatus", Integer.toString(status));

                try {
                    URL url = new URL(Variaveis.BASE_URL + "liga_desliga_tomada.php");
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

                    Log.e("TESTE DO RESULT", result);

                    JSONArray jsonArray = new JSONArray(result);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    boolean atualizou = jsonObject.getBoolean("atualizou");

                    if (!atualizou) {
                        result = "Ocorreu algum erro ao atualizar o status da tomada!";
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
                if(result.equals("true")){
                    Log.e("TOMADA", "ATUALIZADO");
                }
                else {
                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
                }
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void schedule(View view){
        initData();
        Calendar cDefault = Calendar.getInstance();
        cDefault.set(year, month, day);

        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                this,
                cDefault.get(Calendar.YEAR),
                cDefault.get(Calendar.MONTH),
                cDefault.get(Calendar.DAY_OF_MONTH)
        );

        Calendar cMin = Calendar.getInstance();
        Calendar cMax = Calendar.getInstance();
        cMax.set( cMax.get(Calendar.YEAR) , 11, 31);

        datePickerDialog.setMinDate(cMin);
        datePickerDialog.setMaxDate(cMax);

        datePickerDialog.setOnCancelListener(this);
        datePickerDialog.show(getFragmentManager(), "DatePickerDialog");
    }

    private void initData(){
        if(year == 0){
            Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        year = month = day = hour = minute = 0;
        botaoEnviarProg.setVisibility(View.GONE);
        textData.setText("");
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int i, int i1, int i2) {
        Calendar tDefault = Calendar.getInstance();
        tDefault.set(year, month, day, hour, minute);

        year = i;
        month = i1; // BASE 0
        day = i2;

        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                this,
                tDefault.get(Calendar.HOUR_OF_DAY),
                tDefault.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.setOnCancelListener(this);
        timePickerDialog.show(getFragmentManager(), "timePickerDialog");
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute2, int second) {
        hour = hourOfDay;
        minute = minute2;

        textData.setText( "Confirmar programação para " +(day < 10 ? "0"+day : day)+"/"+
                (month+1 < 10 ? "0"+(month+1) : month+1)+"/"+
                year + " às " +
                (hour < 10 ? "0"+hour : hour)+"h"+
                (minute < 10 ? "0"+minute : minute) +"?");
        botaoEnviarProg.setVisibility(View.VISIBLE);
        botaoRemoverProg.setVisibility(View.GONE);
    }
}