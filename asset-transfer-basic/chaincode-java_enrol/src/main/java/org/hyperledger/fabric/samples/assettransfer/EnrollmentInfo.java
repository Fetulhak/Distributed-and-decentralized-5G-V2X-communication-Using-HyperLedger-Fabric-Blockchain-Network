package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;
import java.math.BigInteger;


@DataType()
public class EnrollmentInfo {

    @Property()
    private String hashedID8_EC;

    @Property()
    private String validityperiod_EC;

    @Property()
    private String itss_ID;

    @Property()
    private Cert[] certificates;

    @Property()
    private BigInteger accumulator;

    @Property()
    private BigInteger[] witnesses;

    @Property()
    private BigInteger N;
    
    @Property()
    private BigInteger A0;

    @Property()
    private String status;

    private static final Genson genson = new Genson();
    private static final ObjectMapper mapper = new ObjectMapper();


    public  EnrollmentInfo() {
    }
    
    public EnrollmentInfo(@JsonProperty("id") String hashedID8_EC,  @JsonProperty("validityperiod") String validityperiod_EC, @JsonProperty("itssid") String itss_ID,
      @JsonProperty("requiredcert") Cert[] docs, @JsonProperty("accumulator") BigInteger accumulator, @JsonProperty("witnesses") BigInteger[] witnesses, 
      @JsonProperty("n") BigInteger N, @JsonProperty("anot") BigInteger A0,
      @JsonProperty("status") String status) {
        this.hashedID8_EC = hashedID8_EC;
        this.validityperiod_EC = validityperiod_EC;
        this.itss_ID = itss_ID;
        this.certificates = docs;
        this.accumulator = accumulator;
        this.witnesses = witnesses;
        this.N = N;
        this.A0 = A0;
        this.status = status;
    }



    
    
    
    public String getid() {
        return hashedID8_EC;
    }

    public void setid(String id) {
        this.hashedID8_EC = id;
    }

    public String getvalidityperiod() {
        return validityperiod_EC;
    }

    public void setvalidityperiod(String validityperiod) {
        this.validityperiod_EC = validityperiod;
    }


    public String getitssid() {
        return itss_ID;
    }

    public void setitssid(String itssid) {
        this.itss_ID = itssid;
    }

    
    public Cert[] getrequiredcert() {
        return certificates;
    }

    public void setrequiredcert(Cert[] docs) {
        this.certificates = docs;
    }

    public BigInteger getaccumulator() {
        return accumulator;
    }

    public void setaccumulator(BigInteger acc) {
        this.accumulator = acc;
    }

    
    public BigInteger[] getwitnesses() {
        return witnesses;
    }

    public void setwitnesses(BigInteger[] witn) {
        this.witnesses = witn;
    }

    public BigInteger getn() {
        return N;
    }

    public void setn(BigInteger n) {
        this.N = n;
    }


    public BigInteger getanot() {
        return A0;
    }

    public void setanot(BigInteger a0) {
        this.A0 = a0;
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

    public static EnrollmentInfo fromJSONString (String json){
       
        


        //@SuppressWarnings("unchecked")
        
        EnrollmentInfo enrollment_info= null;

        

        try {

            
            enrollment_info = mapper.readValue(json, EnrollmentInfo.class);
            
            

            

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return enrollment_info;
    }
}
    
    
    
    
    
    