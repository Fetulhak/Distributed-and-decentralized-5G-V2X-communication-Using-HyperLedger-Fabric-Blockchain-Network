package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;


@DataType()
public class ValidationInfo {

    @Property()
    private String Tx_id;

    @Property()
    private String recipient_eaId;

    @Property()
    private String requester_aaId;

    @Property()
    private EcSignature[] Ecsigna;

    @Property()
    private SharedAtRequest[] SharedAtreq;

    @Property()
    private ConfirmedSubjectAttributes[] confirmedattr;

    @Property()
    private String status;

    private static final Genson genson = new Genson();
    
    public ValidationInfo(@JsonProperty("id") String Tx_id, @JsonProperty("eaid") String recipient_eaId,  @JsonProperty("aaid") String requester_aaId, @JsonProperty("requiredecsignature") EcSignature[] Ecsigna, 
        @JsonProperty("requiredsharedatreq") SharedAtRequest[] SharedAtreq, @JsonProperty("confirmedattributes") ConfirmedSubjectAttributes[] confirmedattr, 
        @JsonProperty("status") String status) {
        this.Tx_id = Tx_id;
        this.recipient_eaId = recipient_eaId;
        this.requester_aaId = requester_aaId;
        this.Ecsigna = Ecsigna;
        this.SharedAtreq = SharedAtreq;
        this.confirmedattr = confirmedattr;
        this.status = status;
    }
    
    
    public String getid() {
        return Tx_id;
    }

    public void setid(String id) {
        this.Tx_id = id;
    }


    public String geteaid() {
        return recipient_eaId;
    }

    public void seteaid(String id) {
        this.recipient_eaId = id;
    }

    public String getaaid() {
        return requester_aaId;
    }

    public void setaaid(String id) {
        this.requester_aaId = id;
    }

    

    public EcSignature[] getrequiredecsignature() {
        return Ecsigna;
    }

    public void setrequiredecsignature(EcSignature[] docs) {
        this.Ecsigna = docs;
    }

    
    public SharedAtRequest[] getrequiredsharedatreq() {
        return SharedAtreq;
    }

    public void setrequiredsharedatreq(SharedAtRequest[] docs) {
        this.SharedAtreq = docs;
    }


    public ConfirmedSubjectAttributes[] getconfirmedattributes() {
        return confirmedattr;
    }

    public void setconfirmedattributes(ConfirmedSubjectAttributes[] docs) {
        this.confirmedattr = docs;
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

    public static ValidationInfo fromJSONString(String json) {
        return genson.deserialize(json, ValidationInfo.class);
    }
}
    
    
    
    
    
    