package com.lz.blockchainauthentication.service;

import com.lz.blockchainauthentication.POJO.Device;
import com.lz.blockchainauthentication.POJO.Message;
import com.lz.blockchainauthentication.vc.AnonymousVC;

public interface DeviceService {


    Device initDevice();

    Device selectDevice(String id);

    String requestForRegistration(Device device);

    boolean storeDeviceVC(String regRes, Device device);

    String[] sendData(String data, String deviceVCStr);


    boolean isServe(Message message);

}
