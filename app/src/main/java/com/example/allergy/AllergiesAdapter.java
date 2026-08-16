package com.example.allergy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;

/**
 * Adapter for displaying a list of allergies with toggles
 */
public class AllergiesAdapter extends RecyclerView.Adapter<AllergiesAdapter.AllergyViewHolder> {

    private final List<Allergy> allergyList;
    private final OnAllergyChangeListener listener;

    public interface OnAllergyChangeListener {
        void onAllergyChanged(Allergy allergy);
    }

    public AllergiesAdapter(List<Allergy> allergyList, OnAllergyChangeListener listener) {
        this.allergyList = allergyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AllergyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.allergy, parent, false);
        return new AllergyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllergyViewHolder holder, int position) {
        Allergy allergy = allergyList.get(position);
        holder.tvName.setText(allergy.getDisplayName());

        // Temporarily remove listener to prevent trigger during initialization
        holder.switchAllergy.setOnCheckedChangeListener(null);
        holder.switchAllergy.setChecked(allergy.isActive());

        // Re-attach listener to handle user changes
        holder.switchAllergy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allergy.setActive(isChecked);
            listener.onAllergyChanged(allergy);
        });
    }

    @Override
    public int getItemCount() { return allergyList.size(); }

    //ViewHolder class for individual allergy items
    static class AllergyViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final MaterialSwitch switchAllergy;

        public AllergyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAllergyName);
            switchAllergy = itemView.findViewById(R.id.switchAllergy);
        }
    }
}
