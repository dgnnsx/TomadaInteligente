package com.tomadas.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomadas.R;
import com.tomadas.domain.Tomada;

import java.util.List;

/**
 * Created by PC on 11/06/2016.
 */
public class TomadaAdapter extends RecyclerView.Adapter<TomadaAdapter.ViewHolder> {
    private final Context context;
    private final List<Tomada> tomadas;
    private TomadaOnClickListener tomadaOnClickListener;
    private TomadaOnLongClickListener tomadaOnLongClickListener;

    public TomadaAdapter(Context context, List<Tomada> tomadas, TomadaOnClickListener tomadaOnClickListener,
                         TomadaOnLongClickListener tomadaOnLongClickListener){
        this.context = context;
        this.tomadas = tomadas;
        this.tomadaOnClickListener = tomadaOnClickListener;
        this.tomadaOnLongClickListener = tomadaOnLongClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_tomada, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // Atualiza a view
        Tomada t = tomadas.get(position);
        holder.descLinha.setText(t.nome);
        holder.img.setImageResource(t.icone);

        // Click
        if(tomadaOnClickListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    tomadaOnClickListener.onClickTomada(holder.itemView, position);
                }
            });
        }

        // Long Click
        if(tomadaOnLongClickListener != null){
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View view){
                    tomadaOnLongClickListener.onLongClickTomada(holder.itemView, position);
                    return true; // TRATA O EVENTO (NAO CHAMA O CLICK)
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return this.tomadas != null ? this.tomadas.size() : 0;
    }

    public interface TomadaOnClickListener{
        public void onClickTomada(View view, int idx);
    }

    public interface TomadaOnLongClickListener{
        public void onLongClickTomada(View view, int idx);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView descLinha;
        ImageView img;
        CardView cardView;

        public ViewHolder(View view){
            super(view);
            img = (ImageView) view.findViewById(R.id.imgTomada);
            descLinha = (TextView) view.findViewById(R.id.textLinha);
            cardView = (CardView) view.findViewById(R.id.card_view);
        }
    }
}
