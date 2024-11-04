package com.lz.blockchainauthentication.POJO;

import com.lz.blockchainauthentication.util.ECCUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

@Component
@Data
@ToString
public class ESP {

    {
        keyPair = ECCUtil.generateSame((byte) 0);
    }
    KeyPair keyPair;

}
