package com.lz.blockchainauthentication.controller;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.result.ResponseResult;
import com.lz.blockchainauthentication.service.MNService;
import com.lz.blockchainauthentication.util.ECCUtil;
import com.lz.blockchainauthentication.vc.AnonymousVC;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MNController {

    @Resource
    private MNService mnService;

    @PostMapping("/dealData")
    public String dealData(@RequestBody JSONObject jsonObject){

        String dataReq = jsonObject.getString("dataReq");
        Integer buildingNum = jsonObject.getInteger("buildingNum");
        System.out.println("-".repeat(50));

        if(mnService.handleDeviceData(dataReq, buildingNum)){
            return "ACK";
        }else{
            return "NACK";
        }
    }

    @PostMapping("/issueAnonymousVC")
    public String issueAnonymousVC(@RequestBody JSONObject jsonObject) {

        String message = jsonObject.getString("message");
        return mnService.issueAnonymousVC(new Message(message));


    }
}
