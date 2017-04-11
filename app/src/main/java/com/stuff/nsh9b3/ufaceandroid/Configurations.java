package com.stuff.nsh9b3.ufaceandroid;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by nick on 11/21/16.
 * This file sets the values used to communicate with the servers
 * This file also setups up how to do LBP and encrypt the information
 */

public interface Configurations
{
    // Paillier encryption size
    int BITS_FOR_ENCRYPTION = 1024;

    // Address of UFace
    // Right now everything is run on the same server, but in the future these will need to all be different values
    String UFACE_BASE_ADDRESS = "192.168.0.5";
    String UFACE_DATA_ADDRESS = UFACE_BASE_ADDRESS + ":" + "3000";
    String UFACE_BANK_ADDRESS = UFACE_BASE_ADDRESS + ":" + "3001";
    String UFACE_KEY_ADDRESS = UFACE_BASE_ADDRESS + ":" + "3002";

    // These are values used to communicate with specific APIs on the servers
    String UFACE_PUBLIC_KEY = "public_key";
    String UFACE_PUBLIC_KEY_NAME = "Public";
    String UFACE_SERVICE_LIST = "service_list";
    String UFACE_SERVICE_LIST_NAME = "Services";
    String SERVICE_ADD_USER = "add_user";
    String SERVICE_ADD_USER_RESULT = "add_user_result_client";
    String SERVICE_USER_KEY = "User";
    String SERVICE_USER_INDEX_KEY = "Index";
    String SERVICE_SERVICE_KEY = "Service";
    String PASSWORD_SIZE_KEY = "Size";
    String PASSWORD_KEY = "Password";
    String UFACE_REGISTRATION_PASSWORD = "add_password";
    String UFACE_AUTHENTICATE_USER = "authenticate_user";
    String UFACE_AUTHENTICATE_PASSWORD = "authenticate_password";
    String UFACE_AUTHENTICATION_RESULT = "authentication_result_client";

    // Change the number of rows/cols in the grid
    // Keep it so that there are the same amount of pixels per section of the grid
    int GRID_ROWS = 4;
    int GRID_COLS = GRID_ROWS;
    int GRID_SIZE = GRID_ROWS * GRID_COLS;

    // Change the number of pixels for each facial image
    int IMAGE_PIXEL_ROWS = 1024;
    int IMAGE_PIXEL_COLS = IMAGE_PIXEL_ROWS;
    int IMAGE_PIXEL_SIZE = IMAGE_PIXEL_ROWS * IMAGE_PIXEL_COLS;

    // Number of pixels per section
    int SECTION_PIXEL_ROWS = IMAGE_PIXEL_ROWS / GRID_ROWS;
    int SECTION_PIXEL_COLS = IMAGE_PIXEL_COLS / GRID_COLS;
    int SECTION_PIXEL_SIZE = SECTION_PIXEL_ROWS * SECTION_PIXEL_COLS;

    // Number of bins (should stay 59 - ie. don't change unless the LBP algorithm gets updated)
    int BINS = 59;
    int NEEDED_BINS = BINS * GRID_SIZE;

    // Math stuff for packing integers for faster encryption
    // Simply, these are constants used to convert an array of integers to an array of compact bytes
    // To understand what these do in detail, look at Utilities.createByteFV()
    int BITS_ALLOWED_PER_INT = 17;
    int BITS_NEEDED_PER_INT = (int) (Math.ceil(Math.log(SECTION_PIXEL_SIZE) / Math.log(2))) + 1;
    int ZERO_BITS_PER_INT = BITS_ALLOWED_PER_INT - BITS_NEEDED_PER_INT;
    int BYTES_PER_BIG_INT = BITS_FOR_ENCRYPTION / 8;
    int INTS_PER_BIG_INT = BITS_FOR_ENCRYPTION / BITS_ALLOWED_PER_INT;
    int ZERO_BITS_PER_BIG_INT = BITS_FOR_ENCRYPTION % BITS_ALLOWED_PER_INT;
    int LABELS_IN_FEATURE_VECTOR = IMAGE_PIXEL_SIZE - (2 * SECTION_PIXEL_COLS * GRID_COLS) - (2 * SECTION_PIXEL_ROWS * GRID_ROWS) + 4;
    int BIG_INTS_IN_FEATURE_VECTOR = (int)Math.ceil(NEEDED_BINS / (double)INTS_PER_BIG_INT);
}
