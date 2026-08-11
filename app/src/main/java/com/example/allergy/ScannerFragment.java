package com.example.allergy;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment implements AnalyzeBarcode.ScannerListener {

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isScanning = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(requireContext(), R.string.camera_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Podpinamy layout dedykowany dla fragmentu skanera
        return inflater.inflate(R.layout.scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Sprawdzamy uprawnienia wewnątrz fragmentu
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new AnalyzeBarcode(this));
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), getString(R.string.error_camera, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onBarcodeScanned(String rawValue) {
        if (!isScanning) return;
        isScanning = false;

        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            Product existingProduct = db.productDAO().getProductByBarcode(rawValue);

            // Wyznaczamy czas TTL: 1 minuta jeśli switch jest włączony, inaczej 1 rok
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean isTestMode = prefs.getBoolean("test_ttl_enabled", false);
            long ttlDuration = isTestMode ? (60 * 1000L) : (365L * 24 * 60 * 60 * 1000L);
            long currentTime = System.currentTimeMillis();

            // 1. Sprawdzamy czy mamy świeży produkt w pamięci bazy
            if (existingProduct != null && (currentTime - existingProduct.getLastUpdated()) < ttlDuration) {
                showProductAnalysisDialog(
                        existingProduct.getName(),
                        existingProduct.isAllergic() ? List.of(existingProduct.getDetectedAllergens()) : List.of(),
                        existingProduct.getIngredients(),
                        existingProduct.getImageUrl(),
                        false
                );
                return;
            }

            Client.getApiService().getProductByBarcode(rawValue).enqueue(new retrofit2.Callback<Response>() {
                @Override
                public void onResponse(retrofit2.Call<Response> call, retrofit2.Response<Response> response) {
                    if (!isAdded() || getContext() == null) return;

                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                        Request product = response.body().getProduct();

                        // 1. Pobierz aktywne alergie z bazy Room
                        AppDatabase db = AppDatabase.getInstance(getContext());
                        List<Allergy> userActiveAllergies = db.allergyDAO().getActiveAllergies();
                        List<String> productTags = product.getAllergensTags();

                        // 2. Sprawdź, czy produkt zawiera którykolwiek z alergenów użytkownika
                        List<String> detectedAllergensNames = new ArrayList<>();
                        if (productTags != null) {
                            for (Allergy allergy : userActiveAllergies) {
                                if (productTags.contains(allergy.getOffTag())) {
                                    detectedAllergensNames.add(allergy.getDisplayName());
                                }
                            }
                        }

                        boolean isAllergic = !detectedAllergensNames.isEmpty();
                        String detectedStr = String.join(", ", detectedAllergensNames);
                        String tagsJson = productTags != null ? productTags.toString() : "[]";

                        // Zapisujemy/aktualizujemy produkt w bazie Room
                        Product productToSave = new Product(
                                rawValue,
                                product.getProductName(),
                                product.getIngredientsText(),
                                tagsJson,
                                currentTime, // Aktualna data skanowania
                                isAllergic,
                                false, // Nowe skanowanie — brak nowej wiadomości retroaktywnej
                                detectedStr,
                                product.getImageUrl()
                        );
                        db.productDAO().insertOrUpdate(productToSave);
                        // 3. Pokaż spersonalizowany wynik
                        showProductAnalysisDialog(product.getProductName(), detectedAllergensNames, product.getIngredientsText(), product.getImageUrl(), false);
                    }
                    else{
                            // Jeśli produkt jest przestarzały, ale nie ma go w API / brak sieci, pokażemy stary produkt z ostrzeżeniem
                            if (existingProduct != null) {
                                showProductAnalysisDialog(existingProduct.getName(),
                                        existingProduct.isAllergic() ? List.of(existingProduct.getDetectedAllergens()) : List.of(),
                                        existingProduct.getIngredients(), existingProduct.getImageUrl(), true);
                            } else {
                                showErrorDialog(getString(R.string.product_not_found));
                            }
                        }
                }

                @Override
                public void onFailure(retrofit2.Call<Response> call, Throwable t) {
                    if (!isAdded() || getContext() == null) return;

                    if (existingProduct != null) {
                        // W przypadku braku internetu używamy danych z bazy z ostrzeżeniem o braku odświeżenia
                        showProductAnalysisDialog(existingProduct.getName(),
                                existingProduct.isAllergic() ? List.of(existingProduct.getDetectedAllergens()) : List.of(),
                                existingProduct.getIngredients(), existingProduct.getImageUrl(), true);
                    } else {
                        showErrorDialog(getString(R.string.network_error));
                    }
                }
            });
        });
    }

    private void showProductAnalysisDialog(String title, List<String> detectedAllergens, String ingredients, String imageUrl, boolean isOutdatedWarning) {
        if (!isAdded() || getContext() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        StringBuilder msg = new StringBuilder();

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_result, null);

        ImageView ivProduct = dialogView.findViewById(R.id.ivDialogProductImage);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivProduct);
        } else {
            ivProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (isOutdatedWarning) {
            msg.append(getString(R.string.outdated_warning));
        }

        if (!detectedAllergens.isEmpty()) {
            msg.append(getString(R.string.detected_allergens_warning, String.join("\n- ", detectedAllergens)));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        } else {
            msg.append(getString(R.string.product_safe));
            builder.setIcon(android.R.drawable.ic_dialog_info);
        }

        msg.append("\n\n").append(getString(R.string.ingredients_title)).append(ingredients);
        builder.setMessage(msg.toString());
        builder.setPositiveButton("OK", (dialog, which) -> isScanning = true);
        builder.setCancelable(false);
        builder.show();
    }

    // Pomocnicze okno dialogowe z błędem
    private void showErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.problem_product)
                .setMessage(message)
                .setPositiveButton(R.string.try_again, (dialog, which) -> {
                    isScanning = true; // Odblokowujemy skaner
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}