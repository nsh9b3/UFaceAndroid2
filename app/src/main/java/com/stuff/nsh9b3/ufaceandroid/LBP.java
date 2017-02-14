package com.stuff.nsh9b3.ufaceandroid;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by nick on 11/21/16.
 */

public class LBP
{
    // Keys for histogram containing uniform numbers only
    private static HashMap<Integer, Integer> histogramKeys;

    public static int[][] generateFeatureVector(int[][] pixels)
    {
        int[][] featureVector = new int[Configurations.GRID_SIZE][Configurations.BINS];

        // Keys used to properly list uniform values
        histogramKeys = new HashMap<>();
        generateKeyMappings(histogramKeys);

        for(int i = 0; i < Configurations.GRID_SIZE; i++)
        {
            featureVector[i] = generateRegionHistogram(pixels, i);
        }

        return featureVector;
    }

    private static int[] generateRegionHistogram(int[][] pixels, int region)
    {
        int[] histogram = new int[Configurations.BINS];
        int startRow = (region >= Configurations.GRID_COLS ? 0 : 1);
        int endRow = (region < Configurations.GRID_SIZE - Configurations.GRID_COLS ? Configurations.SECTION_PIXEL_ROWS : Configurations.SECTION_PIXEL_ROWS - 1);
        int startCol = (region % Configurations.GRID_ROWS != 0 ? 0 : 1);
        int endCol = (region % Configurations.GRID_ROWS != (Configurations.GRID_ROWS - 1) ? Configurations.SECTION_PIXEL_COLS : Configurations.SECTION_PIXEL_COLS - 1);

        for(int i = startRow; i < endRow; i++)
        {
            for(int k = startCol; k < endCol; k++)
            {
                int label = getLabel(pixels, region, i, k);

                // Place the value in the correct spot in the array based off the keys
                // If the value is not uniform value, throw it into the last bin
                if (histogramKeys.get((label & 0xFF)) != null)
                    histogram[histogramKeys.get((label & 0xFF))]++;
                else
                    histogram[histogram.length - 1]++;
            }
        }

        return histogram;
    }

    private static int getLabel(int[][] pixels, int region, int row, int col)
    {
        int label = 0;

        int pValue = pixels[region][row * Configurations.SECTION_PIXEL_ROWS + col] & 0xFF;

        // Top Left Pixel
        int tL = 0 & 0xFF;

        if(row != 0 && col != 0)
        {
            tL = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_ROWS + (col - 1)] & 0xFF;
        }
        else if(row == 0 && col != 0)
        {
            tL = pixels[region - Configurations.GRID_ROWS][Configurations.SECTION_PIXEL_SIZE - Configurations.SECTION_PIXEL_COLS + (col - 1)] & 0xFF;
        }
        else if(row != 0 && col == 0)
        {
            tL = pixels[region - 1][(row - 1) * Configurations.SECTION_PIXEL_ROWS + (Configurations.SECTION_PIXEL_COLS - 1)] & 0xFF;
        }
        else
        {
            tL = pixels[region - Configurations.GRID_ROWS - 1][Configurations.SECTION_PIXEL_SIZE - 1] & 0xFF;
        }

