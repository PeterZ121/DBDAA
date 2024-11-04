package com.lz.blockchainauthentication.service;

import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.vc.AnonymousVC;

public interface MNService {
    boolean handleDeviceData(String dataReq, int buildingNum);
    String issueAnonymousVC(Message message);
}
