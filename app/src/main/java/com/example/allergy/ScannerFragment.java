package com.example.allergy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

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
    private static final int CAMERA_REQUEST_CODE = 101;

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
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
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
                Toast.makeText(requireContext(), "Błąd kamery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onBarcodeScanned(String rawValue) {
        if (!isScanning) return;
        isScanning = false;

        requireActivity().runOnUiThread(() -> {
            Client.getApiService().getProductByBarcode(rawValue).enqueue(new retrofit2.Callback<Response>() {
                @Override
                public void onResponse(retrofit2.Call<Response> call, retrofit2.Response<Response> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                        Request product = response.body().getProduct();

                        // 1. Pobierz aktywne alergie z bazy Room
                        AppDatabase db = AppDatabase.getInstance(requireContext());
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

                        // 3. Pokaż spersonalizowany wynik
                        showProductAnalysisDialog(product.getProductName(), detectedAllergensNames, product.getIngredientsText());
                    } else {
                        showErrorDialog("Produkt nie istnieje w bazie Open Food Facts.");
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Response> call, Throwable t) {
                    showErrorDialog("Błąd sieci: " + t.getLocalizedMessage());
                }
            });
        });
    }

    private void showProductAnalysisDialog(String title, List<String> detectedAllergens, String ingredients) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        if (!detectedAllergens.isEmpty()) {
            // Produkt NIEBEZPIECZNY
            String warningMsg = "UWAGA! Produkt zawiera Twoje alergeny:\n- "
                    + String.join("\n- ", detectedAllergens)
                    + "\n\nSKŁAD:\n" + ingredients;
            builder.setMessage(warningMsg);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        } else {
            // Produkt BEZPIECZNY
            builder.setMessage("Produkt wygląda na bezpieczny pod kątem Twoich zapisanych alergii!\n\nSKŁAD:\n" + ingredients);
            builder.setIcon(android.R.drawable.ic_dialog_info);
        }

        builder.setPositiveButton("OK", (dialog, which) -> isScanning = true);
        builder.setCancelable(false);
        builder.show();
    }

    // Pomocnicze okno dialogowe z sukcesem
    private void showProductDialog(String title, String allergens, String ingredients) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage("ALERGENY (Tagi z API):\n" + allergens + "\n\nSKŁAD:\n" + ingredients)
                .setPositiveButton("OK", (dialog, which) -> {
                    isScanning = true; // Odblokowujemy skaner po zamknięciu okna
                })
                .setCancelable(false)
                .show();
    }

    // Pomocnicze okno dialogowe z błędem
    private void showErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Problem z produktem")
                .setMessage(message)
                .setPositiveButton("Spróbuj ponownie", (dialog, which) -> {
                    isScanning = true; // Odblokowujemy skaner
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(requireContext(), "Kamera jest wymagana do skanowania!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}