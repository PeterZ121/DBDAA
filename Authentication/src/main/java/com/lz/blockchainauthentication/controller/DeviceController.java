package com.lz.blockchainauthentication.controller;

import com.alibaba.fastjson.JSONObject;
import com.lz.blockchainauthentication.POJO.Device;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.mapper.DeviceMapper;
import com.lz.blockchainauthentication.result.ResponseResult;
import com.lz.blockchainauthentication.service.DeviceService;
import com.lz.blockchainauthentication.service.ESPService;
import com.lz.blockchainauthentication.service.MNService;
import com.lz.blockchainauthentication.util.RestTemplateUtil;
import com.lz.blockchainauthentication.vc.AnonymousVC;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;


@RestController
public class DeviceController {

    private static final String host = "http://localhost:9002/";

    @Resource
    private DeviceService deviceService;

    @Resource
    private ESPService espService;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private MNService MNService;



    @GetMapping("/getDevice")
    public ResponseResult getDevice(){
        Device device = deviceService.initDevice();
        return new ResponseResult<>(200, device.toString());
    }

    @PostMapping("/deviceRegistration")
    public ResponseResult deviceRegistration(@RequestParam("buildingNum") int buildingNum,
                                             @RequestParam("id") String deviceId){

        Device device = deviceService.selectDevice(deviceId);
        if(device.getRegistered()==1){
            return new ResponseResult<>(200, "The equipment is registered");
        }
        device.setBuildingNum(buildingNum);


        System.out.println("-".repeat(50));

        String regReq = deviceService.requestForRegistration(device);


        HashMap<String, Object> regRegMap = new HashMap<>();
        regRegMap.put("regReq", regReq);
        String regRes = RestTemplateUtil.post(host+"dealDeviceRegistration", regRegMap);


        System.out.println("-".repeat(50));
        if(deviceService.storeDeviceVC(regRes, device)){
            return new ResponseResult<>(200, device.getDeviceVC().toString());
        }else {
            return new ResponseResult<>(500, "");
        }


    }



    @PostMapping("/deviceSenbData")
    public ResponseResult deviceSendData(@RequestParam("data") String data,
                                         @RequestParam("deviceVC") String deviceVC){


        System.out.println("-".repeat(50));
        String[] results = deviceService.sendData(data, deviceVC);
        String dataReq = results[0];
        int buildingNum = Integer.parseInt(results[1]);


        HashMap<String, Object> reqMap = new HashMap<>();
        reqMap.put("dataReq", dataReq);
        reqMap.put("buildingNum", buildingNum);
        String regRes = RestTemplateUtil.post(host+"dealData", reqMap);


        if(regRes.equals("ACK")){
            return new ResponseResult<>(200, "success");
        }else{
            return new ResponseResult<>(200, "false");
        }

    }

    @PostMapping("/getService")
    public String getService(@RequestBody JSONObject jsonObject){
        //设备检查是否提供服务
        System.out.println("-".repeat(50));
        String message = jsonObject.getString("message");
        if(deviceService.isServe(new Message(message))){
            return "ACK";
        }else{
            return "NACK";
        }
    }

    @GetMapping("/getAllDevice")
    public ResponseResult getAll(){
        return new ResponseResult<>(200, deviceMapper.selectAllDevice());
    }


}

