package com.example.inellipsetextrecognition;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextRecognitionActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    Uri image_uri;
    private ImageView imageView;
    private ImageButton captureButton, detectText, logOutButton;
    private TextView recognizedText;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        imageView = findViewById(R.id.image_view);
        captureButton = findViewById(R.id.take_photo_button);
        detectText = findViewById(R.id.detect_text_button);
        recognizedText = findViewById(R.id.recognized_text);
        logOutButton = findViewById(R.id.logout_button);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        captureButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, PERMISSION_CODE);
            } else {
                openCamera();
            }
        });
        detectText.setOnClickListener(v -> runTextRecognition());
        logOutButton.setOnClickListener(v -> logOut());
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            imageView.setImageURI(image_uri);
        }
    }

    private void runTextRecognition() {
        InputImage image = null;
        try {
            if (image_uri != null) {
                image = InputImage.fromFilePath(this, image_uri);
                TextRecognizer recognizer = TextRecognition.getClient();
                assert image != null;
                Task<Text> result = recognizer.process(image)
                        .addOnSuccessListener(text -> {
                            processTextRecognitionResult(text);
                        });
            } else
                Toast.makeText(this, "Please take a picture first", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processTextRecognitionResult(Text text) {
        String resultText = text.getText();
        if (resultText.isEmpty()) {
            Toast.makeText(this, "Text is not found", Toast.LENGTH_SHORT).show();
        }
        saveRecognizedText(resultText);
        recognizedText.setText(resultText);
    }

    private void saveRecognizedText(String text) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        assert firebaseUser != null;
        String userID = firebaseUser.getUid();
        try {
            Map<String, Object> savedText = new HashMap<>();
            savedText.put("text", text);

            db.collection("Users").document(userID)
                    .collection("Saved Texts").document(text)
                    .set(savedText, SetOptions.merge())
                    .addOnCompleteListener(task -> Log.d("Text", "Text Saved"));
        } catch (Exception e) {
            Toast.makeText(this, "Text is not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        Intent logoutIntent = new Intent(this, MainActivity.class);
        logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(logoutIntent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}