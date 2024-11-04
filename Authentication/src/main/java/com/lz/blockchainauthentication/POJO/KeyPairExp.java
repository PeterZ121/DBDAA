package com.lz.blockchainauthentication.POJO;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lz.blockchainauthentication.util.ECCUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigInteger;
import java.security.KeyPair;

@Data
@ToString
@NoArgsConstructor
public class KeyPairExp {
    private String publicKey;
    private BigInteger privateKey;
    @JsonIgnore
    private KeyPair keyPair;


    public KeyPairExp(KeyPair keyPair) {
        this.keyPair = keyPair;
        this.publicKey = ECCUtil.publicKeyToStr(keyPair.getPublic());
        this.privateKey = ECCUtil.privateKeyToInt(keyPair.getPrivate());
    }

    public KeyPairExp(String publicKey, BigInteger privateKey, KeyPair keyPair) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        if(keyPair == null){
            keyPair = new KeyPair(ECCUtil.strToPublicKey(publicKey), ECCUtil.strToPrivateKey(privateKey));
        }
        this.keyPair = keyPair;
    }

    @JsonCreator
    public static KeyPairExp createFromJson(@JsonProperty("publicKey") String publicKey,
                                            @JsonProperty("privateKey") BigInteger privateKey) {
        if(privateKey == null || privateKey == null){
            return null;
        }
        return new KeyPairExp(publicKey, privateKey, null);
    }
}
