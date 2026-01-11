package com.example.watermanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<Bill> billList;
    private OnBillClickListener onBillClickListener;
    private int selectedPosition = -1;

    public interface OnBillClickListener {
        void onBillClick(Bill bill, int position);
    }

    public BillAdapter(List<Bill> billList, OnBillClickListener listener) {
        this.billList = billList;
        this.onBillClickListener = listener;
    }

    public BillAdapter(List<Bill> billList) {
        this.billList = billList;
        this.onBillClickListener = null;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = billList.get(position);

        holder.billIdText.setText("Bill #" + bill.getBillId().substring(0, Math.min(8, bill.getBillId().length())));
        holder.usernameText.setText(bill.getFullName());
        holder.amountText.setText(String.format(Locale.getDefault(), "$%.2f", bill.getAmount()));
        holder.volumeText.setText(String.format(Locale.getDefault(), "%.1f L", bill.getWaterVolume()));
        holder.periodText.setText(bill.getBillingPeriod());
        holder.statusText.setText(bill.getStatus());

        // Format due date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.dueDateText.setText("Due: " + sdf.format(new Date(bill.getDueDate())));

        // Set status color
        int statusColor;
        switch (bill.getStatus()) {
            case "Paid":
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark);
                break;
            case "Overdue":
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark);
                break;
            default: // Pending
                statusColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark);
                break;
        }
        holder.statusText.setTextColor(statusColor);

        // Handle selection
        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_light));
        } else {
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (onBillClickListener != null) {
                onBillClickListener.onBillClick(bill, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public void clearSelection() {
        int previousSelected = selectedPosition;
        selectedPosition = -1;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
    }

    public Bill getSelectedBill() {
        if (selectedPosition >= 0 && selectedPosition < billList.size()) {
            return billList.get(selectedPosition);
        }
        return null;
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView billIdText, usernameText, amountText, volumeText, periodText, statusText, dueDateText;

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            billIdText = itemView.findViewById(R.id.billIdText);
            usernameText = itemView.findViewById(R.id.usernameText);
            amountText = itemView.findViewById(R.id.amountText);
            volumeText = itemView.findViewById(R.id.volumeText);
            periodText = itemView.findViewById(R.id.periodText);
            statusText = itemView.findViewById(R.id.statusText);
            dueDateText = itemView.findViewById(R.id.dueDateText);
        }
    }
}

