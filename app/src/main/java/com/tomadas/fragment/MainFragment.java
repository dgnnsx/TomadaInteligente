package com.tomadas.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.tomadas.R;
import com.tomadas.activity.MainActivity;
import com.tomadas.activity.TomadaActivity;
import com.tomadas.adapter.TomadaAdapter;
import com.tomadas.domain.Tomada;
import com.tomadas.domain.TomadaService;
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
import java.util.List;
import java.util.Map;

/**
 * Created by Diego on 25/03/2016.
 */
public class MainFragment extends Fragment {
    private Dialog loadingDialog;
    private List<Tomada> tomadas;
    protected RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        try {
            taskTomadas();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view;
        view = inflater.inflate(R.layout.fragment_main, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        // Linear Layout Manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        return view;
    }

    public void taskTomadas() throws IOException{
        ImageView view = (ImageView) getView().findViewById(R.id.sem_tomadas);
        // Busca as tomadas
        try {
            if(Variaveis.USER_TOMADAS == null) {
                view.setVisibility(View.VISIBLE);
                tomadas = null;
            }
            else {
                if(view.getVisibility() == View.VISIBLE)
                    view.setVisibility(View.GONE);
                String s = Variaveis.USER_TOMADAS.toString();
                this.tomadas = TomadaService.getTomadas(s);
            }
        }
        catch(IOException ie) {
            ie.printStackTrace();
        }
        // Atualiza a lista
        mRecyclerView.setAdapter(new TomadaAdapter(getContext(), tomadas, onClickTomada(), onLongClickTomada()));
    }

    public void atualizarTomadas(){
        class LoginAsync extends AsyncTask<String, Void, String> {
            private Dialog loadingDialog;

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(getContext(), "Aguarde", "Atualizando lista de tomadas...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;

                SharedPreferences pref = getContext().getSharedPreferences("UserPrefs", 0); // 0 - for private mode
                String email, senha;
                email = pref.getString("email", "null");
                senha = pref.getString("senha", "null");

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
                        result = "Ocorreu algum erro ao atualizar a lista de tomadas!";
                    } else {
                        result = "true";
                        Variaveis.USER_EXISTE_TOMADAS = jsonObject.getBoolean("achoutomadas");
                        if(Variaveis.USER_EXISTE_TOMADAS)
                            Variaveis.USER_TOMADAS = jsonObject.getJSONArray("tomadas");
                        else
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
                if(result.equals("true")){
                    try {
                        taskTomadas();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getContext(), "Lista de tomadas atualizada!", Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private void enviaRemoverTomada(final int id) {
        class LoginAsync extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                loadingDialog = ProgressDialog.show(getContext(), "Aguarde", "Removendo Tomada...");
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String result = null;
                HashMap<String, String> values = new HashMap();
                values.put("idtomada", Integer.toString(id));
                try {
                    URL url = new URL(Variaveis.BASE_URL + "remove_tomada.php");
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
                    boolean deletou = jsonObject.getBoolean("deletou");
                    if (!deletou) {
                        result = "Erro ao remover tomada!";
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
                if (result.equals("true")) {
                    atualizarTomadas();
                }
                else
                    Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
            }
        }
        LoginAsync la = new LoginAsync();
        la.execute();
    }

    private TomadaAdapter.TomadaOnLongClickListener onLongClickTomada(){
        return new TomadaAdapter.TomadaOnLongClickListener(){
            @Override
            public void onLongClickTomada(View view, int idx){
                final Tomada t = tomadas.get(idx);
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setIcon(R.drawable.tomada_red);
                builder.setTitle("Remover Tomada");
                builder.setMessage("Tem certeza que deseja remover esta tomada?");
                builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        enviaRemoverTomada(t.id);
                        return;
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
    }

    private TomadaAdapter.TomadaOnClickListener onClickTomada(){
        return new TomadaAdapter.TomadaOnClickListener(){
            @Override
            public void onClickTomada(View view, int idx){
                Tomada t = tomadas.get(idx);
                Intent intent = new Intent(getContext(), TomadaActivity.class);
                Bundle args = new Bundle();
                args.putSerializable("tomada", t);
                startActivity(intent.putExtras(args));
            }
        };
    }
}

