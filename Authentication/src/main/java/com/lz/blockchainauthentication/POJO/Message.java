package com.lz.blockchainauthentication.POJO;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    JSONObject msgJson;
    String signature;

    @Override
    public String toString() {
        return "Message{" +
                "msgJson=" + msgJson.toJSONString() +
                ", signature='" + signature + '\'' +
                '}';
    }

    public Message(String str) {

        Pattern pattern = Pattern.compile("msgJson=(\\{.*\\}), signature='([^']*)'");
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            this.msgJson = JSONObject.parseObject(matcher.group(1).trim());
            this.signature = matcher.group(2).trim();


        }


    }
}
