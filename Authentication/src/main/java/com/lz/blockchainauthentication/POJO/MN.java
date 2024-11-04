package com.lz.blockchainauthentication.POJO;

import com.lz.blockchainauthentication.util.ECCUtil;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyPair;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MN {
    private KeyPair keyPair;
    private int buildingNum;

}


