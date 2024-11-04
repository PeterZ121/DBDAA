package com.lz.blockchainauthentication.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.*;
import com.lz.blockchainauthentication.mapper.DeviceMapper;
import com.lz.blockchainauthentication.service.DeviceService;
import com.lz.blockchainauthentication.util.BlockchainUtil;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.vc.AnonymousVC;
import com.lz.blockchainauthentication.vc.DeviceVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

@Service
public class DeviceServiceImpl implements DeviceService {

   @Autowired
   ESP esp;

   @Autowired
    DeviceMapper deviceMapper;

   @Autowired
   MNInitializer mnInitializer;

   @Override
   public Device initDevice(){


      System.out.println("-".repeat(50));

      Device device = new Device();

      String id = IdUtil.getSnowflake(1, 1).nextIdStr();
      device.setId(id);
      System.out.println("ID:"+ id);





      KeyPair keyPair = ECCUtil.generate();




      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      String publicKeyValue = ECCUtil.publicKeyToStr(publicKey);
      BigInteger privateKeyValue = ECCUtil.privateKeyToInt(privateKey);
      device.setPrivateKey(privateKeyValue);
      device.setPublicKey(publicKeyValue);
      System.out.println("publicKey:"+publicKeyValue);
      System.out.println("privateKey:"+privateKeyValue);

      deviceMapper.insertDevice(id, privateKeyValue, publicKeyValue);
      return device;
   }

   @Override
   public Device selectDevice(String id) {
      Device device = deviceMapper.selectDeviceById(id);
      return device;
   }

   @Override
   public String requestForRegistration(Device device){
      JSONObject requestJSON = new JSONObject();
      requestJSON.put("ID", device.getId());
      requestJSON.put("pk", device.getPublicKey());
      requestJSON.put("t",System.currentTimeMillis());
      String plainText = requestJSON.toJSONString();
      return ECCUtil.encrypt(plainText,esp.getKeyPair().getPublic());
   }


   @Override
   public boolean storeDeviceVC(String regRes, Device device){
      //设备私钥解密消息
      PrivateKey privateKey;
      try{
         privateKey = ECCUtil.strToPrivateKey(device.getPrivateKey());
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
      String regResMsg = ECCUtil.decrypt(regRes, privateKey);

      DeviceVC deviceVC = new DeviceVC(regResMsg);
      try{

         ECCUtil.verifySignature(deviceVC.getDDID()+deviceVC.getTimestamp(), deviceVC.getSignature(), esp.getKeyPair().getPublic());
      }catch (Exception e){
         System.out.println("false");
         e.printStackTrace();
         return false;
      }
      System.out.println("store deviceVC");
      device.setDeviceVC(deviceVC);

      deviceMapper.updateById(device.getId(), device.getBuildingNum(), deviceVC.getDDID());
      return true;
   }

   public String[] sendData(String data, String deviceVCStr){

      DeviceVC deviceVC = new DeviceVC(deviceVCStr);
      Device device = deviceMapper.selectDeviceByDDID(deviceVC.getDDID());

      JSONObject dataJson = new JSONObject();
      dataJson.put("data", data);
      dataJson.put("deviceVC", deviceVCStr);

      String signature = ECCUtil.signMessage(dataJson.toJSONString(), ECCUtil.strToPrivateKey(device.getPrivateKey()));

      Message message = new Message(dataJson, signature);

      MN mn = mnInitializer.getMnMap().get(device.getBuildingNum());

      String encrypt = ECCUtil.encrypt(message.toString(), mn.getKeyPair().getPublic());
      String[] results = new String[2];
      results[0] = encrypt;
      results[1] = String.valueOf(device.getBuildingNum());
      return results;


   }

   @Override
   public boolean isServe(Message message) {

      JSONObject vcJson = message.getMsgJson();
      String signature = message.getSignature();
      String adid = vcJson.getString("ADID");



      String[] anonInfo = BlockchainUtil.queryFromAUIT(adid);

      String pk2 = anonInfo[0];

      if(!ECCUtil.verifySignature(vcJson.toJSONString(), signature, ECCUtil.strToPublicKey(pk2))){
         return false;
      }



      boolean res = ECCUtil.verifySignature(vcJson.toJSONString(), vcJson.getString("signature"), mnInitializer.getMnMap().get(-1).getKeyPair().getPublic());

      return res;

   }
}
