package com.example.imagemodification;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_1 = 1;
    private static final int PICK_IMAGE_2 = 2;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private ImageView imageView;
    private Bitmap originalBitmap;
    private Bitmap workingBitmap;
    private Bitmap secondBitmap;
    private LinearLayout controlPanel;
    private TextView statusText;

    // Crop variables
    private boolean cropMode = false;
    private float cropStartX, cropStartY, cropEndX, cropEndY;
    private boolean isCropping = false;

    // Filter types
    private enum FilterType {
        NONE, GRAYSCALE, SEPIA, BRIGHTNESS, CONTRAST, NEGATIVE, BLUR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
        checkPermissions();
    }

    private void setupUI() {
        // Main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1a1a1a"));
        mainLayout.setPadding(20, 20, 20, 20);

        // Title
        TextView title = new TextView(this);
        title.setText("Image Manipulator Pro");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 20, 0, 20);
        title.setGravity(android.view.Gravity.CENTER);
        mainLayout.addView(title);

        // Status text
        statusText = new TextView(this);
        statusText.setText("Load an image to begin");
        statusText.setTextColor(Color.parseColor("#00ff88"));
        statusText.setTextSize(14);
        statusText.setPadding(0, 10, 0, 10);
        statusText.setGravity(android.view.Gravity.CENTER);
        mainLayout.addView(statusText);

        // Image view container
        LinearLayout imageContainer = new LinearLayout(this);
        imageContainer.setOrientation(LinearLayout.VERTICAL);
        imageContainer.setBackgroundColor(Color.parseColor("#2a2a2a"));
        imageContainer.setPadding(10, 10, 10, 10);
        LinearLayout.LayoutParams imageContainerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 3);
        imageContainerParams.setMargins(0, 10, 0, 10);
        imageContainer.setLayoutParams(imageContainerParams);

        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setOnTouchListener(cropTouchListener);
        imageContainer.addView(imageView);
        mainLayout.addView(imageContainer);

        // Control panel in ScrollView
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 2);
        scrollView.setLayoutParams(scrollParams);

        controlPanel = new LinearLayout(this);
        controlPanel.setOrientation(LinearLayout.VERTICAL);
        controlPanel.setPadding(10, 10, 10, 10);

        // Load Image Buttons
        addButton("📁 Load Image 1", v -> loadImage(PICK_IMAGE_1));
        addButton("📁 Load Image 2 (for Merge)", v -> loadImage(PICK_IMAGE_2));

        addSeparator();

        // Feature buttons
        addButton("✂️ Crop Image", v -> enableCropMode());
        addButton("🔄 Resize (50%)", v -> resizeImage(0.5f));
        addButton("🔄 Resize (150%)", v -> resizeImage(1.5f));

        addSeparator();

        // Filter buttons
        addButton("⚫ Grayscale Filter", v -> applyFilter(FilterType.GRAYSCALE));
        addButton("🟤 Sepia Filter", v -> applyFilter(FilterType.SEPIA));
        addButton("🔆 Increase Brightness", v -> adjustBrightness(30));
        addButton("🔅 Decrease Brightness", v -> adjustBrightness(-30));
        addButton("🎨 Negative Filter", v -> applyFilter(FilterType.NEGATIVE));
        addButton("💫 Blur Effect", v -> applyBlur());

        addSeparator();

        // Merge button
        addButton("🔗 Merge Images", v -> mergeImages());

        addSeparator();

        // Utility buttons
        addButton("↩️ Reset to Original", v -> resetImage());
        addButton("💾 Save Image", v -> saveImage());

        scrollView.addView(controlPanel);
        mainLayout.addView(scrollView);

        setContentView(mainLayout);
    }

    private void addButton(String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#3a3a3a"));
        btn.setPadding(30, 25, 30, 25);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);
        controlPanel.addView(btn);
    }

    private void addSeparator() {
        View separator = new View(this);
        separator.setBackgroundColor(Color.parseColor("#4a4a4a"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 15, 0, 15);
        separator.setLayoutParams(params);
        controlPanel.addView(separator);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private void loadImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                if (requestCode == PICK_IMAGE_1) {
                    originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    imageView.setImageBitmap(workingBitmap);
                    statusText.setText("Image 1 loaded successfully");
                } else if (requestCode == PICK_IMAGE_2) {
                    secondBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    statusText.setText("Image 2 loaded for merging");
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // FEATURE 1: CROPPING
    private void enableCropMode() {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        cropMode = !cropMode;
        if (cropMode) {
            statusText.setText("Crop Mode: Touch and drag to select area");
            Toast.makeText(this, "Drag to select crop area", Toast.LENGTH_LONG).show();
        } else {
            statusText.setText("Crop mode disabled");
        }
    }

    private View.OnTouchListener cropTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!cropMode || workingBitmap == null) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cropStartX = event.getX();
                    cropStartY = event.getY();
                    isCropping = true;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isCropping) {
                        cropEndX = event.getX();
                        cropEndY = event.getY();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (isCropping) {
                        cropEndX = event.getX();
                        cropEndY = event.getY();
                        performCrop();
                        isCropping = false;
                        cropMode = false;
                        statusText.setText("Image cropped successfully");
                    }
                    break;
            }
            return true;
        }
    };

    private void performCrop() {
        if (workingBitmap == null) return;

        float scaleX = (float) workingBitmap.getWidth() / imageView.getWidth();
        float scaleY = (float) workingBitmap.getHeight() / imageView.getHeight();

        int x = (int) Math.min(cropStartX, cropEndX) * (int) scaleX;
        int y = (int) Math.min(cropStartY, cropEndY) * (int) scaleY;
        int width = (int) Math.abs(cropEndX - cropStartX) * (int) scaleX;
        int height = (int) Math.abs(cropEndY - cropStartY) * (int) scaleY;

        x = Math.max(0, Math.min(x, workingBitmap.getWidth() - 1));
        y = Math.max(0, Math.min(y, workingBitmap.getHeight() - 1));
        width = Math.min(width, workingBitmap.getWidth() - x);
        height = Math.min(height, workingBitmap.getHeight() - y);

        if (width > 0 && height > 0) {
            Bitmap croppedBitmap = Bitmap.createBitmap(workingBitmap, x, y, width, height);
            workingBitmap = croppedBitmap;
            imageView.setImageBitmap(workingBitmap);
        }
    }

    // FEATURE 2: RESIZING
    private void resizeImage(float scaleFactor) {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        int newWidth = (int) (workingBitmap.getWidth() * scaleFactor);
        int newHeight = (int) (workingBitmap.getHeight() * scaleFactor);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        Bitmap resizedBitmap = Bitmap.createBitmap(workingBitmap, 0, 0,
                workingBitmap.getWidth(), workingBitmap.getHeight(), matrix, true);

        workingBitmap = resizedBitmap;
        imageView.setImageBitmap(workingBitmap);
        statusText.setText("Image resized to " + newWidth + "x" + newHeight);
    }

    // FEATURE 3: FILTERING
    private void applyFilter(FilterType filterType) {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap filteredBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(), workingBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(filteredBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();

        switch (filterType) {
            case GRAYSCALE:
                colorMatrix.setSaturation(0);
                statusText.setText("Grayscale filter applied");
                break;

            case SEPIA:
                colorMatrix.set(new float[]{
                        0.393f, 0.769f, 0.189f, 0, 0,
                        0.349f, 0.686f, 0.168f, 0, 0,
                        0.272f, 0.534f, 0.131f, 0, 0,
                        0, 0, 0, 1, 0
                });
                statusText.setText("Sepia filter applied");
                break;

            case NEGATIVE:
                colorMatrix.set(new float[]{
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, 1, 0
                });
                statusText.setText("Negative filter applied");
                break;
        }

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(workingBitmap, 0, 0, paint);
        workingBitmap = filteredBitmap;
        imageView.setImageBitmap(workingBitmap);
    }

    private void adjustBrightness(int value) {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap brightBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(), workingBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(brightBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix(new float[]{
                1, 0, 0, 0, value,
                0, 1, 0, 0, value,
                0, 0, 1, 0, value,
                0, 0, 0, 1, 0
        });

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(workingBitmap, 0, 0, paint);
        workingBitmap = brightBitmap;
        imageView.setImageBitmap(workingBitmap);
        statusText.setText("Brightness adjusted");
    }

    private void applyBlur() {
        if (workingBitmap == null) {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap blurredBitmap = Bitmap.createBitmap(
                workingBitmap.getWidth(), workingBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        int radius = 5;
        int w = workingBitmap.getWidth();
        int h = workingBitmap.getHeight();
        int[] pixels = new int[w * h];
        workingBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int y = radius; y < h - radius; y++) {
            for (int x = radius; x < w - radius; x++) {
                int r = 0, g = 0, b = 0, count = 0;

                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int pixel = pixels[(y + ky) * w + (x + kx)];
                        r += Color.red(pixel);
                        g += Color.green(pixel);
                        b += Color.blue(pixel);
                        count++;
                    }
                }

                pixels[y * w + x] = Color.rgb(r / count, g / count, b / count);
            }
        }

        blurredBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        workingBitmap = blurredBitmap;
        imageView.setImageBitmap(workingBitmap);
        statusText.setText("Blur effect applied");
    }

    // FEATURE 4: MERGING
    private void mergeImages() {
        if (workingBitmap == null || secondBitmap == null) {
            Toast.makeText(this, "Please load both images first", Toast.LENGTH_SHORT).show();
            return;
        }

        int width = Math.max(workingBitmap.getWidth(), secondBitmap.getWidth());
        int height = workingBitmap.getHeight() + secondBitmap.getHeight();

        Bitmap mergedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedBitmap);
        canvas.drawColor(Color.WHITE);

        canvas.drawBitmap(workingBitmap, 0, 0, null);
        canvas.drawBitmap(secondBitmap, 0, workingBitmap.getHeight(), null);

        workingBitmap = mergedBitmap;
        imageView.setImageBitmap(workingBitmap);
        statusText.setText("Images merged successfully");
    }

    private void resetImage() {
        if (originalBitmap == null) {
            Toast.makeText(this, "No original image to reset", Toast.LENGTH_SHORT).show();
            return;
        }
        workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(workingBitmap);
        statusText.setText("Image reset to original");
    }

    private void saveImage() {
        if (workingBitmap == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    workingBitmap,
                    "IMG_" + System.currentTimeMillis(),
                    "Manipulated Image"
            );

            if (savedImageURL != null) {
                Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_LONG).show();
                statusText.setText("Image saved successfully");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }
}