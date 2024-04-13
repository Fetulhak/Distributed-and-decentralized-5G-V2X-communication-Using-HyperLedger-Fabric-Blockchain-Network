package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;
import java.math.BigInteger;


@DataType()
public class AuthorizationInfo {

    @Property()
    private String eaID;

    @Property()
    private String Ec_signature;

    
    @Property()
    private BigInteger accumulator;

    @Property()
    private BigInteger[] witnesses;

    @Property()
    private String status;

    private static final Genson genson = new Genson();
    
    public AuthorizationInfo(@JsonProperty("eaid") String eaID,  @JsonProperty("ecsignature") String Ec_signature, @JsonProperty("accumulator") BigInteger accumulator,
      @JsonProperty("witnesses") BigInteger[] witnesses, @JsonProperty("status") String status) {
        this.eaID = eaID;
        this.Ec_signature = Ec_signature;
        this.accumulator = accumulator;
        this.witnesses = witnesses;
        this.status = status;
    }
    
    
    
    public String geteaid() {
        return eaID;
    }

    public void seteaid(String id) {
        this.eaID = id;
    }

    public String getecsignature() {
        return Ec_signature;
    }

    public void setecsignature(String ecsign) {
        this.Ec_signature = ecsign;
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


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }

    public static AuthorizationInfo fromJSONString(String json) {
        return genson.deserialize(json, AuthorizationInfo.class);
    }
}
    
    
    
    
    
    