        int t = 0;
        if(row != 0)
        {
            t = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_COLS + col] & 0xFF;
        }
        else
        {
            t = pixels[region - Configurations.GRID_ROWS][Configurations.SECTION_PIXEL_SIZE - Configurations.SECTION_PIXEL_COLS + col] & 0xFF;
        }

        int tR = 0;
        if(row != 0 && col != (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            tR = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_ROWS + (col + 1)] & 0xFF;
        }
        else if(row == 0 && col != (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            tR = pixels[region - Configurations.GRID_ROWS][Configurations.SECTION_PIXEL_SIZE - Configurations.SECTION_PIXEL_COLS + (col + 1)] & 0xFF;
        }
        else if(row != 0 && col == (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            tR = pixels[region + 1][(row - 1) * Configurations.SECTION_PIXEL_ROWS] & 0xFF;
        }
        else
        {
            tR = pixels[region - Configurations.GRID_ROWS + 1][Configurations.SECTION_PIXEL_SIZE - Configurations.SECTION_PIXEL_ROWS] & 0xFF;
        }

        int r = 0;
        if(col != (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            r = pixels[region][row * Configurations.SECTION_PIXEL_ROWS + (col + 1)] & 0xFF;
        }
        else
        {
            r = pixels[region + 1][row * Configurations.SECTION_PIXEL_ROWS] & 0xFF;
        }

        int dR = 0;
        if(row != (Configurations.SECTION_PIXEL_COLS - 1) && col != (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            dR = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_ROWS + (col + 1)] & 0xFF;
        }
        else if(row == (Configurations.SECTION_PIXEL_COLS - 1) && col != (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            dR = pixels[region + Configurations.GRID_ROWS][(col + 1)] & 0xFF;
        }
        else if(row != (Configurations.SECTION_PIXEL_COLS - 1) && col == (Configurations.SECTION_PIXEL_ROWS - 1))
        {
            dR = pixels[region + 1][(row + 1) * Configurations.SECTION_PIXEL_ROWS] & 0xFF;
        }
        else
        {
            dR = pixels[region + (Configurations.GRID_ROWS + 1)][0] & 0xFF;
        }

        int d = 0;
        if(row != (Configurations.SECTION_PIXEL_COLS - 1))
        {
            d = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_ROWS + col] & 0xFF;
        }
        else
        {
            d = pixels[region + Configurations.GRID_ROWS][col] & 0xFF;
        }

        int dL = 0;
        if(row != (Configurations.SECTION_PIXEL_COLS - 1) && col != 0)
        {
            dL = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_ROWS + (col - 1)] & 0xFF;
        }
        else if(row == (Configurations.SECTION_PIXEL_COLS - 1) && col != 0)
        {
            dL = pixels[region + Configurations.GRID_ROWS][(col - 1)] & 0xFF;
        }
        else if(row != (Configurations.SECTION_PIXEL_COLS - 1) && col == 0)
        {
            dL = pixels[region - 1][(row + 1) * Configurations.SECTION_PIXEL_ROWS + (Configurations.SECTION_PIXEL_ROWS - 1)] & 0xFF;
        }
        else
        {
            dL = pixels[region + (Configurations.GRID_ROWS - 1)][(Configurations.SECTION_PIXEL_ROWS - 1)] & 0xFF;
        }

        int l = 0;
        if(col != 0)
        {
            l = pixels[region][row * Configurations.SECTION_PIXEL_ROWS + (col - 1)] & 0xFF;
        }
        else
        {
            l = pixels[region - 1][row * Configurations.SECTION_PIXEL_ROWS + (Configurations.SECTION_PIXEL_ROWS - 1)] & 0xFF;
        }

        if(tL > pValue)
            label = label | 0x80;
        if(t > pValue)
            label = label | 0x40;
        if(tR > pValue)
            label = label | 0x20;
        if(r > pValue)
            label = label | 0x10;
        if(dR > pValue)
            label = label | 0x08;
        if(d > pValue)
            label = label | 0x04;
        if(dL > pValue)
            label = label | 0x02;
        if(l > pValue)
            label = label | 0x01;

        return label;
    }


    //This probably won't work with different grid heights and widths
    private static int getLabel(int[][] pixels, int region, int row, int col,
                                boolean secLeft, boolean secRight,
                                boolean secUp, boolean secDown)
    {
        int label = 0;

        int pValue = pixels[region][row * Configurations.SECTION_PIXEL_COLS + col] & 0xFF;

        // Top Left Pixel
        int tL;
        if(secUp && secLeft)
            tL = pixels[region - (Configurations.GRID_COLS + 1)][Configurations.SECTION_PIXEL_ROWS* Configurations.SECTION_PIXEL_COLS - 1] & 0xFF;
        else if(secUp && !secLeft)
            tL = pixels[region - Configurations.GRID_COLS][(Configurations.SECTION_PIXEL_ROWS - 1) * Configurations.SECTION_PIXEL_COLS + (col - 1)] & 0xFF;
        else if(!secUp && secLeft)
            tL = pixels[region - 1][(row - 1) * Configurations.SECTION_PIXEL_COLS + (Configurations.SECTION_PIXEL_COLS - 1)] & 0xFF;
        else
            tL = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_COLS + (col - 1)] & 0xFF;

        // Top Pixel
        int t;
        if(secUp)
            t = pixels[region - Configurations.GRID_COLS][(Configurations.SECTION_PIXEL_ROWS - 1) * Configurations.SECTION_PIXEL_COLS + (col)] & 0xFF;
        else
            t = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_COLS + (col)] & 0xFF;

        // Top Right Pixel
        int tR;
        if(secUp && secRight)
            tR = pixels[region - (Configurations.GRID_COLS - 1)][(Configurations.SECTION_PIXEL_ROWS - 1) * Configurations.SECTION_PIXEL_COLS] & 0xFF;
        else if(secUp && !secRight)
            tR = pixels[region - Configurations.GRID_COLS][(Configurations.SECTION_PIXEL_ROWS - 1) * Configurations.SECTION_PIXEL_COLS + (col + 1)] & 0xFF;
        else if(!secUp && secRight)
            tR = pixels[region + 1][(row - 1) * Configurations.SECTION_PIXEL_COLS] & 0xFF;
        else
            tR = pixels[region][(row - 1) * Configurations.SECTION_PIXEL_COLS + (col + 1)] & 0xFF;

        // Right Pixel
        int r;
        if(secRight)
            r = pixels[region + 1][row * Configurations.SECTION_PIXEL_COLS] & 0xFF;
        else
            r = pixels[region][row * Configurations.SECTION_PIXEL_COLS + (col + 1)] & 0xFF;

        // Down Right Pixel
        int dR;
        if(secDown && secRight)
            dR = pixels[region + (Configurations.GRID_COLS + 1)][0] & 0xFF;
        else if(secDown && !secRight)
            dR = pixels[region + Configurations.GRID_COLS][(col + 1)] & 0xFF;
        else if(!secDown && secRight)
            dR = pixels[region + 1][(row + 1) * Configurations.SECTION_PIXEL_COLS] & 0xFF;
        else
        {
            dR = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_COLS + (col + 1)] & 0xFF;
        }


        // Down Pixel
        int d;
        if(secDown)
            d = pixels[region + Configurations.GRID_COLS][col] & 0xFF;
        else
            d = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_COLS + col] & 0xFF;

        // Down Left Pixel
        int dL;
        if(secDown && secLeft)
            dL = pixels[region + (Configurations.GRID_COLS - 1)][(Configurations.SECTION_PIXEL_COLS - 1)] & 0xFF;
        else if(secDown && !secLeft)
            dL = pixels[region + Configurations.GRID_COLS][(col - 1)] & 0xFF;
        else if(!secDown && secLeft)
            dL = pixels[region - 1][(row + 1) * Configurations.SECTION_PIXEL_COLS + (Configurations.SECTION_PIXEL_COLS - 1)] & 0xFF;
        else
            dL = pixels[region][(row + 1) * Configurations.SECTION_PIXEL_COLS + (col - 1)] & 0xFF;

        // Left Pixel
        int l;
        if(secLeft)
            l = pixels[region - 1][row * Configurations.SECTION_PIXEL_COLS + (Configurations.SECTION_PIXEL_COLS - 1)] & 0xFF;
        else
            l = pixels[region][row * Configurations.SECTION_PIXEL_COLS + (col - 1)] & 0xFF;

        if(tL > pValue)
            label = label | 0x80;
        if(t > pValue)
            label = label | 0x40;
        if(tR > pValue)
            label = label | 0x20;
        if(r > pValue)
            label = label | 0x10;
        if(dR > pValue)
            label = label | 0x08;
        if(d > pValue)
            label = label | 0x04;
        if(dL > pValue)
            label = label | 0x02;
        if(l > pValue)
            label = label | 0x01;

        return label;
    }

    /**
     * Generates bins for the numbers below only. These are uniform values (less than 3 bitwise changes
     * in each value). All other values get dumped into a separate bin (non-uniform bin).
     * Only contains 1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 24, 28, 30, 31, 32, 48, 56, 60, 62, 63, 64,
     * 96, 112, 120, 124, 126, 127, 128, 129, 131, 135, 143, 159, 191, 192, 193, 195, 199, 207, 223,
     * 224, 225, 227, 231, 239, 240, 241, 243, 247, 248, 249, 251, 252, 253, 254, 255
     *
     * @param keys hashmap for these uniform values into the correct location in an array
     */
    private static void generateKeyMappings(HashMap<Integer, Integer> keys)
    {
        int count = 0;
        for (int i = 0; i < 256; i++)
        {
            byte value = (byte) i;
            int transitions = 0;
            int last = value & 1;
            for (int k = 1; k < 8; k++)
            {
                if (((value >> k) & 1) != last)
                {
                    last = ((value >> k) & 1);
                    transitions++;
                    if (transitions > 2)
                    {
                        break;
                    }
                }
            }
            if (transitions <= 2)
            {
                keys.put(i, count++);
            }
        }
    }
}
