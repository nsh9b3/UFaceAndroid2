package com.stuff.nsh9b3.ufaceandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * Created by nick on 11/23/16.
 * These functions do random things that don't necessarily fit anywhere
 */

public class Utilities
{
    // This function converts an input stream from the service into a String
    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    // Takes a photo using the camera
    public static String takePhoto(Activity activity)
    {
        // Create the File where the photo should go
        File photoFile = null;

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.getBaseContext().getPackageManager()) != null)
        {
            try
            {
                photoFile = Utilities.createImageFile(activity.getBaseContext());
            } catch (IOException ex)
            {
                // Error occurred while creating the File
            }

            // Continue only if the File was successfully created
            if (photoFile.exists())
            {
                Log.d("TAG", "path: " + photoFile.getAbsolutePath());
                // Take a picture and place the information in the newly created file
                Uri photoURI = FileProvider.getUriForFile(activity.getBaseContext().getApplicationContext(),
                        "com.stuff.nsh9b3.ufaceandroid.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                activity.startActivityForResult(takePictureIntent, IntentKeys.REQUEST_TAKE_PHOTO);
            }
        }
        return photoFile.getAbsolutePath();
    }

    // Creates a temporary image
    public static File createImageFile(Context that) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = that.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    public static File createTimeSheet(Context that) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String timeFileName = "Time_" + timeStamp + "_";
        File storageDir = that.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = File.createTempFile(
                timeFileName,  /* prefix */
                ".txt",         /* suffix */
                storageDir      /* directory */
        );

        return file;
    }

    // Takes a bitmap and breaks it into multiple bitmaps (based on the number of sections)
    public static int[][] splitImageIntoSections(Bitmap bitmap)
    {
        int[][] pixelMap = new int[Configurations.GRID_SIZE][Configurations.SECTION_PIXEL_SIZE];

        for(int i = 0; i < Configurations.GRID_SIZE; i++)
        {
            int[] secPixels = new int[Configurations.SECTION_PIXEL_SIZE];
            bitmap.getPixels(secPixels, 0, Configurations.SECTION_PIXEL_COLS,
                    (i % Configurations.GRID_COLS) * Configurations.SECTION_PIXEL_COLS,
                    (i / Configurations.GRID_ROWS) * Configurations.SECTION_PIXEL_ROWS,
                    Configurations.SECTION_PIXEL_COLS, Configurations.SECTION_PIXEL_ROWS);
            pixelMap[i] = secPixels;
        }

        return pixelMap;
    }

    // Converts a bitmap to grayscale (LBP uses intensity instead of color)
    public static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    // Changes an image size to match the size of the size specified in Configurations file
    public static Bitmap resizeImage(String imagePath)
    {
        Bitmap origBitmap = BitmapFactory.decodeFile(imagePath);
        Bitmap grayBitmap = toGrayscale(origBitmap);
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(grayBitmap, Configurations.IMAGE_PIXEL_COLS, Configurations.IMAGE_PIXEL_ROWS, true);

        return resizeBitmap;
    }

    // Splits the FV into multiple portions
    // Each portion is the size of the number of elements that can be encrypted at once
    // So if there are 944 bins total and each bin uses 13 bits, then 78 ints can be concatenated together
    public static int[][] splitFVForEncryption(int[][] intFV)
    {
        int[][] splitFV = new int[Configurations.BIG_INTS_IN_FEATURE_VECTOR][Configurations.INTS_PER_BIG_INT];

        int outIndex = 0;
        int inIndex = 0;
        for(int i = 0; i < Configurations.BIG_INTS_IN_FEATURE_VECTOR; i++)
        {
            for(int k = 0; k < Configurations.INTS_PER_BIG_INT; k++)
            {
                splitFV[i][k] = intFV[outIndex][inIndex++];
                if(inIndex == Configurations.BINS)
                {
                    outIndex++;
                    inIndex = 0;
                    if(outIndex == Configurations.GRID_SIZE)
                    {
                        break;
                    }
                }
            }
            if(outIndex == Configurations.GRID_SIZE)
            {
                break;
            }
        }
        return splitFV;
    }

    // This COMPLICATED function creates each array of integers from the function above into an array of bytes (to be converted to BitIntegers later)
    public static byte[][] createByteFV(int[][] splitFV)
    {
        byte[][] byteFV = new byte[Configurations.BIG_INTS_IN_FEATURE_VECTOR][Configurations.BYTES_PER_BIG_INT];

        // For the number of BigIntegers we need to create
        for(int i = 0; i < Configurations.BIG_INTS_IN_FEATURE_VECTOR; i++)
        {
            // Grab the array of integers that will be used for this BigInteger
            int[] intArray = splitFV[i];

            // Figure out how many empty bits there are needed at the beginning
            int leftEmptyBits = Configurations.ZERO_BITS_PER_BIG_INT;

            // Values used in calculations
            byte next = 0x00;
            int index = 0;
            int bitsUsedPerByte = 0;

            // For each integer
            for(int k = 0; k < splitFV[i].length; k++)
            {
                // This should be zero, but depends on the programmer
                // It's the number of excess bits used to represent each integer
                leftEmptyBits += Configurations.ZERO_BITS_PER_INT;
                // This is simply the number of bits needed to represent each integer fully
                int bitsNeededPerInt = Configurations.BITS_NEEDED_PER_INT;
                // The current integer
                int val = intArray[k];

                // While we need to skip bits
                while(bitsNeededPerInt > 0)
                {
                    // If we needed to skip more than a byte
                    if(leftEmptyBits >= 8)
                    {
                        // Add the blank byte to the byte array
                        byteFV[i][index++] = next;
                        next = 0x00;
                        leftEmptyBits -= 8;
                    }
                    else
                    {
                        // If we can fill the next byte with ONLY bits from the current int
                        if (bitsNeededPerInt >= (8 - leftEmptyBits - bitsUsedPerByte))
                        {
                            // Fill the rest of the byte based off this integer
                            int shiftR = bitsNeededPerInt + leftEmptyBits + bitsUsedPerByte - 8;
                            next = (byte) (((val >>> shiftR) & 0xFF) | next);
                            bitsNeededPerInt -= (8 - leftEmptyBits - bitsUsedPerByte);
                            bitsUsedPerByte = 0;
                            // add the byte
                            byteFV[i][index++] = next;
                            next = 0x00;
                            leftEmptyBits = 0; // I think this value should be 'Configurations.ZERO_BITS_PER_INT', but like I said earlier, it should be 0 anyway
                        }
                        else
                        {
                            // Fill the next byte with what we can of this current integer
                            int shiftL = 8 - bitsNeededPerInt;
                            next = (byte) (((val << shiftL) & 0xFF) | next);
                            // Get ready for the next integer value
                            bitsUsedPerByte = bitsNeededPerInt;
                            bitsNeededPerInt = 0;
                        }
                    }
                }
            }
        }

        return byteFV;
    }

    // Encrypt the feature vector by converting each array of bytes to an array of BigIntegers and then calling Paillier Encryption
    public static String encryptFV(byte[][] byteFV)
    {
        BigInteger[] encryptedFV = new BigInteger[Configurations.BIG_INTS_IN_FEATURE_VECTOR];

        for(int i = 0; i < Configurations.BIG_INTS_IN_FEATURE_VECTOR; i++)
        {
            BigInteger bigInt = new BigInteger(byteFV[i]);
            encryptedFV[i] = MainActivity.paillier.Encryption(bigInt);
        }

        // Convert the Encrypted BigInteger array into a String
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encryptedFV.length; i++)
        {
            sb.append(encryptedFV[i]).append(" ");
        }

        return sb.toString();
    }

    /*
    public static void writeToFile(String data, Context context, String fileName) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String readFromFile(Context context, String fileName)
    {
        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(fileName);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }*/
}
