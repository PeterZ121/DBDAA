package com.lz.blockchainauthentication.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.KeyPairExp;
import com.lz.blockchainauthentication.POJO.MerkleTreeGenerator;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.POJO.User;
import com.lz.blockchainauthentication.service.UserService;
import com.lz.blockchainauthentication.util.BlockchainUtil;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.vc.PreVC;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.HashMap;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User storePreVC(PreVC preVC, String uid) {

        User user = new User();
        user.setPreVC(preVC);
        user.setFirstKeyPairExp(new KeyPairExp(ECCUtil.generate()));
        user.setUID(uid);
        return user;
    }

    @Override
    public boolean hasAuthority(PreVC preVC, String ddid) {
        return preVC.getAccessList().contains(ddid);
    }

    @Override
    public Message requestForADID(String ddid, User user) {


        String[] realInfo = BlockchainUtil.queryFromRUIT(user.getPreVC().getMDID());
        String userPkStr = realInfo[0];
        String merkleRoot = realInfo[1];



        KeyPairExp secondKeyPairExp = new KeyPairExp(ECCUtil.generate());
        System.out.println("privateKey:"+secondKeyPairExp.getPrivateKey());
        System.out.println("publicKey:"+secondKeyPairExp.getPublicKey());
        if(user.getSecondKeyPairExps() == null){

            user.setSecondKeyPairExps(new HashMap<>());
        }
        user.getSecondKeyPairExps().put(ddid, secondKeyPairExp);

        JSONObject json = new JSONObject();
        json.put("MDID", user.getPreVC().getMDID());
        json.put("secondPk", secondKeyPairExp.getPublicKey());
        long t = System.currentTimeMillis();
        json.put("t", t);

        String signature = ECCUtil.signMessage(json.toJSONString(), user.getFirstKeyPairExp().getKeyPair().getPrivate());


        Message message = new Message(json, signature);

        return message;
    }
    @Override
    public Message requestForAnonymousVC(String adid, String ddid, User user) {


        JSONObject json = new JSONObject();
        json.put("ADID", adid);
        json.put("DDID", ddid);
        json.put("index", user.getPreVC().getAccessList().indexOf(ddid));
        json.put("merkleRoot", user.getPreVC().getMerkleRoot());
        long t = System.currentTimeMillis();
        json.put("t", t);

        String signature = ECCUtil.signMessage(json.toJSONString(),
                user.getSecondKeyPairExps().get(ddid).getKeyPair().getPrivate());


        Message message = new Message(json, signature);
        message.setMsgJson(json);

        return message;
    }
}
