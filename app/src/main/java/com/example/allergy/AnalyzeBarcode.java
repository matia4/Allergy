package com.example.allergy;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class AnalyzeBarcode implements ImageAnalysis.Analyzer {

    private BarcodeScanner scanner;
    private ScannerListener listener;

    // Interfejs do przekazywania wyniku z powrotem do Aktywności
    public interface ScannerListener {
        void onBarcodeScanned(String rawValue);
    }

    public AnalyzeBarcode(ScannerListener listener) {
        this.listener = listener;

        // Konfiguracja ML KIT: szukamy tylko formatów EAN (UE) i UPC (USA) dla żywności
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                ).build();

        scanner = BarcodeScanning.getClient(options);
    }

    @Override
    @ExperimentalGetImage // Adnotacja wymagana przez API CameraX
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // 1. Konwersja obrazu z CameraX na format ML Kit
        InputImage inputImage = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // 2. Skanowanie
        scanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.isEmpty()) {
                                listener.onBarcodeScanned(rawValue);
                                // Zamykamy skaner, żeby nie skanował wiele razy tego samego
                                scanner.close();
                                return; // Przerywamy przetwarzanie tej klatki
                            }
                        }
                    }
                })
                .addOnCompleteListener(task -> {
                    // 3. KLUCZOWE: Musimy zamknąć imageProxy, inaczej CameraX przestanie wysyłać klatki
                    imageProxy.close();
                });
    }
}