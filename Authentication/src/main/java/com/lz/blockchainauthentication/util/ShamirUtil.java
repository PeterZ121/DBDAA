package com.lz.blockchainauthentication.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;


public class ShamirUtil {
    private static final SecureRandom random = new SecureRandom();


    public static byte[][] partialSign(byte[] data , KeyPair keyPair, int n, int k) throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] encodedPrivateKey = privateKey.getEncoded();
        byte[][] shares = new byte[n][];
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedPrivateKey = cipher.doFinal(encodedPrivateKey);
        for (int i = 0; i < n; i++) {
            byte[] randomBytes = new byte[16];
            random.nextBytes(randomBytes);
            shares[i] = new byte[encryptedPrivateKey.length + randomBytes.length];
            System.arraycopy(encryptedPrivateKey, 0, shares[i], 0, encryptedPrivateKey.length);
            System.arraycopy(randomBytes, 0, shares[i], encryptedPrivateKey.length, randomBytes.length);
        }
        return shares;
    }

    public static byte[] aggregateSignatures(byte[][] signatures) throws Exception {
        int signatureLength = signatures[0].length;
        byte[] aggregatedSignature = new byte[signatureLength];
        for (int i = 0; i < signatureLength; i++) {
            byte[] xor = new byte[signatures.length];
            for (int j = 0; j < signatures.length; j++) {
                xor[j] = signatures[j][i];
            }
            aggregatedSignature[i] = xorByteArray(xor);
        }
        return aggregatedSignature;
    }


    public static boolean verifySignature(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    private static byte xorByteArray(byte[] bytes) {
        byte result = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            result ^= bytes[i];
        }
        return result;
    }
}
