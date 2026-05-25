package com.example.foodapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_GALLERY = 102;

    private ImageView imagePreview;
    private TextView resultText;
    private Button btnCamera, btnGallery;
    private ImageClassifier classifier;
    private Bitmap currentImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePreview = findViewById(R.id.imagePreview);
        resultText = findViewById(R.id.resultText);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);

        // Загружаем модель
        try {
            classifier = new ImageClassifier(this, "food_model.tflite", "labels.txt");
            Toast.makeText(this, "Модель загружена!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            resultText.setText("Ошибка загрузки модели");
        }

        btnCamera.setOnClickListener(v -> checkCameraPermission());
        btnGallery.setOnClickListener(v -> openGallery());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Камера не доступна", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    currentImage = (Bitmap) extras.get("data");
                    if (currentImage != null) {
                        imagePreview.setImageBitmap(currentImage);
                        try {
                            recognizeFood(currentImage);
                        } catch (Exception e) {
                            resultText.setText("Ошибка: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } else if (requestCode == REQUEST_GALLERY) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        currentImage = MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), selectedImageUri);
                        if (currentImage != null) {
                            imagePreview.setImageBitmap(currentImage);
                            try {
                                recognizeFood(currentImage);
                            } catch (Exception e) {
                                resultText.setText("Ошибка анализа: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        resultText.setText("Ошибка загрузки: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void recognizeFood(Bitmap image) {
        if (classifier == null) {
            resultText.setText("Модель не загружена");
            return;
        }

        String recognizedFood = classifier.classifyImage(image);
        String caloriesInfo = getCaloriesInfo(recognizedFood);
        resultText.setText(recognizedFood + "\n\n" + caloriesInfo);
    }

    private String getCaloriesInfo(String foodLabel) {
        String food = foodLabel.toLowerCase();

        if (food.contains("pizza")) return "🍕 ~285 ккал/100г";
        if (food.contains("burger")) return "🍔 ~295 ккал/100г";
        if (food.contains("sushi")) return "🍣 ~130 ккал/100г";
        if (food.contains("apple")) return "🍎 ~52 ккал";
        if (food.contains("banana")) return "🍌 ~89 ккал";
        if (food.contains("cake")) return "🍰 ~350 ккал/100г";
        if (food.contains("salad")) return "🥗 ~50 ккал/100г";
        if (food.contains("coffee")) return "☕ ~2 ккал";

        return "❓ Калорийность не определена";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Разрешение на камеру необходимо", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) {
            classifier.close();
        }
    }
}