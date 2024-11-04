package com.lz.blockchainauthentication.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class BlockchainUtil {

    private static final String host = "http://localhost:7080/";


    public static Boolean uploadDeviceInfo(String DDID, String pk){
        JSONObject json = new JSONObject();
        json.put("DDID", DDID);
        json.put("pk", pk);
        String resStr = RestTemplateUtil.post(host +"consortium/"+ "uploadDeviceInfo", json);
        JSONObject res = JSONObject.parseObject(resStr);
        return res.getBoolean("result");
    }
    public static String queryFromDIT(String DDID){
        JSONObject json = new JSONObject();
        json.put("DDID", DDID);
        String resStr = RestTemplateUtil.post(host +"consortium/"+ "queryFromDIT", json);
        JSONObject res = JSONObject.parseObject(resStr);
        return res.getString("result");
    }
    public static boolean uploadRealUserInfo(String MDID, String pk, String merkleRoot){
        JSONObject json = new JSONObject();
        json.put("MDID", MDID);
        json.put("pk", pk);
        json.put("merkleRoot", merkleRoot);
        String resStr = RestTemplateUtil.post(host +"private/"+ "uploadRealUserInfo", json);
        JSONObject res = JSONObject.parseObject(resStr);
        return res.getBoolean("result");
    }
    public static String[] queryFromRUIT(String MDID){
        JSONObject json = new JSONObject();
        json.put("MDID", MDID);
        String resStr = RestTemplateUtil.post(host +"private/"+ " queryFromRUIT", json);
        JSONObject res = JSONObject.parseObject(resStr);
        JSONArray jsonArray = res.getJSONArray("result");
        return jsonArray.toArray(new String[jsonArray.size()]);
    }

    public static Boolean  uploadAnonUserInfo(String ADID, String pk, String hM){
        JSONObject json = new JSONObject();
        json.put("ADID", ADID);
        json.put("pk", pk);
        json.put("hM", hM);
        String resStr = RestTemplateUtil.post(host +"consortium/"+ "uploadAnonUserInfo", json);
        JSONObject res = JSONObject.parseObject(resStr);
        return res.getBoolean("result");
    }
    public static String[] queryFromAUIT(String ADID){
        JSONObject json = new JSONObject();
        json.put("ADID", ADID);
        String resStr = RestTemplateUtil.post(host +"consortium/"+ " queryFromAUIT", json);
        JSONObject res = JSONObject.parseObject(resStr);
        JSONArray jsonArray = res.getJSONArray("result");
        return jsonArray.toArray(new String[jsonArray.size()]);
    }




}
