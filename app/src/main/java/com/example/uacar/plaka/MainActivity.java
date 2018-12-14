package com.example.uacar.plaka;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;

    Drawable drawable;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
      // drawable = ContextCompat.getDrawable(this, R.drawable.kes);
       bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.kes);//((BitmapDrawable) drawable).getBitmap();

       // String imageInSD = "/sdcard/plaka/kes.png";
        //Bitmap bitmap = BitmapFactory.decodeFile(imageInSD);

        //Bitmap newBitmap = test(bitmap);
        Bitmap newBitmap = convertImage(bitmap);
        imageView.setImageBitmap(newBitmap);

    }

    public static Bitmap convertImage(Bitmap original) {
        Bitmap finalimage = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());

        int A, R, G, B;
        int colorPixel;
        int width = original.getWidth();
        int height = original.getHeight();
        //int width=3840;
        //int height=732;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                try {
                    colorPixel = original.getPixel(x, y);
                    A = Color.alpha(colorPixel);
                    R = Color.red(colorPixel);
                    G = Color.green(colorPixel);
                    B = Color.blue(colorPixel);

                    R = (R + B + G) / 3;
                    //G = R;
                    //B = R;


                    finalimage.setPixel(x, y, Color.argb(A, R, R, R));

                } catch (Exception e) {

                    String a = e.toString();
                }


            }
        }


        return finalimage;
    }
    public static Bitmap test(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);
                // use 128 as threshold, above -> white, below -> black
                if (gray > 128) {
                    gray = 255;
                } else {
                    gray = 0;
                }
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
            }
        }
        return bmOut;
    }

}
