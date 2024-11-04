package com.lz.blockchainauthentication.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
@ToString
public class PreVC {
    String MDID;
    List<String> accessList;
    long seed;
    String merkleRoot;
    ArrayList<String> tok;
    long timestamp;
    String signature;


    public PreVC(String str) {


        str = str.replace("PreVC(", "").replace(")", "").trim();


        String[] parts = str.split(", (?=[a-zA-Z]+)");

        String MDID = null;
        List<String> accessList = null;
        long seed = 0;
        String merkleRoot = null;
        ArrayList<String> tok = null;
        long timestamp = 0;
        String signature = null;

        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            switch (key) {
                case "MDID":
                    this.MDID = value;
                    break;
                case "accessList":

                    value = value.replace("[", "").replace("]", "");
                    this.accessList = Arrays.asList(value.split(", "));
                    break;
                case "seed":
                    this.seed = Long.parseLong(value);
                    break;
                case "merkleRoot":
                    this.merkleRoot = value;
                    break;
                case "tok":

                    value = value.replace("[", "").replace("]", "");
                    this.tok = new ArrayList<>(Arrays.asList(value.split(", ")));
                    break;
                case "timestamp":
                    this.timestamp = Long.parseLong(value);
                    break;
                case "signature":
                    this.signature = value;
                    break;
            }
        }
    }


}
