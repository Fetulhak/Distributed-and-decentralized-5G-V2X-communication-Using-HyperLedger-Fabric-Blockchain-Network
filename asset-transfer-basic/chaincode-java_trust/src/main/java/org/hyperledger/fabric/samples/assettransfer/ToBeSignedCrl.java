package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;

@DataType()
public class ToBeSignedCrl {

    private static final Genson genson = new Genson();

    @Property()
    private String thisUpdate;

    @Property()
    private String nextUpdate;

    @Property()
    private Cert[] HashedId8;

    public Cert() {
    }

    public Cert(@JsonProperty("thisUpdate") String thisUpdate, @JsonProperty("nextUpdate") String nextUpdate, @JsonProperty("HashedId8") Cert[]  HashedId8) {
        this.thisUpdate = thisUpdate;
        this.nextUpdate = nextUpdate;
        this.HashedId8 = HashedId8;
    }

    public String getthisUpdate() {
        return thisUpdate;
    }

    public void setthisUpdate(String thisUpdate) {
        this.thisUpdate = thisUpdate;
    }

    public String getnextUpdate() {
        return nextUpdate;
    }

    public void setnextUpdate(String nextUpdate) {
        this.nextUpdate = nextUpdate;
    }

    public Cert[] getHashedId8() {
        return HashedId8;
    }

    public void setHashedId8(Cert[] HashedId8) {
        this.HashedId8 = HashedId8;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }

    public static ToBeSignedCrl fromJSONString(String json) {
        return genson.deserialize(json, ToBeSignedCrl.class);
    }


    
}