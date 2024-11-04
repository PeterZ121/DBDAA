package com.lz.blockchainauthentication.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.ESP;
import com.lz.blockchainauthentication.POJO.MerkleTreeGenerator;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.POJO.User;
import com.lz.blockchainauthentication.mapper.DeviceMapper;
import com.lz.blockchainauthentication.mapper.UserMapper;
import com.lz.blockchainauthentication.service.CacheService;
import com.lz.blockchainauthentication.service.ESPService;
import com.lz.blockchainauthentication.util.BlockchainUtil;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.util.HashUtil;
import com.lz.blockchainauthentication.vc.DeviceVC;
import com.lz.blockchainauthentication.vc.PreVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
@Service
public class ESPServiceImpl implements ESPService {

    @Autowired
    ESP esp;

    @Autowired
    private CacheService cacheService;


    @Autowired
    DeviceMapper deviceMapper;

    @Autowired
    UserMapper userMapper;

    static Long validDuration = 1000000000l;



    @Override
    public String registrateForDevice(String requestMsg) throws Exception{

        String decryptMsg = ECCUtil.decrypt(requestMsg, esp.getKeyPair().getPrivate());
        JSONObject requestJson = JSONObject.parseObject(decryptMsg);


        Long timestamp = requestJson.getLong("timestamp");
        long timeDifference = System.currentTimeMillis() - timestamp;
        if (timeDifference < 0 && timeDifference > validDuration) {
            return "time fail";
        }

        String deviceId = requestJson.getString("ID");
        String devicePkStr = requestJson.getString("pk");
        String DDID = HashUtil.hash2(deviceId);

        long t = System.currentTimeMillis();

        System.out.println("signature:"+ECCUtil.privateKeyToInt(esp.getKeyPair().getPrivate()));
        String signature = ECCUtil.signMessage(DDID + t, esp.getKeyPair().getPrivate());
        DeviceVC deviceVC = new DeviceVC(DDID, t, signature);

        BlockchainUtil.uploadDeviceInfo(DDID, devicePkStr);

        PublicKey devicePk = ECCUtil.strToPublicKey(devicePkStr);
        String encrypt = ECCUtil.encrypt(deviceVC.toString(), devicePk);
        return encrypt;


    }


    @Override
    public PreVC registrateForUser(String uid) {


        String MDID = HashUtil.hash0(uid);


        int i = userMapper.selectCountByMDID(MDID);

        if(i>0){
            return null;
        }


        List<String> ddids = null;
        if(uid.startsWith("0")){
            ddids = deviceMapper.selectDDIDByNotLimited();
        }else{
            return null;
        }

        MerkleTreeGenerator merkleTreeGenerator = new MerkleTreeGenerator(ddids);
        String merkleRoot = merkleTreeGenerator.generateMerkleRoot();
        cacheService.cacheValue(MDID, merkleRoot);

        long seed = merkleTreeGenerator.getSeed();


        Pair<String, BigInteger> tok = genarateTok(MDID);
        ArrayList<String> tok1 = new ArrayList<>();
        tok1.add(tok.getLeft());
        tok1.add(tok.getRight().toString());
        long t = System.currentTimeMillis();



        String signature = ECCUtil.signMessage(MDID + ddids + seed + merkleRoot + t + tok1, esp.getKeyPair().getPrivate());

        PreVC preVC = new PreVC(MDID,
                ddids,
                seed,
                merkleRoot,
                tok1,
                t,
                signature);
        System.out.println("preVC:"+preVC);


        BlockchainUtil.uploadRealUserInfo(MDID,pk,merkleRoot);

        return preVC;
    }

    private Pair<String, BigInteger> genarateTok(String mdid){
        KeyPair keyPair = ECCUtil.generate();
        return Pair.of(ECCUtil.publicKeyToStr(keyPair.getPublic()).split(",")[0],
                ECCUtil.privateKeyToInt(keyPair.getPrivate()));
    }

    public boolean uploadRealUserInform(String mdid, String pk){


        String merkleRoot = cacheService.getCachedValue(mdid);
        if (merkleRoot != null) {
            cacheService.clearCache(mdid);
        }

        BlockchainUtil.uploadRealUserInfo(mdid, pk, merkleRoot);

        if(userMapper.insertRealUser(mdid, pk, merkleRoot)!=0){
            return true;
        }else{
            return false;
        }


    }

    public String issueADID(Message message){
        // 解密
        // 验证签名
        if(!ECCUtil.verifySignature(message.toString(), message.getSignature(), esp.getKeyPair().getPublic())){
            return "";
        }
        JSONObject json = message.getMsgJson();
        String mdid = json.getString("MDID");
        String secondPk = json.getString("secondPk");
        // 生成ADID
        String temp1 = String.valueOf(ECCUtil.privateKeyToInt(esp.getKeyPair().getPrivate()));
        String temp2 = String.valueOf(System.currentTimeMillis());//1711365657569
        byte[] mdidBytes = mdid.getBytes(StandardCharsets.UTF_8);
        String hash1 = HashUtil.hash1(temp1);
        String hash2 = HashUtil.hash1(temp2);
        byte[] tempBytes = (hash1 + hash2).getBytes(StandardCharsets.UTF_8);
        // 确保tempBytes长度至少与mdidBytes一样长，避免ArrayIndexOutOfBoundsException
        byte[] adidBytes = new byte[mdidBytes.length];
        for (int i = 0; i < mdidBytes.length; i++) {
            adidBytes[i] = (byte) (mdidBytes[i] ^ tempBytes[i % tempBytes.length]); // 注意使用 % 操作保证索引在tempBytes范围内
        }
        // 将异或结果的字节数组转换为十六进制字符串
        String adid = bytesToHex(adidBytes);//0001060507070903070b
        System.out.println("生成ADID:"+adid);

        //查看匿名用户表中是否已经有相同的ADID
        if(userMapper.selectCountByADID(adid) > 0){
            return null;
        }


        //生成hM，从区块链查询merkleRoot
        String merkleRoot = userMapper.selectMerkleRootByMDID(mdid);
        String hM = HashUtil.hash3(adid + merkleRoot);
        System.out.println("生成hM:"+hM);


        userMapper.insertAnonymousUser(adid, secondPk, hM);

        // todo：上传联盟区块链
        BlockchainUtil.uploadAnonUserInfo(adid,secondPk,hM);


        return adid;


    }

    // 辅助方法：将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}

