package com.lz.blockchainauthentication.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets; // 导入StandardCharsets类，用于字符串编码。
import java.security.MessageDigest; // 导入MessageDigest类，用于生成哈希值。
import java.security.NoSuchAlgorithmException; // 导入NoSuchAlgorithmException类，用于处理异常。

public class HashUtil {

    final static int a = 10;
    final static int a2 = a/2;
    final static int b = 10;
    final static int c = 10;



    public static String hash0(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = Math.abs((hash * 31 + input.charAt(i)) % (int)Math.pow(10, a));
        }
        return String.format("%0" + a + "d", hash);
    }



    public static String hash1(String input) {


        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = (hash * 31 + input.charAt(i)) % 1000000;
        }
        return String.format("%0" + a2 + "x", hash);
    }



    public static String hash2(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = Math.abs((hash * 17 + input.charAt(i)) % (int)Math.pow(10, b));
        }
        return String.format("%0" + b + "d", hash);
    }


    public static String hash3(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = Math.abs((hash * 13 + input.charAt(i)) % (int)Math.pow(10, c));
        }
        return String.format("%0" + c + "d", hash);
    }


    public static BigInteger uHash(BigInteger input){
        try {

            byte[] inputData = input.toByteArray();


            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedData = digest.digest(inputData);

            BigInteger hashedBigInt = new BigInteger(1, hashedData);


            int bitLength = hashedBigInt.bitLength();
            if (bitLength > 256) {

                hashedBigInt = hashedBigInt.shiftRight(bitLength - 256);
            } else if (bitLength < 256) {

                hashedBigInt = hashedBigInt.shiftLeft(256 - bitLength);
            }

            return hashedBigInt;
        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
            return null;
        }
    }





}



