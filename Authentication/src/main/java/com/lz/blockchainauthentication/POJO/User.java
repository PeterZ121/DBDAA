package com.lz.blockchainauthentication.POJO;

import com.lz.blockchainauthentication.vc.AnonymousVC;
import com.lz.blockchainauthentication.vc.PreVC;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.KeyPair;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String UID;
    private KeyPairExp firstKeyPairExp;
    private Map<String, KeyPairExp> secondKeyPairExps;
    private PreVC preVC;
    private Map<String, AnonymousVC> anonymousVCS;

}
