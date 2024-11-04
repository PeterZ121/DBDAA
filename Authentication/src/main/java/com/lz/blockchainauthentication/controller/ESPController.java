package com.lz.blockchainauthentication.controller;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.result.ResponseResult;
import com.lz.blockchainauthentication.service.ESPService;
import com.lz.blockchainauthentication.vc.PreVC;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class ESPController {

    @Resource
    private ESPService espService;

    @PostMapping("/dealDeviceRegistration")
    public String dealDeviceRegistration(@RequestBody JSONObject jsonObject){

        String regReq = jsonObject.getString("regReq");
        System.out.println("-".repeat(50));

        String regRes = "";
        try{
            regRes = espService.registrateForDevice(regReq);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return regRes;
        }

    }

    @PostMapping("/createPreVC")
    public String createPreVC(@RequestBody JSONObject jsonObject){
        String uid = jsonObject.getString("uid");
        System.out.println("-".repeat(50));

        PreVC preVC = espService.registrateForUser(uid);
        if(preVC==null){
            return "";
        }else{
            return preVC.toString();
        }
    }

    @PostMapping("/uploadReal")
    public String uploadReal(@RequestBody JSONObject jsonObject) {
        String mdid = jsonObject.getString("mdid");
        String pk = jsonObject.getString("pk");
        espService.uploadRealUserInform(mdid, pk);
        return "ACK";
    }

    @PostMapping("/dealAdidReq")
    public String dealAdidReq(@RequestBody JSONObject jsonObject){
        String message = jsonObject.getString("message");
        String adid = espService.issueADID(new Message(message));
        return adid;

    }




}
