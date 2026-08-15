package com.example.allergy;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AllergyTileAdapter extends RecyclerView.Adapter<AllergyTileAdapter.ViewHolder> {

    public interface OnTileClickListener {
        void onTileClick(Allergy allergy, int position);
    }

    private List<Allergy> allergyList;
    private final OnTileClickListener listener;

    public AllergyTileAdapter(List<Allergy> allergyList, OnTileClickListener listener) {
        this.allergyList = allergyList;
        this.listener = listener;
    }

    // Metoda do aktualizacji listy po sortowaniu
    public void updateList(List<Allergy> newList) {
        this.allergyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Allergy allergy = allergyList.get(position);
        holder.tvAllergyName.setText(allergy.getDisplayName());

        // Zmiana koloru w zależności od stanu (zaznaczone / niezaznaczone)
        if (allergy.isActive()) {
            holder.cardAllergy.setCardBackgroundColor(Color.parseColor("#1E88E5")); // Niebieski aktywny
            holder.tvAllergyName.setTextColor(Color.WHITE);
        } else {
            holder.cardAllergy.setCardBackgroundColor(Color.WHITE); // Biały nieaktywny
            holder.tvAllergyName.setTextColor(Color.parseColor("#424242"));
        }

        holder.itemView.setOnClickListener(v -> listener.onTileClick(allergy, position));
    }

    @Override
    public int getItemCount() {
        return allergyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardAllergy;
        TextView tvAllergyName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAllergy = itemView.findViewById(R.id.cardAllergy);
            tvAllergyName = itemView.findViewById(R.id.tvAllergyName);
        }
    }
}