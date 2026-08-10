package com.example.allergy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void OnProductClick(Product product);
    }

    public HistoryAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean isTestMode = prefs.getBoolean("test_ttl_enabled", false);
        long ttlDuration = isTestMode ? (60 * 1000L) : (365L * 24 * 60 * 60 * 1000L);

        // Formatowanie daty
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(product.getLastUpdated()));

        boolean isOutdated = (System.currentTimeMillis() - product.getLastUpdated()) > ttlDuration;

        if (isOutdated) {
            String label = isTestMode ? "🕒 Przestarzałe (>1 min)" : "🕒 Przestarzałe (>1 rok)";
            holder.tvScanDate.setText("Skanowano: " + dateStr + "\n" + label);
        } else {
            holder.tvScanDate.setText("Skanowano: " + dateStr);
        }

        if (product.isNewAlert()) {
            holder.tvNewAlertBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvNewAlertBadge.setVisibility(View.GONE);
        }

        // Status bezpieczeństwa
        if (product.isAllergic()) {
            holder.tvStatusBadge.setText("ZAWIERA: " + product.getDetectedAllergens());
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FFCDD2")); // Jasnoczerwony
            holder.tvStatusBadge.setTextColor(Color.parseColor("#B71C1C"));
        } else {
            holder.tvStatusBadge.setText("BEZPIECZNY");
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#C8E6C9")); // Jasnozielony
            holder.tvStatusBadge.setTextColor(Color.parseColor("#1B5E20"));
        }

        holder.itemView.setOnClickListener(v -> listener.OnProductClick(product));
    }

    @Override
    public int getItemCount() { return productList.size(); }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvScanDate, tvStatusBadge, tvNewAlertBadge;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvScanDate = itemView.findViewById(R.id.tvScanDate);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvNewAlertBadge = itemView.findViewById(R.id.tvNewAlertBadge);
        }
    }
}