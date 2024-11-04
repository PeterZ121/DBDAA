package com.lz.blockchainauthentication.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class DeviceVC {
    String DDID;
    Long timestamp;
    String signature;


    public DeviceVC(String str) {
        String trimmedStr = str.substring(str.indexOf("(") + 1, str.length() - 1);
        String[] parts = trimmedStr.split(", ", -1);

        this.DDID = parts[0].split("=")[1].trim();
        this.timestamp = Long.parseLong(parts[1].split("=")[1].trim());

        this.signature = parts[2].split("=")[1].trim().replaceAll(" ", "+");

    }

}
