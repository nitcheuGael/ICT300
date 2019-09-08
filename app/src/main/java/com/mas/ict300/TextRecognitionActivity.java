package com.mas.ict300;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Arrays;
public class TextRecognitionActivity extends AppCompatActivity {

    EditText mResultEt;
    ImageView mPreviewIv;

    Uri image_uri;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String cameraPermission[];
    String storagePermission[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        mResultEt = findViewById(R.id.resultEt);
        mPreviewIv = findViewById(R.id.imageIv);

        //Camera permission

        cameraPermission = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //Storage permission

        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,};
    }

    //handle cactionbar click items


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.addImage:
                //cette fonction permet d'afficher le menu d'option pour selectionner soit la camera soit la photo dans gallerie
                showImageImportDialog();
                break;
            case R.id.settings:
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();

                break;

            default:
                break;
        }

        return true;

    }

    private void showImageImportDialog() {

        String[] items = {"Camera", "Gallery"};

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("Select Image");

        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //Camera clicked
                        if (!checkCameraPermission()){
                            //Camera permission not allowed
                            requestCameraPermission();
                        }else {
                            //Permission granted to  app to picture
                            pickCamera();
                        }

                        break;

                    case 1:
                        //Gallery clicked
                        if (!checkStoragePermission()){
                            //Storage permission not allowed
                            requestStoragePermission();
                        }else {
                            //Permission granted to  app to picture
                            pickGallery();
                        }

                        break;

                    default:
                        break;
                }
            }
        });
        dialog.create().show();
    }

    private void pickGallery() {
        //Intend to take image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //Set intent type image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        //Intend to take image it will also save image to get high quality
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic");//Title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image to text");//Description
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    //Handle permission result

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }else {
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }
                }

                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickGallery();
                    }else {
                        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                    }
                }

                break;

            default:
                break;
        }
    }

    //Handle image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //Gor image from gallery
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)//enable image guidelines
                        .start(this);// ;
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //got image from camera
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)//enable image guidelines
                        .start(this);// ;
            }
        }
        //Get cropped image

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                Uri resultUri = result.getUri();//get image

                //set image to image view

                mPreviewIv.setImageURI(resultUri);

                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                recognizeText(bitmap);
/*                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if (!recognizer.isOperational()){
                    Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show();
                }else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder stringBuilder = new StringBuilder();
                    //Get text from screenbuilder untill there is no text
                    for (int i = 0; i < items.size();i++){
                        TextBlock myItem = items.valueAt(i);
                        stringBuilder.append(myItem.getValue());
                        stringBuilder.append("\n");
                    }
                    //Get text to edit text
                    mResultEt.setText(stringBuilder.toString());
                }*/
            }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                //
                Exception error = result.getError();
                Toast.makeText(this,""+error,Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recognizeText(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
//        FirebaseApp.initializeApp(this);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {

                                String resultText = firebaseVisionText.getText();
                                mResultEt.setText(resultText);
//                        for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
//                            String blockText = block.getText();
//                            Float blockConfidence = block.getConfidence();
//                            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//                            Point[] blockCornerPoints = block.getCornerPoints();
//                            Rect blockFrame = block.getBoundingBox();
//                            for (FirebaseVisionText.Line line: block.getLines()) {
//                                String lineText = line.getText();
//                                Float lineConfidence = line.getConfidence();
//                                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//                                Point[] lineCornerPoints = line.getCornerPoints();
//                                Rect lineFrame = line.getBoundingBox();
//                                for (FirebaseVisionText.Element element: line.getElements()) {
//                                    String elementText = element.getText();
//                                    Float elementConfidence = element.getConfidence();
//                                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//                                    Point[] elementCornerPoints = element.getCornerPoints();
//                                    Rect elementFrame = element.getBoundingBox();
//                                }
//                            }
//                        }

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(TextRecognitionActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        });
    }

    private void recognizeText2(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("eng"))
                .build();
        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);
        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        mResultEt.setText(firebaseVisionText.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("EDMT_ERROR",e.getMessage());


            }
        });
    }
}
