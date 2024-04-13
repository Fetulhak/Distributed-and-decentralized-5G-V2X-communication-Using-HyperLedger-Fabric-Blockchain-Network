package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;
import com.owlike.genson.Genson;
import java.math.BigInteger;


@DataType()
public class ToBeSignedRcaCtl {

    @Property()
    private String nextUpdate;

    
    @Property()
    private String[] eaCertificate;

    @Property()
    private String[] aaAccessPoint;

    @Property()
    private String[] itsAccessPoint;

    @Property()
    private String[] aaCertificate;

    @Property()
    private String[] aaaccessPoint;

    @Property()
    private String[] dcurl;

    @Property()
    private String[] RCAcertdigest;

    private static final Genson genson = new Genson();
    
    public AuthorizationInfo(@JsonProperty("version") String version,  @JsonProperty("nextUpdate") String nextUpdate, @JsonProperty("eaCertificate") String eaCertificate,
      @JsonProperty("aaAccessPoint") String aaAccessPoint, @JsonProperty("itsAccessPoint") String itsAccessPoint, @JsonProperty("aaCertificate") String aaCertificate, 
      @JsonProperty("aaaccessPoint") String aaaccessPoint, @JsonProperty("dcurl") String dcurl, @JsonProperty("RCAcertdigest") String RCAcertdigest) {
        this.nextUpdate = nextUpdate;
        this.eaCertificate = eaCertificate;
        this.aaAccessPoint = aaAccessPoint;
        this.itsAccessPoint = itsAccessPoint;
        this.aaCertificate = aaCertificate;
        this.aaaccessPoint = aaaccessPoint;
        this.dcurl = dcurl;
        this.RCAcertdigest = RCAcertdigest;
    }
    
    
    
    public String getnextUpdate() {
        return nextUpdate;
    }

    public void setnextUpdate(String id) {
        this.nextUpdate = id;
    }

    public String geteaCertificate() {
        return eaCertificate;
    }

    public void seteaCertificate(String eacert) {
        this.eaCertificate = eacert;
    }


    public BigInteger getaaAccessPoint() {
        return aaAccessPoint;
    }

    public void setaaAccessPoint(String aaAccessPoint) {
        this.aaAccessPoint = aaAccessPoint;
    }

    
    public BigInteger[] getitsAccessPoint() {
        return itsAccessPoint;
    }

    public void setitsAccessPoint(String itsAccessPoint) {
        this.itsAccessPoint = itsAccessPoint;
    }


    public String getaaCertificate() {
        return aaCertificate;
    }

    public void setaaCertificate(String aaCertificate) {
        this.aaCertificate = aaCertificate;
    }


    public String getaaaccessPoint() {
        return aaaccessPoint;
    }


    public void setaaaccessPoint(String aaaccessPoint) {
        this.aaaccessPoint = aaaccessPoint;
    }



    public String getdcurl() {
        return dcurl;
    }


    public void setdcurl(String dcurl) {
        this.dcurl = dcurl;
    }

    public String getRCAcertdigest() {
        return RCAcertdigest;
    }


    public void setRCAcertdigest(String RCAcertdigest) {
        this.RCAcertdigest = RCAcertdigest;
    }



    public String toJSONString() {
        return genson.serialize(this);
    }

    public static ToBeSignedRcaCtl fromJSONString(String json) {
        return genson.deserialize(json, ToBeSignedRcaCtl.class);
    }
}
    
    
    
    
    
    