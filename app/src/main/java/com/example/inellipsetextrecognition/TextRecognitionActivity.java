package com.example.inellipsetextrecognition;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Map;

public class TextRecognitionActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap bMap;
    private ImageView imageView;
    private ImageButton takePhoto, detectText, logOutButton;
    private TextView recognizedText;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imageView = findViewById(R.id.image_view);
        detectText = findViewById(R.id.detect_text_button);
        takePhoto = findViewById(R.id.take_photo_button);
        recognizedText = findViewById(R.id.recognized_text);
        logOutButton = findViewById(R.id.logout_button);

        detectText.setOnClickListener(v -> runTextRecognition(bMap));
        takePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        logOutButton.setOnClickListener(v -> logOut());
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

        Map<String, Object> savedText = new HashMap<>();
        savedText.put("text", text);

        db.collection("Users").document(userID)
                .collection("Saved Texts").document(text)
                .set(savedText, SetOptions.merge())
                .addOnCompleteListener(task -> Log.d("Text", "Text Saved"));
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
            imageView.setImageBitmap(imageBitmap);
            bMap = imageBitmap;
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