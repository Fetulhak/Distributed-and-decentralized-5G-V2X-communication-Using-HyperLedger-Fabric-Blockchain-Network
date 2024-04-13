package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


@DataType()
public class IntermediaryInfo {

    @Property()
    private String home_eaId;

    @Property()
    private String visited_eaId;

    @Property()
    private String hashedID8_EC;

    private Map<String, Object> EnrollmentInfo;


    @Property()
    private String status;

    private static final Genson genson = new Genson();
    private static final ObjectMapper mapper = new ObjectMapper();

    public  IntermediaryInfo() {
    }
    
    public IntermediaryInfo(@JsonProperty("homeeaId") String homeeaId,  @JsonProperty("visitedeaId") String visitedeaId, @JsonProperty("hashedID8EC") String hashedid8ec, 
        @JsonProperty("enrollmentinfo") Map<String, Object> enrinfo, @JsonProperty("status") String status) {
        this.home_eaId = homeeaId;
        this.visited_eaId = visitedeaId;
        this.hashedID8_EC = hashedid8ec;
        this.EnrollmentInfo = enrinfo;
        this.status = status;
    }
    
    
    
    public String gethomeeaId() {
        return home_eaId;
    }

    public void sethomeeaId(String id) {
        this.home_eaId = id;
    }

    
    public String getvisitedeaId() {
        return visited_eaId;
    }

    public void setvisitedeaId(String id) {
        this.visited_eaId = id;
    }

    public String gethashedID8EC() {
        return hashedID8_EC;
    }

    public void sethashedID8EC(String id) {
        this.hashedID8_EC = id;
    }

    public Map<String, Object>  getEnrollmentInfo() {
        return EnrollmentInfo;
    }

    public void setEnrollmentInfo(Map<String, Object> field) {
        this.EnrollmentInfo = field;
    }



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }



    public static IntermediaryInfo fromJSONString (String json){
        
        IntermediaryInfo enrollment_info= null;

        

        try {

            
            enrollment_info = mapper.readValue(json, IntermediaryInfo.class);
            
                    

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return enrollment_info;
    }
}
    
    
    
    
    
    