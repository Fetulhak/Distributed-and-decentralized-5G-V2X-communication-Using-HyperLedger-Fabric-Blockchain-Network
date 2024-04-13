package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;

@DataType()
public class GeographicRegion {

    private static final Genson genson = new Genson();

    @Property()
    private String country;

    public GeographicRegion() {
    }

    public GeographicRegion(@JsonProperty("country") String dt) {
        this.country = dt;
    }

    public String getcountry() {
        return country;
    }

    public void setcountry(String dt) {
        this.country = dt;
    }

    public String toJSONString() {
        return genson.serialize(this);
    }

    public static GeographicRegion fromJSONString(String json) {
        return genson.deserialize(json, GeographicRegion.class);
    }
}