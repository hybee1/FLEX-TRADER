package com.synogiestechnologies.flex_trader_auth.JsonAndObjectConversion;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import java.util.Map;


@Component
public class JsonAndObjectConversion {

    public Map jsonToMap(String jsonString){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String errorE = null;
        try {
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            errorE = e.getMessage();

        }
        return Map.of("error", errorE);
    }

    public String objectToJson(Map<String, Object> obj){

        String errorE = null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String jsonString = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
            System.out.println("JSON String: " + jsonString);
            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
            errorE = e.getMessage();
        }

        return "error: " + errorE;
    }
}
