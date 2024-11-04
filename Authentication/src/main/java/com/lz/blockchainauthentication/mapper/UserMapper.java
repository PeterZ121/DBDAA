package com.lz.blockchainauthentication.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;

@Mapper
public interface UserMapper {

    int insertRealUser(@Param("mdid")String mdid,
                       @Param("pk")String publicKey,
                       @Param("merkleRoot")String merkleRoot);

    String selectMerkleRootByMDID(String mdid);

    int selectCountByMDID(String mdid);

    int insertAnonymousUser(@Param("adid")String adid,
                            @Param("pk")String publicKey,
                            @Param("hM")String hM);

    int selectCountByADID(String adid);

    String selectHMByADID(String adid);

}
