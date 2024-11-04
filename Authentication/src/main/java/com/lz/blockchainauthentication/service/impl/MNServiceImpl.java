package com.lz.blockchainauthentication.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.*;
import com.lz.blockchainauthentication.mapper.UserMapper;
import com.lz.blockchainauthentication.service.MNService;
import com.lz.blockchainauthentication.util.BlockchainUtil;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.util.HashUtil;
import com.lz.blockchainauthentication.util.ShamirUtil;
import com.lz.blockchainauthentication.vc.AnonymousVC;
import com.lz.blockchainauthentication.vc.DeviceVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Block;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class MNServiceImpl implements MNService {

    @Autowired
    MNInitializer mnInitializer;

    @Autowired
    UserMapper userMapper;

    @Autowired
    ESP esp;


    @Override
    public boolean handleDeviceData(String dataReq,  int buildingNum) {



        MN mn = mnInitializer.getMnMap().get(buildingNum);


        String decrypt = ECCUtil.decrypt(dataReq, mn.getKeyPair().getPrivate());
        Message message = new Message(decrypt);
        DeviceVC deviceVC = new DeviceVC(message.getMsgJson().getString("deviceVC"));



        String devicePkStr = BlockchainUtil.queryFromDIT(deviceVC.getDDID());


        if(!ECCUtil.verifySignature(message.getMsgJson().getString("deviceVC"),message.getSignature(), ECCUtil.strToPublicKey(devicePkStr))){
            return false;
        }

        System.out.println(ECCUtil.publicKeyToStr(esp.getKeyPair().getPublic()));
        boolean res = ECCUtil.verifySignature(deviceVC.getDDID() + deviceVC.getTimestamp(), deviceVC.getSignature(), esp.getKeyPair().getPublic());

        if(!res){
            return false;
        }

        System.out.println("MN"+buildingNum+"dealData:"+message.getMsgJson().getString("data"));
        return true;


    }

    @Override
    public String issueAnonymousVC(Message message) {

        JSONObject dataJson = message.getMsgJson();
        String adid = dataJson.getString("ADID");
        String ddid = dataJson.getString("DDID");
        String merkleRoot= dataJson.getString("merkleRoot");
        JSONObject vcJson = new JSONObject();
        vcJson.put("ADID", adid);
        vcJson.put("DDID", ddid);

        MN creMn = mnInitializer.getMnMap().get(-1);



        String[] anonInfo = BlockchainUtil.queryFromAUIT(adid);
        String pk2 = anonInfo[0];
        String hM = anonInfo[1];

        if(!ECCUtil.verifyhM(hM, adid, merkleRoot)){
            return "";
        }
        byte[][] signatures = new byte[4][];

        for(int i = 1; i <= 4; i++){
            MN mn = mnInitializer.getMnMap().get(i);
            try {
                if(i == 3){
                    signatures[i-1] = partialSign(message, mn);

                }else{
                    signatures[i-1] = partialSign(message, mn);
                    System.out.println(Base64.getEncoder().encodeToString(signatures[i-1]));
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        String aggregatedSignature = null;

        try {
            aggregatedSignature =  Base64.getEncoder().encodeToString(ShamirUtil.aggregateSignatures(signatures));
            aggregatedSignature =  ECCUtil.signMessage(vcJson.toJSONString(), creMn.getKeyPair().getPrivate());
            System.out.println(aggregatedSignature);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AnonymousVC anonymousVC = new AnonymousVC(adid, ddid, aggregatedSignature);
        String signature = ECCUtil.signMessage(anonymousVC.toString(), creMn.getKeyPair().getPrivate());
        JSONObject res = new JSONObject();
        res.put("vc", anonymousVC.toString());
        res.put("sig", signature);
        String str = res.toJSONString();
        String encrypt = ECCUtil.encrypt(str, ECCUtil.strToPublicKey(pk2));
        return encrypt;


    }

    private byte[] partialSign(Message message, MN mn) throws Exception{

        JSONObject dataJson = message.getMsgJson();
        String adid = dataJson.getString("ADID");
        String ddid = dataJson.getString("DDID");
        String merkleRoot = dataJson.getString("MerkleRoot");

        String hM1 = HashUtil.hash3(adid + merkleRoot);


        JSONObject vcJson = new JSONObject();
        vcJson.put("ADID", adid);
        vcJson.put("DDID", ddid);


        byte[] data = vcJson.toJSONString().getBytes();


        return ShamirUtil.partialSign(data,mn.getKeyPair(), 4, 2)[0];




    }




}
