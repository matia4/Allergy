package com.example.allergy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for scanning product barcodes using the camera
 */
public class ScannerFragment extends Fragment implements AnalyzeBarcode.ScannerListener {

    private static final String TAG = "ScannerFragment";
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private AnalyzeBarcode analyzeBarcode;
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
        return inflater.inflate(R.layout.scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Check for camera permission before starting the camera
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Initializes CameraX and binds preview and analysis use cases to the lifecycle
     */
    private void startCamera() {
        Context context = getContext();
        if (context == null) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                if (!isAdded()) return;

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                // Configure Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Configure resolution strategy for better barcode detection
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setResolutionStrategy(new ResolutionStrategy(new Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                // Configure ImageAnalysis use case with AnalyzeBarcode
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analyzeBarcode = new AnalyzeBarcode(this);
                imageAnalysis.setAnalyzer(cameraExecutor, analyzeBarcode);
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Context currentContext = getContext();
                if (currentContext != null) {
                    Toast.makeText(currentContext, getString(R.string.error_camera, e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Callback from AnalyzeBarcode when a barcode is detected.
     * Checks local database first, then falls back to Open Food Facts API.
     */
    @Override
    public void onBarcodeScanned(String rawValue) {
        if (!isScanning || !isAdded()) return;

        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) return;

        isScanning = false;
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Context context = getContext();
            if (context == null) return;

            AppDatabase db = AppDatabase.getInstance(context);
            Product existingProduct = db.productDAO().getProductByBarcode(rawValue);

            long ttlDuration = 365L * 24 * 60 * 60 * 1000L; // 1 year
            long currentTime = System.currentTimeMillis();

            if (existingProduct != null && (currentTime - existingProduct.getLastUpdated()) < ttlDuration) {
                activity.runOnUiThread(() -> showProductAnalysisDialog(
                        existingProduct.getName(),
                        stringToList(existingProduct.getDetectedAllergens()),
                        existingProduct.getIngredients(),
                        existingProduct.getImageUrl(),
                        false,
                        existingProduct.getBarcode(),
                        jsonToList(existingProduct.getCategoriesTagsJson())
                ));
                return;
            }

            Client.getApiService().getProductByBarcode(rawValue).enqueue(new retrofit2.Callback<>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<OffResponse> call, @NonNull retrofit2.Response<OffResponse> response) {
                    if (!isAdded() || getContext() == null) return;

                    if (response.isSuccessful() && response.body() != null && response.body().getStatus() == 1) {
                        OffProduct product = response.body().getProduct();

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase dbInternal = AppDatabase.getInstance(requireContext());
                            List<Allergy> userActiveAllergies = dbInternal.allergyDAO().getActiveAllergies();
                            List<String> productAllergensTags = product.getAllergensTags();
                            List<String> productCategoriesTags = product.getCategoriesTags();

                            List<String> detectedAllergensNames = new ArrayList<>();
                            if (productAllergensTags != null) {
                                for (Allergy allergy : userActiveAllergies) {
                                    if (productAllergensTags.contains(allergy.getOffTag())) {
                                        detectedAllergensNames.add(allergy.getDisplayName());
                                    }
                                }
                            }

                            String detectedStr = String.join(", ", detectedAllergensNames);
                            String allergensJson = productAllergensTags != null ? String.join(", ", productAllergensTags) : "";
                            String categoriesJson = productCategoriesTags != null ? new Gson().toJson(productCategoriesTags) : "[]";

                            Product productToSave = new Product(
                                    rawValue,
                                    product.getProductName(),
                                    product.getIngredientsText(),
                                    allergensJson,
                                    categoriesJson,
                                    currentTime,
                                    !detectedAllergensNames.isEmpty(),
                                    false,
                                    detectedStr,
                                    product.getImageUrl()
                            );
                            dbInternal.productDAO().insertOrUpdate(productToSave);

                            activity.runOnUiThread(() -> showProductAnalysisDialog(
                                    product.getProductName(),
                                    detectedAllergensNames,
                                    product.getIngredientsText(),
                                    product.getImageUrl(),
                                    false,
                                    rawValue,
                                    productCategoriesTags
                            ));
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            if (existingProduct != null) {
                                showProductAnalysisDialog(
                                        existingProduct.getName(),
                                        stringToList(existingProduct.getDetectedAllergens()),
                                        existingProduct.getIngredients(),
                                        existingProduct.getImageUrl(),
                                        true,
                                        existingProduct.getBarcode(),
                                        jsonToList(existingProduct.getCategoriesTagsJson())
                                );
                            } else {
                                showErrorDialog(getString(R.string.product_not_found));
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<OffResponse> call, @NonNull Throwable t) {
                    if (!isAdded() || getContext() == null) return;

                    activity.runOnUiThread(() -> {
                        if (existingProduct != null) {
                            showProductAnalysisDialog(
                                    existingProduct.getName(),
                                    stringToList(existingProduct.getDetectedAllergens()),
                                    existingProduct.getIngredients(),
                                    existingProduct.getImageUrl(),
                                    true,
                                    existingProduct.getBarcode(),
                                    jsonToList(existingProduct.getCategoriesTagsJson())
                            );
                        } else {
                            showErrorDialog(getString(R.string.network_error));
                        }
                    });
                }
            });
        });
    }

    private List<String> stringToList(String input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(input.split(", ")));
    }

    private List<String> jsonToList(String jsonInput) {
        if (jsonInput == null || jsonInput.isEmpty()) return new ArrayList<>();
        try {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> list = new Gson().fromJson(jsonInput, listType);
            return java.util.Objects.requireNonNullElseGet(list, ArrayList::new);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing categories JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * Shows a detailed analysis dialog for a product, highlighting allergens.
     */
    private void showProductAnalysisDialog(String title, List<String> detectedAllergens, String ingredients, String imageUrl, boolean isOutdatedWarning, String barcode, List<String> categoriesTags) {
        if (!isAdded() || getContext() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        StringBuilder msg = new StringBuilder();

        // Inflate custom dialog layout to show product image
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_result, null);

        ImageView ivProduct = dialogView.findViewById(R.id.ivDialogProductImage);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivProduct);
        } else {
            ivProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        builder.setView(dialogView);

        if (isOutdatedWarning) {
            msg.append(getString(R.string.outdated_warning));
        }

        boolean isAllergic = !detectedAllergens.isEmpty();

        // Warning logic if user's allergens are detected
        if (isAllergic) {
            msg.append(getString(R.string.detected_allergens_warning, String.join("\n- ", detectedAllergens)));
            builder.setIcon(android.R.drawable.ic_dialog_alert);

            // Button to trigger alternative product search
            builder.setNeutralButton(getString(R.string.safe_alternative), (dialog, which) ->
                RecommendationHelper.showHierarchicalRecommendationsDialog(requireContext(), barcode, categoriesTags, () -> isScanning = true)
            );

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

    private void showErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.problem_product)
                .setMessage(message)
                .setPositiveButton(R.string.try_again, (dialog, which) -> isScanning = true)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (analyzeBarcode != null) {
            analyzeBarcode.close();
        }
        cameraExecutor.shutdown();

        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
            cameraProvider.unbindAll();
        } catch (Exception ignored) {}
    }
}
