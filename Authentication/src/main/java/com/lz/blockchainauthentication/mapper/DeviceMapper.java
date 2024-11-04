package com.lz.blockchainauthentication.mapper;

import com.lz.blockchainauthentication.POJO.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceMapper {
    int insertDevice(@Param("id")String id,
                     @Param("privateKey") BigInteger privateKey,
                     @Param("publicKey")String publicKey);

    Device selectDeviceById(@Param("id") String id);

    int updateById(@Param("id") String id,
                   @Param("buildingNum") int buildingNum,
                   @Param("DDID") String DDID);

    Device selectDeviceByDDID(@Param("DDID") String DDID);

    List<String> selectDDIDByNotLimited();

    List<Map<String,Object>> selectAllDevice();
}
