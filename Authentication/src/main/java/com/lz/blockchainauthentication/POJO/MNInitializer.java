package com.lz.blockchainauthentication.POJO;

import com.lz.blockchainauthentication.util.ECCUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@Component
public class MNInitializer {



    private final Map<Integer, MN> mnMap = new HashMap<>();

    @PostConstruct
    public void initMNs() {
        for (int buildingNum = 1; buildingNum <= 4; buildingNum++) {
            KeyPair keyPair = ECCUtil.generateSame((byte) buildingNum);
            MN mn = new MN(keyPair, buildingNum);
            mnMap.put(buildingNum, mn);
        }

        KeyPair keyPair = ECCUtil.generateSame((byte) -1);
        MN mn = new MN(keyPair, -1);
        mnMap.put(-1, mn);


    }


    public Map<Integer, MN> getMnMap() {
        return mnMap;
    }
}
