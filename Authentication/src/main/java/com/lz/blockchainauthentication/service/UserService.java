package com.lz.blockchainauthentication.service;

import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.POJO.User;
import com.lz.blockchainauthentication.vc.PreVC;

public interface UserService {
    User storePreVC(PreVC preVC, String uid);
    boolean hasAuthority(PreVC preVC, String ddid);
    Message requestForADID(String ddid, User user);
    Message requestForAnonymousVC(String adid, String ddid, User user);
}
