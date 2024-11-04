package com.lz.blockchainauthentication.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;



public class ECCUtil {

    private static KeyPairGenerator keyGen;
    private static Cipher cipher;
    private static KeyFactory keyFactory;
    private static Signature signatureTool;
    private static AlgorithmParameters parameters;
    private static ECParameterSpec ecSpec;
    private static final BigInteger a = new BigInteger("0");
    private static final BigInteger b = new BigInteger("7");
    private static final BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    private static final BigInteger Gx = new BigInteger("55066263022277343669578718895168534326250603453777594175500187360389116729240");
    private static final BigInteger Gy = new BigInteger("32670510020758816978083085130507043184471273380659243275938904335757337482424");
    static {
        Security.addProvider(new BouncyCastleProvider());
        try{
            keyGen = KeyPairGenerator.getInstance("EC", "BC");
            cipher = Cipher.getInstance("ECIES", "BC");
            keyFactory = KeyFactory.getInstance("EC", "BC");
            signatureTool = Signature.getInstance("SHA256withECDSA", "BC");
            parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ecSpec = parameters.getParameterSpec(ECParameterSpec.class);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public static KeyPair generate(){


        try {
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        KeyPair keyPair =  keyGen.generateKeyPair();
        return keyPair;
    }

    public static KeyPair generateSame(byte number){


        byte[] seed = new byte[1];
        seed[0] = number;
        System.out.println("Random seed:"+Arrays.toString(seed));

        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(seed);
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), random);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        KeyPair keyPair = keyGen.generateKeyPair();
        System.out.println(ECCUtil.privateKeyToInt(keyPair.getPrivate()));
        System.out.println(ECCUtil.publicKeyToStr(keyPair.getPublic()));
        return keyPair;

    }

    public static String encrypt(String plainText, PublicKey publicKey) {

        byte[] cipherBytes = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherBytes = cipher.doFinal(plainText.getBytes());
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        String cipherText = Base64.getEncoder().encodeToString(cipherBytes);
        return cipherText;
    }


    public static String decrypt(String cipherText, PrivateKey privateKey) {

        if (cipherText == null || cipherText.length() == 0) {

            return "";

        }
        byte[] plainBytes = null;

        byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            plainBytes = cipher.doFinal(cipherBytes);
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return new String(plainBytes);

    }


    public static String signMessage(String message, PrivateKey privateKey){



        try{
            signatureTool.initSign(privateKey);
            signatureTool.update(message.getBytes());
            byte[] signatureBytes = signatureTool.sign();

            String signature = Base64.getEncoder().encodeToString(signatureBytes);

            return signature;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }


    public static boolean verifySignature(String message, String signature, PublicKey publicKey){


        byte[] decode = null;
        boolean result;
        try{
            decode = Base64.getDecoder().decode(signature);
            signatureTool.initVerify(publicKey);
            signatureTool.update(message.getBytes());
            result = signatureTool.verify(decode);
        }catch (Exception e){
            e.printStackTrace();
            result = false;
        }

        return result;
    }


    public static String publicKeyToStr(PublicKey publicKey) {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        BigInteger affineX = ecPublicKey.getW().getAffineX();
        BigInteger affineY = ecPublicKey.getW().getAffineY();
        return affineX+","+affineY;
    }
    public static PublicKey strToPublicKey(String publicKeyString){

        String[] parts = publicKeyString.split(",");
        BigInteger affineX = new BigInteger(parts[0]);
        BigInteger affineY = new BigInteger(parts[1]);
        ECPoint point = new ECPoint(affineX, affineY);
        try{
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(point, ecSpec);
            return keyFactory.generatePublic(publicKeySpec);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }



    }

    public static BigInteger privateKeyToInt(PrivateKey privateKey){
        ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
        BigInteger value = ecPrivateKey.getS();
        return value;
    }

    public static PrivateKey strToPrivateKey(BigInteger privateKeyInt){

        try{

            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, ecSpec);
            return keyFactory.generatePrivate(privateKeySpec);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }



    }


    public static BigInteger[] convertToCurvePoint(BigInteger x) {

        BigInteger ySquare = x.pow(3).add(a.multiply(x)).add(b).mod(p);


        BigInteger y = ySquare.modPow(p.add(BigInteger.ONE).divide(new BigInteger("4")), p);


        return new BigInteger[]{x, y};
    }



    public static BigInteger[] messageToPoint(String message) throws NoSuchAlgorithmException {
        BigInteger xCoordinate = hashToBigInteger(message).mod(p);
        BigInteger yCoordinate = calculateYCoordinate(xCoordinate);
        return new BigInteger[]{xCoordinate, yCoordinate};
    }




    public static String pointToMessage(BigInteger[] point) {
        BigInteger xCoordinate = point[0];

        return xCoordinate.toString();
    }

    private static BigInteger hashToBigInteger(String message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(message.getBytes());
        return new BigInteger(1, hashBytes);
    }


    private static BigInteger calculateYCoordinate(BigInteger x) {
        BigInteger xCubed = x.modPow(new BigInteger("3"), p);
        BigInteger ax = a.multiply(x).mod(p);
        BigInteger ySquared = xCubed.add(ax).add(b).mod(p);
        BigInteger y = ySquared.modPow(p.add(BigInteger.ONE).divide(new BigInteger("4")), p);
        return y;
    }


    public static BigInteger[] pointAddition(BigInteger[] P, BigInteger[] Q) {
        if (P[0].equals(Q[0]) && P[1].equals(Q[1])) {

            return pointDoubling(P);
        }


        BigInteger lambda = (Q[1].subtract(P[1])).multiply((Q[0].subtract(P[0])).modInverse(p));


        BigInteger xR = lambda.pow(2).subtract(P[0]).subtract(Q[0]).mod(p);
        BigInteger yR = lambda.multiply(P[0].subtract(xR)).subtract(P[1]).mod(p);

        return new BigInteger[]{xR, yR};
    }


    public static BigInteger[] pointMultiplication(BigInteger k, BigInteger[] P) {

        BigInteger[] result = {BigInteger.ZERO, BigInteger.ZERO};
        for (int i = 0; i < k.bitLength(); i++) {

            if (k.testBit(i)) {
                result = pointAddition(result, P);
            }

            P = pointDoubling(P);
        }
        return result;
    }


    public static BigInteger[] pointDoubling(BigInteger[] P) {

        BigInteger lambda = (P[0].pow(2).multiply(new BigInteger("3")).add(a)).multiply((P[1].multiply(new BigInteger("2"))).modInverse(p));


        BigInteger xR = lambda.pow(2).subtract(P[0].multiply(new BigInteger("2"))).mod(p);
        BigInteger yR = lambda.multiply(P[0].subtract(xR)).subtract(P[1]).mod(p);

        return new BigInteger[]{xR, yR};
    }




}

