package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;

@DataType()
public class SharedAtRequest {

    private static final Genson genson = new Genson();

    @Property()
    private String docType;

    public SharedAtRequest() {
    }

    public SharedAtRequest(@JsonProperty("docType") String dt) {
        this.docType = dt;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String dt) {
        this.docType = dt;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }

    public static SharedAtRequest fromJSONString(String json) {
        return genson.deserialize(json, SharedAtRequest.class);
    }
}