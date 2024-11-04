package com.lz.blockchainauthentication.vc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AnonymousVC {
    String ADID;
    String DDID;
    String signature;


    public AnonymousVC(String jsonString) throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        AnonymousVC deserialized = mapper.readValue(jsonString, AnonymousVC.class);
        this.ADID = deserialized.ADID;
        this.DDID = deserialized.DDID;
        this.signature = deserialized.signature;
    }


    @Override
    public String toString() {
        return "AnonymousVC{" +
                "ADID='" + ADID + '\'' +
                ", DDID='" + DDID + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }


    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
