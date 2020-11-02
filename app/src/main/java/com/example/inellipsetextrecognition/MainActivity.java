package com.example.inellipsetextrecognition;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    Bitmap bMap;
    private ImageView imageView;
    private ImageButton takePhoto, detectText;
    private TextView recognizedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        detectText = findViewById(R.id.detect_text_button);
        takePhoto = findViewById(R.id.take_photo_button);
        recognizedText = findViewById(R.id.recognized_text);

        detectText.setOnClickListener(v -> runTextRecognition(bMap));
        takePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
    }

    private void runTextRecognition(Bitmap image) {
        try {
            InputImage inputImage = InputImage.fromBitmap(image, 0);
            TextRecognizer recognizer = TextRecognition.getClient();
            Task<Text> result = recognizer.process(inputImage)
                    .addOnSuccessListener(text -> {
                        processTextRecognitionResult(text);
                    })
                    .addOnFailureListener(
                            e -> e.printStackTrace());
        } catch (Exception e) {
            Toast.makeText(this, "Please first take a picture", Toast.LENGTH_SHORT).show();
        }
    }

    private void processTextRecognitionResult(Text text) {
        String resultText = text.getText();
        recognizedText.setText(resultText);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            bMap = imageBitmap;
            imageView.setImageBitmap(imageBitmap);
        }
    }

}