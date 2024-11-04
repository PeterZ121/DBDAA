package com.lz.blockchainauthentication.service;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.POJO.User;
import com.lz.blockchainauthentication.vc.PreVC;

public interface ESPService {

    String registrateForDevice(String requestMsg) throws Exception;

    PreVC registrateForUser(String uid);

    boolean uploadRealUserInform(String mdid, String pk);

    String issueADID(Message message);
}
