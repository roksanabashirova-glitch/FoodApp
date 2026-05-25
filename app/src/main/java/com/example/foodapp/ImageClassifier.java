package com.example.foodapp;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageClassifier {
    private Interpreter tflite;
    private List<String> labels = new ArrayList<>();
    private int imageSize = 224;

    public ImageClassifier(Context context, String modelPath, String labelPath) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context, modelPath);
        tflite = new Interpreter(modelBuffer);
        loadLabels(context, labelPath);

        int[] inputShape = tflite.getInputTensor(0).shape();
        this.imageSize = inputShape[1];
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadLabels(Context context, String labelPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line.trim());
        }
        reader.close();
    }

    public String classifyImage(Bitmap image) {
        Bitmap resizedImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);

        int outputSize = tflite.getOutputTensor(0).shape()[1];
        float[][] output = new float[1][outputSize];
        tflite.run(byteBuffer, output);

        float bestConfidence = 0;
        int bestIndex = 0;
        int limit = Math.min(labels.size(), outputSize);

        for (int i = 0; i < limit; i++) {
            if (output[0][i] > bestConfidence) {
                bestConfidence = output[0][i];
                bestIndex = i;
            }
        }

        String label = labels.get(bestIndex);
        float confidencePercent = bestConfidence * 100;

        return String.format("%s (%.1f%%)", label, confidencePercent);
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(imageSize * imageSize * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[imageSize * imageSize];
        bitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize);

        int pixel = 0;
        for (int i = 0; i < imageSize; i++) {
            for (int j = 0; j < imageSize; j++) {
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}