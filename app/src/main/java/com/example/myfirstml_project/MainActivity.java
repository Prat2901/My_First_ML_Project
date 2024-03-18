package com.example.myfirstml_project;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myfirstml_project.ml.MobilenetV110224Quant;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {


    Button captureBtn, predictBtn ,uploadBtn;
    TextView textView;
    ImageView imageView;
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        String[] labels = new String[1001];
        int count=0;
        try {
            BufferedReader bufferReader= new BufferedReader(new InputStreamReader(getAssets().open("labels.txt")));
            String line = bufferReader.readLine();
            while(line!=null && count < labels.length){
                labels[count]=line;
                count++;
                line = bufferReader.readLine();
            }
            bufferReader.close(); // Close the reader after reading
        } catch (IOException e) {
            e.printStackTrace();
        }
        predictBtn = findViewById(R.id.predictBtn);
        captureBtn = findViewById(R.id.captureBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        textView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i,10);
            }
        });
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(i,12);
            }
        });
        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Predicting...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MobilenetV110224Quant model = MobilenetV110224Quant.newInstance(MainActivity.this);

                            // Creates inputs for reference.
                            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
                            bitmap = Bitmap.createScaledBitmap(bitmap,224,224,true);
                            inputFeature0.loadBuffer(TensorImage.fromBitmap(bitmap).getBuffer());

                            // Runs model inference and gets result.
                            MobilenetV110224Quant.Outputs outputs = model.process(inputFeature0);
                            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(labels[getMax(outputFeature0.getFloatArray())]+"");
                                    progressDialog.dismiss();
                                }
                            });

                            // Releases model resources if no longer used.
                            model.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Dismiss progressDialog in case of an exception
                            progressDialog.dismiss();
                        }
                    }
                }).start();
            }
        });

    }

    int getMax(float[] arr){
        int max=0;
        for(int i=0;i<arr.length;i++){
             if(arr[i]>arr[max]) max=i;
        }
        return max;
    }
    void getPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==11){
            if(grantResults.length>0){
                if(grantResults[0]!= PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode==10 && resultCode==RESULT_OK){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    bitmap  = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode==12){
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}