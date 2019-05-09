package com.edusoho.videoplayer.media.encrypt;

import android.util.Log;

/**
 * Created by suju on 17/3/23.
 */

public class ArrayEncrpyt {

    public byte[] encrpytBuffer(byte[] buffer, int offset) {
        if (offset <= 16) {
            return null;
        }
        String decodeKey = "";
        if (offset == 17) {
            decodeKey = decodeByMode3(new String(buffer, 0, offset));
            return decodeKey.getBytes();
        }
        if (offset > 20) {
            return null;
        }
        int mode = getKeyMode(new String(buffer, 0, offset));
        if (mode == -1) {
            return null;
        }
        switch (mode) {
            case 0:
                decodeKey = decodeByMode0(new String(buffer, 0, offset));
                break;
            case 1:
                decodeKey = decodeByMode1(new String(buffer, 0, offset));
                break;
            case 2:
                decodeKey = decodeByMode2(new String(buffer, 0, offset));
                break;
        }
        return decodeKey.getBytes();
    }

    private int getKeyMode(String key) {
        int firstBit = -1;
        try {
            firstBit = Integer.parseInt(key.substring(0, 1), 36) % 7;
        } catch (Exception e) {
            Log.d("ArrayEncrpyt", "parseInt error:" + key);
        }

        if (firstBit == -1) {
            return -1;
        }

        char[] keyArray = key.toCharArray();
        char[] keyBitArray = new char[2];
        keyBitArray[0] = keyArray[firstBit];
        keyBitArray[1] = keyArray[firstBit + 1];

        try {
            firstBit = Integer.parseInt(new String(keyBitArray), 36) % 3;
        } catch (Exception e) {
            Log.d("ArrayEncrpyt", "parseInt error:" + key);
        }
        return firstBit;
    }

    private String decodeByMode0(String key) {
        char[] keyArray = key.toCharArray();
        char[] decodeKeyArray = new char[16];

        for(int i = 0; i < 9; i++) {
            decodeKeyArray[i] = keyArray[i];
        }
        for(int i = 9; i < 12; i++) {
            decodeKeyArray[i] = keyArray[i + 1];
        }
        for(int i = 12; i < 15; i++) {
            decodeKeyArray[i] = keyArray[i + 2];
        }
        decodeKeyArray[15] = keyArray[18];

        return new String(decodeKeyArray);
    }

    private String decodeByMode1(String key) {
        char[] keyArray = key.toCharArray();
        char[] decodeKeyArray = new char[16];

        int i = 0;
        for(i = 0; i < 8; i++) {
            decodeKeyArray[i] = keyArray[i];
        }

        decodeKeyArray[i++] = keyArray[18];
        decodeKeyArray[i++] = keyArray[16];
        decodeKeyArray[i++] = keyArray[15];
        decodeKeyArray[i++] = keyArray[13];
        decodeKeyArray[i++] = keyArray[12];
        decodeKeyArray[i++] = keyArray[11];
        decodeKeyArray[i++] = keyArray[10];
        decodeKeyArray[i++] = keyArray[8];

        return new String(decodeKeyArray);
    }

    private String decodeByMode2(String key) {
        char[] keyArray = key.toCharArray();
        char[] decodeKeyArray = new char[16];

        for(int i = 0; i < 8; i++) {
            decodeKeyArray[i] = keyArray[i];
        }

        int code9 = (Integer.parseInt(keyArray[9] + "", 36) + 1) * 26 + keyArray[8] - 'a' - 'a';
        decodeKeyArray[8] = (char)code9;
        int code10 = (Integer.parseInt(keyArray[11] + "", 36) + 1) * 26 + keyArray[10] - 'a' - 'a';
        decodeKeyArray[9] = (char)code10;

        decodeKeyArray[10] = keyArray[12];
        decodeKeyArray[11] = keyArray[13];
        decodeKeyArray[12] = keyArray[14];

        int code14 = (Integer.parseInt(keyArray[16] + "", 36) + 1) * 26 + keyArray[15] - 'a' - 'a';
        decodeKeyArray[13] = (char)code14;
        int code15 = (Integer.parseInt(keyArray[18] + "", 36) + 2) * 26 + keyArray[17] - 'a' - 'a';
        decodeKeyArray[14] = (char)code15;

        decodeKeyArray[15] = keyArray[19];

        return new String(decodeKeyArray);
    }

    private String decodeByMode3(String key) {
        char[] keyArray = key.toCharArray();
        char[] decodeKeyArray = new char[16];

        for(int i = 1; i < 16; i++) {
            decodeKeyArray[i] = keyArray[i + 1];
        }

        decodeKeyArray[0] = keyArray[9];
        decodeKeyArray[1] = keyArray[10];
        decodeKeyArray[8] = keyArray[1];
        decodeKeyArray[9] = keyArray[2];


        return new String(decodeKeyArray);
    }
}
