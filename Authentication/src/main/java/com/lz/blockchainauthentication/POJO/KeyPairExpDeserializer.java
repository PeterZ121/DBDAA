package com.lz.blockchainauthentication.POJO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.lz.blockchainauthentication.util.ECCUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyPairExpDeserializer extends StdDeserializer<KeyPairExp> {

    public KeyPairExpDeserializer() {
        this(null);
    }

    public KeyPairExpDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public KeyPairExp deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String publicKeyStr = node.get("publicKey").asText();
        BigInteger privateKeyInt = node.get("privateKey").bigIntegerValue();

        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // Reconstructing KeyPair object
            KeyPair keyPair = new KeyPair(publicKey, ECCUtil.strToPrivateKey(privateKeyInt));

            return new KeyPairExp(keyPair);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize KeyPairExp", e);
        }
    }
}

