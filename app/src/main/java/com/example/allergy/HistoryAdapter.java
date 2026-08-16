package com.example.allergy;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying product scan history
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private static final String TAG = "HistoryAdapter";
    private final List<Product> productList;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public HistoryAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    public Product getProductAt(int position) {
        return productList.get(position);
    }

    // Removes product from adapter list
    public void removeProductAt(int position) {
        productList.remove(position);
        notifyItemRemoved(position);
    }

    // Restores product at given position (for "Undo")
    public void addProductAt(int position, Product product) {
        productList.add(position, product);
        notifyItemInserted(position);
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

        // Load product image using Glide with a default placeholder
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Parse category tags from cached JSON string
        List<String> categoriesTagsList = new ArrayList<>();
        if (product.getCategoriesTagsJson() != null && !product.getCategoriesTagsJson().isEmpty()) {
            try {
                Type listType = new TypeToken<List<String>>(){}.getType();
                categoriesTagsList = new Gson().fromJson(product.getCategoriesTagsJson(), listType);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing categories JSON", e);
            }
        }

        final List<String> finalCategories = categoriesTagsList;

        // Button listener for showing product recommendations
        holder.itemView.findViewById(R.id.btnFindAlternativesHistory).setOnClickListener(v -> 
            RecommendationHelper.showHierarchicalRecommendationsDialog(
                    holder.itemView.getContext(),
                    product.getBarcode(),
                    finalCategories,
                    null
            )
        );

        long ttlDuration = 365L * 24 * 60 * 60 * 1000L; // Data validity period (1 year)

        // Format scan date for display
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(product.getLastUpdated()));

        boolean isOutdated = (System.currentTimeMillis() - product.getLastUpdated()) > ttlDuration;

        // Display scan date and validity warning if necessary
        if (isOutdated) {
            String label = holder.itemView.getContext().getString(R.string.outdated_label_year);
            holder.tvScanDate.setText(holder.itemView.getContext().getString(R.string.scanned_at, dateStr + "\n" + label));
        } else {
            holder.tvScanDate.setText(holder.itemView.getContext().getString(R.string.scanned_at, dateStr));
        }

        // Show retroactive alert badge if enabled
        if (product.isNewAlert()) {
            holder.tvNewAlertBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvNewAlertBadge.setVisibility(View.GONE);
        }

        // Configure status badge colors and text based on safety check
        if (product.isAllergic()) {
            holder.tvStatusBadge.setText(holder.itemView.getContext().getString(R.string.status_contains, product.getDetectedAllergens()));
            holder.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_alert_bg));
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_alert_text));
        } else {
            holder.tvStatusBadge.setText(R.string.status_safe);
            holder.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_safe_bg));
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_safe_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
    }

    @Override
    public int getItemCount() { return productList.size(); }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivProductImage;
        final TextView tvProductName;
        final TextView tvScanDate;
        final TextView tvStatusBadge;
        final TextView tvNewAlertBadge;

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
