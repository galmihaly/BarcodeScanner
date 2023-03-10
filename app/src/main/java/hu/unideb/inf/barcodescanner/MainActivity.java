package hu.unideb.inf.barcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MLKit Barcode";
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private PreviewView previewView;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private TextView codeTextView;

    private LinearLayout llay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        codeTextView = findViewById(R.id.codeTextView);
        llay = findViewById(R.id.cl);

        final ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(R.drawable.ic_barcode);
        imageButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_background));

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(150, 150);
        lp1.setMargins(0,10, 0, 10);

        imageButton.setLayoutParams(lp1);

        // Add ImageButton to LinearLayout
        if (llay != null) {
            llay.addView(imageButton);
        }

        final ImageButton imageButton1 = new ImageButton(this);
        imageButton1.setImageResource(R.drawable.ic_keyboard);
        imageButton1.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_background));

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(150, 150);
        lp2.setMargins(10,10, 0, 10);

        imageButton1.setLayoutParams(lp2);

        // Add ImageButton to LinearLayout
        if (llay != null) {
            llay.addView(imageButton1);
        }

        final ImageButton imageButton2 = new ImageButton(this);
        imageButton2.setImageResource(R.drawable.ic_personalcard);
        imageButton2.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_background));

        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(150, 150);
        lp3.setMargins(10,10, 0, 10);

        imageButton2.setLayoutParams(lp3);

        // Add ImageButton to LinearLayout
        if (llay != null) {
            llay.addView(imageButton2);
        }

        final ImageButton imageButton3 = new ImageButton(this);
        imageButton3.setImageResource(R.drawable.ic_barcode);
        imageButton3.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.circle_background));

        LinearLayout.LayoutParams lp4 = new LinearLayout.LayoutParams(150, 150);
        lp4.setMargins(10,10, 0, 10);

        imageButton3.setLayoutParams(lp4);

        // Add ImageButton to LinearLayout
        if (llay != null) {
            llay.addView(imageButton3);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    public void startCamera() {
        if(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            getPermissions();
        }
    }

    private void getPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (requestCode == PERMISSION_CODE) { setupCamera(); }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        int lensFacing = CameraSelector.LENS_FACING_BACK;
        cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindAllCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Hiba", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) { return; }
        if (previewUseCase != null) { cameraProvider.unbind(previewUseCase); }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetRotation(getRotation());

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Hiba az el??n??zet csatlakoztat??sa k??zben!", e);
        }
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) { return; }
        if (analysisUseCase != null) { cameraProvider.unbind(analysisUseCase); }

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetRotation(getRotation());

        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Hiba az analizer csatlakoztat??sakor!", e);
        }
    }

    protected int getRotation() throws NullPointerException {
        return previewView.getDisplay().getRotation();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
        ).build();

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

        barcodeScanner.process(inputImage)
                .addOnSuccessListener((List<Barcode> barcodes)->{
                    if (barcodes.size() > 0) {
                        codeTextView.setText(barcodes.get(0).getDisplayValue());
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "A k??d feldolgoz??sa sikertelen!", e)
                )
                .addOnCompleteListener(task ->
                        image.close()
                );
    }
}