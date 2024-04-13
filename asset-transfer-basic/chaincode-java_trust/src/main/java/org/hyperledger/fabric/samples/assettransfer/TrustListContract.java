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
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;


import com.owlike.genson.Genson;

import java.nio.charset.Charset;
import java.util.Random;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
//import java.math.BigInteger;

import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;


@Contract(name = "TrustListContract",
    info = @Info(title = "TrustListContract",
                description = "CRL and CTL SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "TrustListContract@example.com",
                                name = "crlctlSC",
                                url = "http://TrustListContract.me")))
                                
                                
@Default
public class TrustListContract implements ContractInterface {

    private final Genson genson = new Genson();
    ObjectMapper mapper = new ObjectMapper();

    public  TrustListContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx) {
        
        System.out.println("Initializing the TrustList contract");
        ChaincodeStub stub = ctx.getStub();
        

    }

    @Transaction()
    @SuppressWarnings("unchecked")
    public boolean exists(Context ctx, String ctlkey) {
        
        byte[] buffer = ctx.getStub().getState(ctlkey);
        return (buffer != null && buffer.length > 0);
    }

    public String getCTLKey(ChaincodeStub stub, String hashedID8_RC) {
        String prefix = "CTL";
        CompositeKey ck = stub.createCompositeKey(prefix, hashedID8_RC);
        if (ck == null) {
            System.out.println("getLCKey() stub function returned null, generating using constructor");
            ck = new CompositeKey(prefix, hashedID8_RC);
        }
        return ck.toString();
    }

    public String getCRLKey(ChaincodeStub stub, String hashedID8_RC) {
        String prefix = "CRL";
        CompositeKey ck = stub.createCompositeKey(prefix, ihashedID8_RCd);
        if (ck == null) {
            System.out.println("getPaymentKey() stub function returned null, generating using constructor");
            ck = new CompositeKey(prefix, hashedID8_RC);
        }
        return ck.toString();
    }

    

    @Transaction()
    @SuppressWarnings("unchecked")
    public String uploadCTLInfo(Context ctx, String hashedID8_RC, String nextUpdate, String[] eaCertificate, String[] aaAccessPoint, String[] itsAccessPoint, String[] aaCertificate, String aaCertificate, 
        String[] aaaccessPoint, String[] dcurl, String[] docs) {
    
        ChaincodeStub stub = ctx.getStub();
        String ctlKey = getCTLKey(stub, hashedID8_RC);
        boolean exists = exists(ctx, ctlKeys);
        String ECAMSPID = AccessControlUtils.GetClientMspId(ctx);
        if (exists) {

            stub.delState(ctlKey);
            
        }

        
        String [] eaCertificatelist = {};
        String [] aaAccessPointlist = {};
        String [] itsAccessPointlist = {};
        String [] aaCertificatelist = {};
        String [] aaaccessPointlist = {};
        String [] dcurllist = {};
        Cert[] cerDocs = {};


        if (docs.length > 0) {
                cerDocs = new Cert[docs.length];
                for (int i = 0 ; i < docs.length ; i++) {
                    cerDocs[i] = new Cert(docs[i]);
                }
            }

        if (eaCertificate.length > 0) {
                //itsaidDocs = new AvailableITSAID[itsaid.length];
                for (int i = 0 ; i < eaCertificate.length ; i++) {
                    eaCertificatelist[i] = eaCertificate[i];
                }
            }

        
        if (aaAccessPoint.length > 0) {
            //geolocDocs = new GeographicRegion[geoloc.length];
            for (int i = 0 ; i < aaAccessPoint.length ; i++) {
                aaAccessPointlist[i] = aaAccessPoint[i];
            }
        }

        
        if (itsAccessPoint.length > 0) {
                //EcsignDocs = new EcSignature[ecsign.length];
                for (int i = 0 ; i < itsAccessPoint.length ; i++) {
                    itsAccessPointlist[i] = itsAccessPoint[i];
                }
            }
        
        if (aaCertificate.length > 0) {
                //EcsignDocs = new EcSignature[ecsign.length];
                for (int i = 0 ; i < aaCertificate.length ; i++) {
                    aaCertificatelist[i] = aaCertificate[i];
                }
            }

        if (aaaccessPoint.length > 0) {
                //EcsignDocs = new EcSignature[ecsign.length];
                for (int i = 0 ; i < aaaccessPoint.length ; i++) {
                    aaaccessPointlist[i] = aaaccessPoint[i];
                }
            }

        if (dcurl.length > 0) {
                //EcsignDocs = new EcSignature[ecsign.length];
                for (int i = 0 ; i < dcurl.length ; i++) {
                    dcurllist[i] = dcurl[i];
                }
            }

      
        ToBeSignedRcaCtl ctl = new ToBeSignedRcaCtl(hashedID8_RC, nextUpdate, eaCertificatelist, aaAccessPointlist, itsAccessPointlist, aaCertificatelist, 
            aaaccessPointlist, dcurllist, cerDocs);

        
        String ctlStr = ctl.toJSONString();

        stub.putState(ctlKey, ctlStr.getBytes(UTF_8));

        return ctlStr;
    }


    @Transaction()
    @SuppressWarnings("unchecked")
    public String uploadCRLInfo(Context ctx, String hashedID8_RC, String thisUpdate, String nextUpdate, String[] docs) {
    
        ChaincodeStub stub = ctx.getStub();
        String crlKey = getCTLKey(stub, hashedID8_RC);
        boolean exists = exists(ctx, crlKey);
        String ECAMSPID = AccessControlUtils.GetClientMspId(ctx);

        if (exists) {

            stub.delState(crlKey);

        }
       
        Cert[] cerDocs = {};


        if (docs.length > 0) {
                cerDocs = new Cert[docs.length];
                for (int i = 0 ; i < docs.length ; i++) {
                    cerDocs[i] = new Cert(docs[i]);
                }
            }



        
        ToBeSignedCrl crl = new ToBeSignedCrl(thisUpdate, nextUpdate, cerDocs);

        
        String crlStr = crl.toJSONString();

        stub.putState(crlKey, crlStr.getBytes(UTF_8));

        return crlStr;
    }











}




    








    
    
    
    




    
    
    

                           