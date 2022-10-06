package com.elluminati.eber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecentHistoryAdapter extends RecyclerView.Adapter<RecentHistoryAdapter.ViewHolder> {
private RecentHistoryEventClicked recentHistoryEventClicked;

    public RecentHistoryAdapter(RecentHistoryEventClicked recentHistoryEventClicked) {
        this.recentHistoryEventClicked = recentHistoryEventClicked;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recent_destination_trip,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recentHistoryEventClicked.RecentEventClicked(holder.getAdapterPosition());
                }
            });
    }

    @Override
    public int getItemCount() {
        return 10;
    }

    public interface RecentHistoryEventClicked{

         void RecentEventClicked(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
