package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;

@DataType()
public class AvailableITSAID {

    private static final Genson genson = new Genson();

    @Property()
    private String Psid;

    public AvailableITSAID() {
    }

    public AvailableITSAID(@JsonProperty("serviceid") String dt) {
        this.Psid = dt;
    }

    public String getpsid() {
        return Psid;
    }

    public void setpsid(String dt) {
        this.Psid = dt;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }

    public static AvailableITSAID fromJSONString(String json) {
        return genson.deserialize(json, AvailableITSAID.class);
    }
}