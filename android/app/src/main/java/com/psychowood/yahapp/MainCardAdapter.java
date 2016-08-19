package com.psychowood.yahapp;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainCardAdapter extends RecyclerView.Adapter<MainCardAdapter.MainCardViewHolder> {

    private static final String TAG = "MainCardAdapter";
    List<MainCard> mainCards;
    MainCardActionListener listener;

    MainCardAdapter(List<MainCard> mainCards,MainCardActionListener listener){
        this.mainCards = mainCards;
        this.listener = listener;
    }

    @Override
    public MainCardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_main, viewGroup, false);
        MainCardViewHolder pvh = new MainCardViewHolder(v,listener);
        return pvh;
    }

    @Override
    public void onBindViewHolder(MainCardViewHolder mainCardViewHolder, int i) {
        final MainCard mainCard = mainCards.get(i);
        mainCardViewHolder.title.setText(mainCard.title);
        mainCardViewHolder.action.setText(mainCard.action);
        if (mainCard.backgroundImage != null) {
            mainCardViewHolder.backgroundImage.setImageResource(mainCard.backgroundImage);
        }
        if (mainCard.backgroundColor != null) {
            mainCardViewHolder.cv.setBackgroundColor(mainCard.backgroundColor);
        }
    }

    @Override
    public int getItemCount() {
        return mainCards.size();
    }

    public static class MainCardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView cv;
        TextView title;
        TextView action;
        ImageView backgroundImage;
        MainCardActionListener listener;

        MainCardViewHolder(View itemView, MainCardActionListener listener) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.maincard_cv);
            title = (TextView)itemView.findViewById(R.id.maincard_title);
            action = (TextView)itemView.findViewById(R.id.maincard_action);
            backgroundImage = (ImageView)itemView.findViewById(R.id.maincard_image);
            cv.setOnClickListener(this);
            action.setOnClickListener(this);
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            if (this.listener != null) {
                int position = getLayoutPosition();
                Context context = view.getContext();
                if (view.equals(cv)) {
                    listener.doMainAction(view, position, context);
                } else if (view.equals(action)) {
                    listener.doSubAction(view, position, context);
                }
            } else {
                Log.e(TAG,"Missing action listener (???)");
                Toast.makeText(view.getContext(), "Missing action listener?", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public static interface MainCardActionListener {
        void doMainAction(View v, int position, Context context);
        void doSubAction(View v, int position, Context context);
    }
}