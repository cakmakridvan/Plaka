package com.aeyacin.cemaradevicetrack.utils.converters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class NV21toYuv420 {

    public static byte[] NV21toYUV420Planar(byte[] input, byte[] output, int width, int height) {


        /*
         * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
         * So we just have to reverse U and V.
         */
        final int frameSize = (width * height);
        final int qFrameSize = frameSize/4;

        System.arraycopy(input, 0, output, 0, frameSize); // Y
        /*
        System.arraycopy(input, frameSize, output, frameSize + qFrameSize, qFrameSize); // Cr (V)
        System.arraycopy(input, frameSize + qFrameSize, output, frameSize, qFrameSize); // Cb (U)
*/

        byte v, u;

        for (int i = 0; i < qFrameSize; i++) {
            v = input[frameSize + i*2];
            u = input[frameSize + i*2 + 1];

            output[frameSize + i + qFrameSize] = v;
            output[frameSize + i] = u;
        }



        return output;
    }

    public byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
        byte[] i420bytes = new byte[yv12bytes.length];
        for (int i = 0; i < width*height; i++)
            i420bytes[i] = yv12bytes[i];
        for (int i = width*height; i < width*height + (width/2*height/2); i++)
            i420bytes[i] = yv12bytes[i + (width/2*height/2)];
        for (int i = width*height + (width/2*height/2); i < width*height + 2*(width/2*height/2); i++)
            i420bytes[i] = yv12bytes[i - (width/2*height/2)];
        return i420bytes;
    }

    public static void NV21toImage(byte[] nv21bytearray,int width,int height,String path ){

        YuvImage yuvImage = new YuvImage(nv21bytearray, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + path + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
