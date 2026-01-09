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

public class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.ViewHolder> {

    private final List<WaterRequest> requestList;
    private final SimpleDateFormat dateFormat;
    private final OnItemSelectedListener selectionListener;
    private int selectedPosition = -1;

    public interface OnItemSelectedListener {
        void onItemSelected(WaterRequest request, int position);
    }

    public AdminRequestAdapter(List<WaterRequest> requestList, OnItemSelectedListener listener) {
        this.requestList = requestList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterRequest request = requestList.get(position);

        holder.userNameText.setText("User: " + request.getFullName());
        holder.volumeText.setText(String.format(Locale.getDefault(), "Volume: %.1f Liters", request.getVolume()));
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

        // Highlight selected item
        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1f3a5f")); // Selected color
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#3a3a3a")); // Default color
        }

        // Set click listener for selection
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Notify changes for previous and current selection
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            // Callback to activity
            if (selectionListener != null) {
                selectionListener.onItemSelected(request, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public void clearSelection() {
        int previousSelected = selectedPosition;
        selectedPosition = -1;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public WaterRequest getSelectedRequest() {
        if (selectedPosition >= 0 && selectedPosition < requestList.size()) {
            return requestList.get(selectedPosition);
        }
        return null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText, volumeText, statusText, dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            volumeText = itemView.findViewById(R.id.volumeText);
            statusText = itemView.findViewById(R.id.statusText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}

