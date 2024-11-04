package com.lz.blockchainauthentication.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {


    @Cacheable(value = "sessionCache", key = "#key")
    public String cacheValue(String key, String value) {
        System.out.println("Caching value: " + value + " with key: " + key);
        return value;
    }


    @Cacheable(value = "sessionCache", key = "#key")
    public String getCachedValue(String key) {
        return null;
    }


    @CacheEvict(value = "sessionCache", key = "#key")
    public void clearCache(String key) {
        System.out.println("Cache cleared for key: " + key);
    }
}
