package com.lz.blockchainauthentication.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.KeyPairExp;
import com.lz.blockchainauthentication.POJO.MNInitializer;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.POJO.User;
import com.lz.blockchainauthentication.result.ResponseResult;
import com.lz.blockchainauthentication.service.CacheService;
import com.lz.blockchainauthentication.service.ESPService;
import com.lz.blockchainauthentication.service.MNService;
import com.lz.blockchainauthentication.service.UserService;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.util.HashUtil;
import com.lz.blockchainauthentication.util.RestTemplateUtil;
import com.lz.blockchainauthentication.vc.AnonymousVC;
import com.lz.blockchainauthentication.vc.PreVC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class UserController {

    private static final String host = "http://localhost:9002/";

    @Autowired
    ESPService espService;

    @Autowired
    UserService userService;

    @Autowired
    MNInitializer mnInitializer;


    @Autowired
    MNService mnService;

    @Autowired
    CacheService cacheService;

    public static Map<String,ArrayList<BigInteger>> deceivesForUser = new HashMap<>();
    public static Map<String,BigInteger> hashResForUser = new HashMap<>();



    @PostMapping("/userRegistration")
    public ResponseResult userRegistration(@RequestParam("UID") String uid){

        HashMap<String, Object> reqMap = new HashMap<>();
        reqMap.put("uid", uid);
        String preVCStr = RestTemplateUtil.post(host+"createPreVC", reqMap);

        if(preVCStr == null){
            return new ResponseResult(200, "preVC fail");
        }
        PreVC preVC = new PreVC(preVCStr);

        System.out.println("-".repeat(50));
        User user = userService.storePreVC(preVC, uid);


        HashMap<String, Object> ackMap = new HashMap<>();
        ackMap.put("mdid", user.getPreVC().getMDID());
        ackMap.put("pk", user.getFirstKeyPairExp().getPublicKey());
        String regRes = RestTemplateUtil.post(host+"uploadReal", ackMap);

        return new ResponseResult(200, "success", user);
    }

    @PostMapping("/anonVCIssue")
    public ResponseResult anonymousAuthentication (@RequestParam("DDID") String ddid,
                                             @RequestBody User user)throws Exception{
//        PreVC preVC = new PreVC(preVCStr);
        System.out.println(user);
        System.out.println(ddid);
        PreVC preVC = user.getPreVC();



        System.out.println("-".repeat(50));

        if(!userService.hasAuthority(preVC, ddid)) {
            return new ResponseResult<>(403, "do not have permission");
        }

        System.out.println("-".repeat(50));




        Message message = userService.requestForADID(ddid, user);


        System.out.println("-".repeat(50));


        HashMap<String, Object> adidReq = new HashMap<>();
        adidReq.put("message", message);
        String adid = RestTemplateUtil.post(host+"dealAdidReq", adidReq);


        if(adid == ""){
            System.out.println("ADID fail");;
        }else{
            System.out.println("ADID success");;
        }

        System.out.println("-".repeat(50));

        Message message2 = userService.requestForAnonymousVC(adid, ddid, user);
        System.out.println("-".repeat(50));

        HashMap<String, Object> vcReq = new HashMap<>();
        vcReq.put("message", message2);
        String encrypt = RestTemplateUtil.post(host+"issueAnonymousVC", vcReq);

        String decrypt = ECCUtil.decrypt(encrypt, user.getSecondKeyPairExps().get(adid).getKeyPair().getPrivate());
        JSONObject res = JSONObject.parseObject(decrypt);
        String anonymousVCStr = res.getString("vc");
        String sig = res.getString("sig");
        if(!ECCUtil.verifySignature(anonymousVCStr, sig, mnInitializer.getMnMap().get(-1).getKeyPair().getPublic())){
            return new ResponseResult<>(200, "fail");
            }
        cacheService.cacheValue(adid, String.valueOf(ECCUtil.privateKeyToInt(user.getSecondKeyPairExps().get(adid).getKeyPair().getPrivate())));
        return new ResponseResult<>(200, "success", new AnonymousVC(anonymousVCStr));


    }

    @PostMapping("/anonymousAuth")
    public ResponseResult anonymousAuth(@RequestBody AnonymousVC anonymousVC){

        PrivateKey sk2 = ECCUtil.strToPrivateKey(new BigInteger(cacheService.getCachedValue(anonymousVC.getADID())));
        JSONObject anonVCJSON = JSON.parseObject(JSON.toJSONString(anonymousVC));
        Message message = new Message(anonVCJSON, ECCUtil.signMessage(anonVCJSON.toJSONString(), sk2));

        HashMap<String, Object> serviceReq = new HashMap<>();
        serviceReq.put("message", message);
        String res = RestTemplateUtil.post(host+"getService", serviceReq);
        return new ResponseResult<>(200, "success", res);

    }



    @PostMapping("/test")
    public ResponseResult test(@RequestParam("DDID") String ddid,
                               @RequestBody KeyPairExp keyPairExp){
        System.out.println(keyPairExp);
        return null;
    }

    @PostMapping("/backup")
    public ResponseResult backup(@RequestBody User user){
        int n = 50;

        System.out.println("-".repeat(50));

        ArrayList<BigInteger> randomCodes = generateRandomCodes(n);
        System.out.println("Codes："+randomCodes);


        int randomIndex = getRandomIndex(randomCodes.size());
        BigInteger code = randomCodes.get(randomIndex);
        System.out.println(code);

        int x = Math.abs(new Random().nextInt(100000)+1);
        BigInteger hashRes = code;
        for(int i = 0;i < x;i++){
            hashRes = HashUtil.uHash(hashRes);
        }
        System.out.println(x+"hash result:"+hashRes);

        BigInteger sk = user.getFirstKeyPairExp().getPrivateKey();
        BigInteger rk = sk.xor(hashRes);


        ArrayList<BigInteger> deceives = generateRandomCodes(n - 1);
        int randomIndex1 = getRandomIndex(deceives.size());
        deceives.add(randomIndex1, rk);
        System.out.println("Deceives："+deceives);
        JSONObject res = new JSONObject();
        deceivesForUser.put(user.getUID(), deceives);
        hashResForUser.put(user.getUID(), code);
        res.put("deceives", deceives);
        res.put("code", code);
        return new ResponseResult<>(200, "success", res);


    }

    @PostMapping("/recover")
    public ResponseResult recover(@RequestBody User user){

        System.out.println("-".repeat(50));

        String merkleRoot = user.getPreVC().getMerkleRoot();

        String ct = ECCUtil.encrypt(merkleRoot, user.getFirstKeyPairExp().getKeyPair().getPublic());


        BigInteger rk = null;
        ArrayList<BigInteger> deceives = deceivesForUser.get(user.getUID());
        BigInteger hashRes = hashResForUser.get(user.getUID());
        int randomIndex = getRandomIndex(deceives.size());
        int i = 0;
        for(BigInteger deceive : deceives){


            boolean res = i == randomIndex;

            i++;
        }
        BigInteger sk = new BigInteger("0");

        if(rk != null){


            sk = rk.xor(hashRes);
        }

    return new ResponseResult<>(200,user.getFirstKeyPairExp().getPrivateKey());

    }


    private static int getRandomIndex(int size) {
        Random random = new Random();
        return Math.abs(random.nextInt(size));
    }

    private static ArrayList<BigInteger> generateRandomCodes(int n) {

        Random random = new Random();
        ArrayList<BigInteger> codes = new ArrayList<>();


        for (int i = 0; i < n; i++) {

            BigInteger randomBigInteger = new BigInteger(256, random);
            codes.add(randomBigInteger);

        }

        return codes;
    }

}
