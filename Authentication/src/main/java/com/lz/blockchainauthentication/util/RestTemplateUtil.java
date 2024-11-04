package com.lz.blockchainauthentication.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;


@Slf4j
public class RestTemplateUtil {


    public static String post(String url, Map<String, Object> parameter) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> r = new HttpEntity<Map<String, Object>>(parameter, requestHeaders);
        String body = restTemplate.postForObject(url, r, String.class);

        return body;
    }


    public static String get(String url, Map<String, Object> parameter) {
        if (!parameter.isEmpty()) {
            url = url + "?";
            for (String key : parameter.keySet()) {
                url = url + key + "=" + parameter.get(key) + "&";
            }
            url = url.substring(0, url.length() - 1);
        }
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        String body = restTemplate.getForEntity(url, String.class, parameter).getBody();
        return body;
    }

}



