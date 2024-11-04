package com.lz.blockchainauthentication.POJO;

import com.lz.blockchainauthentication.vc.DeviceVC;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    private String id;
    private BigInteger privateKey;
    private String publicKey;
    int buildingNum;
    int registered;
    DeviceVC deviceVC;

    @Override
    public String toString() {
        return "Device{" +
                "ID='" + id + '\'' +
                ", privateKey=" + privateKey +
                ", publicKey=" + publicKey +
                '}';
    }
}



