package org.hyperledger.fabric.samples.assettransfer;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;


import com.owlike.genson.Genson;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;



import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;


@Contract(name = "V2xContract",
    info = @Info(title = "V2xContract",
                description = "V2x SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "V2xContract@example.com",
                                name = "v2xSC",
                                url = "http://V2xContract.me")))
                                
                                
@Default
public class V2xContract implements ContractInterface {

    private final Genson genson = new Genson();

    public  V2xContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx) {
        
        System.out.println("Initializing the v2x contract");
    }
    
    
    
    @Transaction()
    public boolean exists(Context ctx, String id) {
        // Check if ITSS-S V2x related info is uploaded by the 5GC 
        byte[] buffer = ctx.getStub().getState(id);
        return (buffer != null && buffer.length > 0);
    }


    public String generateITSSID(ChaincodeStub stub, String SUPI, String y) {
        
        String ITSSID = SUPI + y; 
        MessageDigest md = null;
        try {
        md = MessageDigest.getInstance("SHA-256");

            }catch(NoSuchAlgorithmException e) {
                 System.out.println("Something is wrong");
        }
        
        md.update(ITSSID.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        String hex = String.format("%064x", new BigInteger(1, digest));
        System.out.println("The hash of SUPI and y is " +hex+ " .");
        
        return hex;
    }


    @Transaction()
    public String uploadItssV2xInfo(Context ctx, String SUPI, String y, String[] itsaid, String[] geoloc) {
    
        ChaincodeStub stub = ctx.getStub();
        String canonicalid = generateITSSID(stub,SUPI, y);
        boolean exists = exists(ctx, canonicalid);
        String MSPID = AccessControlUtils.GetClientMspId(ctx);
        if (exists) {
            throw new ChaincodeException("V2X information for " + canonicalid + " already exist.");
        }

        AvailableITSAID[] itsaidDocs = {};
        GeographicRegion[] geolocDocs = {};
        if (itsaid.length > 0) {
                itsaidDocs = new AvailableITSAID[itsaid.length];
                for (int i = 0 ; i < itsaid.length ; i++) {
                    itsaidDocs[i] = new AvailableITSAID(itsaid[i]);
                }
            }

        if (geoloc.length > 0) {
            geolocDocs = new GeographicRegion[geoloc.length];
            for (int i = 0 ; i < geoloc.length ; i++) {
                geolocDocs[i] = new GeographicRegion(geoloc[i]);
            }
        }

        
        V2xInfo v2x = new V2xInfo(canonicalid, itsaidDocs, geolocDocs,"OK");
        
        String v2xStr = v2x.toJSONString();

        stub.putState(canonicalid, v2xStr.getBytes(UTF_8));

        return v2xStr;
    }


    
    @Transaction()
    public String getV2xInfo(Context ctx, String id) {
    
        ChaincodeStub stub = ctx.getStub();
        boolean exists = this.exists(ctx, id);
        if (!exists) {
            throw new ChaincodeException("The V2X information for " + id + " does not exists. " );
        }
        byte[] v2xinfoBytes = stub.getState(id);

        String v2xinfoStr = new String(v2xinfoBytes);
   
        return v2xinfoStr;
    }



    
    @Transaction()
    @SuppressWarnings("unchecked")
    public String QueryAssets(Context ctx, String queryString) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);
        

        List<String> allResults = new ArrayList<>();
        for (KeyValue result : results) {
            String key = result.getKey();
            String jsonValue = result.getStringValue();
            
            String output=jsonValue;
            allResults.add(output);
            }

        return String.join(",", allResults);
        
    }





}


    
    
    

                           