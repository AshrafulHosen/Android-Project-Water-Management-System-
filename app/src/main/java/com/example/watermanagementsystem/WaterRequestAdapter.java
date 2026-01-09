package com.example.watermanagementsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WaterRequestAdapter extends RecyclerView.Adapter<WaterRequestAdapter.ViewHolder> {

    private final List<WaterRequest> requestList;
    private final SimpleDateFormat dateFormat;

    public WaterRequestAdapter(List<WaterRequest> requestList) {
        this.requestList = requestList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterRequest request = requestList.get(position);

        holder.volumeText.setText(String.format(Locale.getDefault(), "%.1f Liters", request.getVolume()));
        holder.statusText.setText(request.getStatus());

        // Format timestamp
        if (request.getTimestamp() > 0) {
            String formattedDate = dateFormat.format(new Date(request.getTimestamp()));
            holder.dateText.setText(formattedDate);
        } else {
            holder.dateText.setText("N/A");
        }

        // Set status color
        switch (request.getStatus().toLowerCase()) {
            case "pending":
                holder.statusText.setTextColor(Color.parseColor("#FFC107")); // Yellow
                break;
            case "approved":
                holder.statusText.setTextColor(Color.parseColor("#4CAF50")); // Green
                break;
            case "rejected":
                holder.statusText.setTextColor(Color.parseColor("#F44336")); // Red
                break;
            default:
                holder.statusText.setTextColor(Color.parseColor("#9E9E9E")); // Grey
                break;
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView volumeText, statusText, dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            volumeText = itemView.findViewById(R.id.volumeText);
            statusText = itemView.findViewById(R.id.statusText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}

