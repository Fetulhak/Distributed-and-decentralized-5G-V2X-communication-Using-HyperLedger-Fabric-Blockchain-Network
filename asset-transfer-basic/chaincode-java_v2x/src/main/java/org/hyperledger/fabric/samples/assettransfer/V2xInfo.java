package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;


@DataType()
public class V2xInfo {

    @Property()
    private String id;

    @Property()
    private AvailableITSAID[] ITSAID;

    @Property()
    private GeographicRegion[] geo;

    @Property()
    private String status;

    private static final Genson genson = new Genson();
    
    public V2xInfo(@JsonProperty("id") String itss_ID, @JsonProperty("availableitsaid") AvailableITSAID[] itsaid, @JsonProperty("geographicregion") GeographicRegion[] geoloc,
        @JsonProperty("status") String status) {
        this.id = itss_ID;
        this.ITSAID = itsaid;
        this.geo = geoloc;
        this.status = status;
    }
    
    
    
    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    
    public AvailableITSAID[] getavailableitsaid() {
        return ITSAID;
    }

    public void setavailableitsaid(AvailableITSAID[] docs) {
        this.ITSAID = docs;
    }

    public GeographicRegion[] getGeographicregion() {
        return geo;
    }

    public void getGeographicregion(GeographicRegion[] docs) {
        this.geo = docs;
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

    public static V2xInfo fromJSONString(String json) {
        return genson.deserialize(json, V2xInfo.class);
    }
}
    
    
    
    
    
    