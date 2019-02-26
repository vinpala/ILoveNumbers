// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.codelab.mlkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.codelab.mlkit.GraphicOverlay.Graphic;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource;
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource;
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class MainActivity extends AppCompatActivity  {
    private static final String TAG = "MainActivity";

/*    private ImageView mImageView;
    private Button mTextButton;
    private Button mFaceButton;
    private Button mCloudButton;*/
    private Button mRunCustomModelButton;
    private DoodleView doodleView;
    private Bitmap mSelectedImage;
//    private GraphicOverlay mGraphicOverlay;
    // Max width (portrait mode)
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;
    /**
     * Name of the model file hosted with Firebase.
     */
    private static final String HOSTED_MODEL_NAME = "mnist-model";
    private static final String LOCAL_MODEL_ASSET = "mnist_custom.tflite";
    /**
     * Name of the label file stored in Assets.
     */
    private static final String LABEL_PATH = "labels.txt";
    /**
     * Number of results to show in the UI.
     */
    private static final int RESULTS_TO_SHOW = 3;
    /**
     * Dimensions of inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 1;
    private static final int DIM_IMG_SIZE_X = 28;
    private static final int DIM_IMG_SIZE_Y = 28;
    /**
     * Labels corresponding to the output of the vision model.
     */
    private List<String> mLabelList;

    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float>
                                o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });
    /* Preallocated buffers for storing image data. */
    private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    /**
     * An instance of the driver class to run model inference with Firebase.
     */
    private FirebaseModelInterpreter mInterpreter;
    /**
     * Data configuration of input & output data of model.
     */
    private FirebaseModelInputOutputOptions mDataOptions;
    // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
    private static final int BYTE_SIZE_OF_FLOAT = 4;
    private static final int PIXEL_WIDTH = 28;
    private int rand1;
    private String selection;

    private TextToSpeech tts;
    private boolean isTTSinitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("I love Numbers");

        isTTSinitialized = false;

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                isTTSinitialized = true;
            }
        });

        doodleView = findViewById(R.id.doodleView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        doodleView.init(metrics);

        mRunCustomModelButton = findViewById(R.id.button_run_custom_model);
        Intent intent = getIntent();
        selection = intent.getStringExtra(MenuActivity.EXTRA_MESSAGE);
        initCustomModel();
        play();

        mRunCustomModelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runModelInference();
            }
        });
        initCustomModel();
    }

    private void play() {
        // Chooses a random numbers from 0-9
        Random randy = new Random();
        rand1 = randy.nextInt(10);
        populateImages(rand1, selection);
    }

    private void populateImages(int rand1, String selection) {
        String source;
        for(int i=0; i<9; i++){
            String id = "image" + (i+1);
            //Log.d(TAG, "ID String = " + id);
            int resID = getResources().getIdentifier(id, "id", getPackageName());
            //Log.d(TAG, "res ID  = " + resID);
            ImageView iv = (ImageView)findViewById(resID);
            iv.setImageDrawable(null);
        }
        if(rand1 == 0){
            if(selection.equals("car")){
                source = "@drawable/bird";
               /* if (isTTSinitialized) {
                    tts.speak("How many cars do you see? ",
                            TextToSpeech.QUEUE_ADD, null);
                }*/
            }else{
                /*if (isTTSinitialized) {
                    tts.speak("How many birds do you see? ",
                            TextToSpeech.QUEUE_ADD, null);
                }*/
                source = "@drawable/car";
            }
            int imageResource = getResources().getIdentifier(source, null, getPackageName());
            ImageView iv = (ImageView)findViewById(R.id.image1);
            Drawable res = getResources().getDrawable(imageResource);
            iv.setImageDrawable(res);
            return;
        }

        if(selection.equals("car")){
            source = "@drawable/car";
            /*if (isTTSinitialized) {
                tts.speak("How many cars do you see? ",
                        TextToSpeech.QUEUE_ADD, null);
            }*/
        }else{
            source = "@drawable/bird";
            /*if (isTTSinitialized) {
                tts.speak("How many birds do you see? ",
                        TextToSpeech.QUEUE_ADD, null);
            }*/
        }
        int imageResource = getResources().getIdentifier(source, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        Log.d(TAG, "random = " + rand1);
        for(int i=0; i<rand1; i++){
            String id = "image" + (i+1);
            Log.d(TAG, "ID String = " + id);
            int resID = getResources().getIdentifier(id, "id", getPackageName());
            Log.d(TAG, "res ID  = " + resID);
            ImageView iv = (ImageView)findViewById(resID);
            iv.setImageDrawable(res);
        }
    }

     private void initCustomModel() {
        Log.e(TAG, "InitModel.");
        mLabelList = loadLabelList(this);

        int[] inputDims = {DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE};
        int[] outputDims = {DIM_BATCH_SIZE, mLabelList.size()};
        try {
            mDataOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, inputDims)
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, outputDims)
                            .build();
            FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions
                    .Builder()
                    .requireWifi()
                    .build();
            FirebaseCloudModelSource cloudSource = new FirebaseCloudModelSource.Builder
                    (HOSTED_MODEL_NAME)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)  // You could also specify
                    // different conditions
                    // for updates
                    .build();
            FirebaseLocalModelSource localSource =
                    new FirebaseLocalModelSource.Builder("asset")
                            .setAssetFilePath(LOCAL_MODEL_ASSET).build();
            FirebaseModelManager manager = FirebaseModelManager.getInstance();
            manager.registerCloudModelSource(cloudSource);
            manager.registerLocalModelSource(localSource);
            FirebaseModelOptions modelOptions =
                    new FirebaseModelOptions.Builder()
                            .setCloudModelName(HOSTED_MODEL_NAME)
                            .setLocalModelName("asset")
                            .build();
            mInterpreter = FirebaseModelInterpreter.getInstance(modelOptions);
        } catch (FirebaseMLException e) {
            showToast("Error while setting up the model");
            e.printStackTrace();
        }
    }

    private void runModelInference() {
        mSelectedImage = doodleView.getBitmap();
        Log.d(TAG, "mSelectedImage = " + mSelectedImage.getByteCount());
        if (mInterpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return;
        }
        // Create input data.
        ByteBuffer imgData = convertBitmapToByteBuffer(mSelectedImage, mSelectedImage.getWidth(),
                mSelectedImage.getHeight());
        try{
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(imgData).build();
            Log.d(TAG, "Success ? " + mInterpreter.run(inputs,mDataOptions).isSuccessful());
            mInterpreter.run(inputs,mDataOptions)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseModelOutputs>() {
                        @Override
                        public void onSuccess(FirebaseModelOutputs result) {
                            float[][] output = result.getOutput(0);
                            float[] probabilities = output[0];
                            Log.d(TAG, "Output 0  " + probabilities[0]);
                            Log.d(TAG, "Output 1  " + probabilities[1]);
                            Log.d(TAG, "Output 2  " + probabilities[2]);
                            Log.d(TAG, "Output 3  " + probabilities[3]);
                            Log.d(TAG, "Output 4  " + probabilities[4]);
                            Log.d(TAG, "Output 5  " + probabilities[5]);
                            Log.d(TAG, "Output 6  " + probabilities[6]);
                            Log.d(TAG, "Output 7  " + probabilities[7]);
                            Log.d(TAG, "Output 8  " + probabilities[8]);
                            Log.d(TAG, "Output 9  " + probabilities[9]);
                            int maxAt = 0;

                            for (int i = 0; i < probabilities.length; i++) {
                                maxAt = probabilities[i] > probabilities[maxAt] ? i : maxAt;
                            }
                            if(maxAt == rand1){
                                if (isTTSinitialized) {
                                    tts.speak("You are right!",
                                            TextToSpeech.QUEUE_ADD, null);
                                }
                                doodleView.clear();
                                play();
                            }
                            else{
                                if (isTTSinitialized) {
                                    tts.speak("It's wrong!try again ",
                                            TextToSpeech.QUEUE_ADD, null);
                                }
                                doodleView.clear();
                            }
                            Log.d(TAG, "Detected " + maxAt);
                        }});
            mInterpreter.run(inputs,mDataOptions)
                .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                    showToast("Failed to run model inference");
                }
            });
            } catch (FirebaseMLException e) {
                e.printStackTrace();
                showToast("FirebaseMLException running model inference"); }
              catch (Exception e) {
                e.printStackTrace();
                showToast("Error running model inference"); }


        /*try {
            FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(imgData).build();

            // Here's where the magic happens!!
            mInterpreter
                    .run(inputs, mDataOptions)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                            showToast("Error running model inference");
                        }
                    })
                    .continueWith(
                            new Continuation<FirebaseModelOutputs, List<String>>() {

                                @Override

                                public List<String> then(Task<FirebaseModelOutputs> task) {
                                    List<String> topLabels = new ArrayList<>();;
                                    try {
                                        float[][] labelProbArray = task.getResult()
                                                .<float[][]>getOutput(0);
                                        Log.d(TAG, "Result: " + task.getResult().getOutput(0));
//                                        topLabels = getTopLabels(labelProbArray);

                                        return topLabels;
                                    } catch (Exception e) {
                                        Log.d(TAG, "Exception in getResult  " + e);
                                    }
                                    return topLabels;
                                }
                            });
        } catch (FirebaseMLException e) {
            e.printStackTrace();
            showToast("Error running model inference");
        }
        catch (Exception e) {
            e.printStackTrace();
            showToast("Error!!");
        }
*/
    }

     /**
     * Gets the top labels in the results.
     */
    /*private synchronized List<String> getTopLabels(float[][] labelProbArray) {
        Log.d(TAG, "getToblabels");
        for (int i = 0; i < mLabelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(mLabelList.get(i), (labelProbArray[0][i] &
                            0xff) / 255.0f));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        List<String> result = new ArrayList<>();
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            result.add(label.getKey() + ":" + label.getValue());
        }
        Log.d(TAG, "labels: " + result.toString());
        return result;
    }*/

    /**
     * Reads label list from Assets.
     */
    private List<String> loadLabelList(Activity activity) {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(activity.getAssets().open
                             (LABEL_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read label list.", e);
        }
        return labelList;
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private synchronized ByteBuffer convertBitmapToByteBuffer(
            Bitmap bitmap, int width, int height) {
        ByteBuffer imgData =
                ByteBuffer.allocateDirect(
                        BYTE_SIZE_OF_FLOAT *DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y,
                true);
        imgData.rewind();
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0,
                scaledBitmap.getWidth(), scaledBitmap.getHeight());
        // Convert the image to int points.
        int pixel = 0;
        /*for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.put((byte) ((val >> 16) & 0xFF));
                imgData.put((byte) ((val >> 8) & 0xFF));
                imgData.put((byte) (val & 0xFF));
            }
        }*/
        for (int i = 0; i < intValues.length; ++i) {
            // Set 0 for white and 255 for black pixels
            Log.d(TAG, "i = " + i);
            pixel = intValues[i];
            Log.d(TAG, "pixel = " + pixel);
            // The color of the input is black so the blue channel will be 0xFF.
            int channel = pixel & 0xff;
            Log.d(TAG, "channel = " + channel);
            imgData.putFloat(0xff - channel);
            Log.d(TAG, "after putFloat = " + imgData);
        }
        return imgData;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxWidth = doodleView.getWidth();
        }

        return mImageMaxWidth;
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private Integer getImageMaxHeight() {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for
            // a UI layout pass to get the right values. So delay it to first time image
            // rendering time.
            mImageMaxHeight =
                    doodleView.getHeight();
        }

        return mImageMaxHeight;
    }

    // Gets the targeted width / height.
    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }


    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream is;
        Bitmap bitmap = null;
        try {
            is = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }
}
