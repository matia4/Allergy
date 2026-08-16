package com.example.allergy;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

/**
 * Analyzer for processing camera frames to detect barcodes using ML Kit
 */
public class AnalyzeBarcode implements ImageAnalysis.Analyzer {

    private final BarcodeScanner scanner;
    private final ScannerListener listener;

    public interface ScannerListener {
        void onBarcodeScanned(String rawValue);
    }

    public AnalyzeBarcode(ScannerListener listener) {
        this.listener = listener;

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                ).build();

        scanner = BarcodeScanning.getClient(options);
    }

    /**
     * Main analysis method called for each camera frame.
     * Extracts a bitmap from ImageProxy and passes it to ML Kit for scanning.
     */
    @Override
    @ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Prepare InputImage from MediaImage source
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // Run ML Kit scanner
        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            // Notify listener on first successful detection
                            listener.onBarcodeScanned(rawValue);
                            return;
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close()); // Ensure frame is closed to avoid memory leak
    }

    public void close() {
        scanner.close();
    }
}
