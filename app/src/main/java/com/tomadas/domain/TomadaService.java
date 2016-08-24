package com.tomadas.domain;

import android.util.Log;

import com.tomadas.R;
import com.tomadas.domain.Tomada;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by PC on 11/06/2016.
 */
public class TomadaService {
    public static List<Tomada> getTomadas(String json) throws IOException {
        List<Tomada> tomadas = new ArrayList<Tomada>();
        try{
            JSONArray jsonTomadas = new JSONArray(json);
            // Insere as tomadas na lista
            for(int i = 0; i < jsonTomadas.length(); i++){
                JSONObject jsonTomada = jsonTomadas.getJSONObject(i);
                Tomada t = new Tomada();
                // Lê as informações de cada tomada
                t.icone = R.drawable.outros;
                t.id = jsonTomada.getInt("id");
                t.nome = jsonTomada.getString("nome");
                t.serie = jsonTomada.getString("serie");
                t.status = jsonTomada.getInt("status");
                tomadas.add(t);
            }
        }
        catch(JSONException e){
            throw new IOException(e.getMessage(), e);
        }
        return tomadas;
    }
}
