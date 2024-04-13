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
//import org.hyperledger.fabric.shim.ledger.QueryResultsIterator.iterator;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;


import com.owlike.genson.Genson;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
//import java.math.BigInteger;


@Contract(name = "EnrollmentContract",
    info = @Info(title = "EnrollmentContract",
                description = "Enrollment SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "EnrollmentContract@example.com",
                                name = "enrollmentSC",
                                url = "http://enrollmentcontract.me")))
                                
                                
@Default
public class EnrollmentContract implements ContractInterface {

    private final Genson genson = new Genson();
    ObjectMapper mapper = new ObjectMapper();

    public  EnrollmentContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx, String intermediaryContractId, String intermediarychannelName, String v2xContractId) {
        
        System.out.println("Initializing the enrollment contract");

        ChaincodeStub stub = ctx.getStub();
        stub.putState(EnrollmentConstants.intermediaryContractIdKey, intermediaryContractId.getBytes(UTF_8));
        stub.putState(EnrollmentConstants.intermediaryChannelNameKey, intermediarychannelName.getBytes(UTF_8));
        stub.putState(EnrollmentConstants.V2xContractIdKey, v2xContractId.getBytes(UTF_8));

    }
    
    
    
    @Transaction()
    @SuppressWarnings("unchecked")
    public boolean exists(Context ctx, String hashedID8_EC) {
        // Check if ITSS-S enrolled 
        byte[] buffer = ctx.getStub().getState(hashedID8_EC);
        return (buffer != null && buffer.length > 0);
    }

    public String generateITSSID(ChaincodeStub stub, String SUPI, String y) {
        //Canonical identity = H(SUPI|y)
        // how much time it will take to generate H(SUPI | Y)
        //Assumption == the 5CN peer has access to SUPI and y
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
    @SuppressWarnings("unchecked")
    public String enrollItss(Context ctx, String hashedID8_EC, String validityperiod_EC, String SUPI, String y, String... docs) {
    
        ChaincodeStub stub = ctx.getStub();
        String itssid = generateITSSID(stub,SUPI, y);
        //The ECA may also check whether the vehicle or any ITSS station is already registered by the 5GC for provisioning of ITS service?
        //when the ECA recieve the enrollemnt cert request it can extract the canonical id of the itss from the message and using this ID it can check whether the 5GC
        // already provide service to it before providing enrollemnt certificate to the ITSSS

        Map<String, Object> v2xInfoObj = getV2xInfo(ctx, itssid);
        //extract the V2X Information and check for its validity before generating validation response

        String itssidFrom5GC = (String) v2xInfoObj.get("id");
        ArrayList<String> availableITSAID = (ArrayList<String>) v2xInfoObj.get("availableitsaid");
        ArrayList<String> availableGeographicRegion = (ArrayList<String>) v2xInfoObj.get("geographicregion");
        String statusOfV2Xservice = (String) v2xInfoObj.get("status");

        if (!statusOfV2Xservice.equals("OK") || !itssidFrom5GC.equals(itssid)) {
            throw new ChaincodeException("The V2X Information for " + itssid + " is invalid.");
        }


        boolean exists = exists(ctx, hashedID8_EC);
        String ECAMSPID = AccessControlUtils.GetClientMspId(ctx);
        if (exists) {
            throw new ChaincodeException("The enrollment " + hashedID8_EC + " already exists and the current enrolling ECA msp is " + ECAMSPID );
        }

        Cert[] cerDocs = {};
        if (docs.length > 0) {
                cerDocs = new Cert[docs.length];
                for (int i = 0 ; i < docs.length ; i++) {
                    cerDocs[i] = new Cert(docs[i]);
                }
            }

        Accumulator iniacc = new Accumulator();
        

        
        EnrollmentInfo enrol = new EnrollmentInfo(hashedID8_EC, validityperiod_EC, itssid, cerDocs, iniacc.getA(), new BigInteger[]{}, iniacc.getN(), iniacc.getA0(), "ENROLLED");

        
        String enrolStr = enrol.toJSONString();

        stub.putState(hashedID8_EC, enrolStr.getBytes(UTF_8));

        return enrolStr;
    }




    


    //This transaction function is called by the ECA when a cross border transaction event occured
    @Transaction()
    @SuppressWarnings("unchecked")
    public String enrollnewItss(Context ctx, String home_eaId, String visited_eaId, String z) {
    
        ChaincodeStub stub = ctx.getStub();

        
        Map<String, Object> enrollmentObj = getCrossBorderValidationResponse(ctx, home_eaId, visited_eaId, z);
        
        Map<String, Object>  CBRvalidationResponseInfo2 = (Map<String, Object>) enrollmentObj.get("enrollmentInfo");
        


        String hashedID8_EC = (String) CBRvalidationResponseInfo2.get("id");
        
        String validityperiod = (String) CBRvalidationResponseInfo2.get("validityperiod");
        String itssid = (String) CBRvalidationResponseInfo2.get("itssid");
        
        ArrayList<Map<String, Object>> EC = (ArrayList<Map<String, Object>>) CBRvalidationResponseInfo2.get("requiredcert");
        Map<String, Object>  enrollmentObj2 = EC.get(0);
        
        String EC_cert = (String)enrollmentObj2.get("docType"); 
        String [] mycert = {EC_cert};
        Cert[] cerDocs = {};
        if (mycert.length > 0) {
                cerDocs = new Cert[mycert.length];
                for (int i = 0 ; i < mycert.length ; i++) {
                    cerDocs[i] = new Cert(mycert[i]);
                }
            }
        
        
        String status = (String) CBRvalidationResponseInfo2.get("status");
        BigInteger acc = (BigInteger) CBRvalidationResponseInfo2.get("accumulator");
        
        ArrayList<BigInteger> ECwit = (ArrayList<BigInteger>) CBRvalidationResponseInfo2.get("witnesses");
        
        BigInteger[] updatedwits = {};
        if (ECwit.size() > 0) {
            updatedwits = new BigInteger[ECwit.size()];
            for (int i = 0 ; i < ECwit.size() ; i++) {
                updatedwits[i] = ECwit.get(i);
            }
        }
        BigInteger N = (BigInteger) CBRvalidationResponseInfo2.get("n");
        BigInteger A0 = (BigInteger) CBRvalidationResponseInfo2.get("anot");
        


                
        EnrollmentInfo enrol = new EnrollmentInfo(hashedID8_EC, validityperiod, itssid, cerDocs, acc, updatedwits, N, A0, status);
        
        String enrolStr = enrol.toJSONString();

        stub.putState(hashedID8_EC, enrolStr.getBytes(UTF_8));

        return enrolStr;
    }


    @Transaction()
    @SuppressWarnings("unchecked")
    public String updateAuthInfo(Context ctx, String EC_signature, String hashprimeofPC ) {
    
        ChaincodeStub stub = ctx.getStub();
        // from the ec signature extract the hash_ID8 of the enrollment certificate and delete the enrollemnt for that vehicle
        
        BigInteger hashprimeofPC_casted = new BigInteger(hashprimeofPC);
        String hashedID8_EC = EC_signature;
        boolean exists = this.exists(ctx, hashedID8_EC);
        if (!exists) {
            throw new ChaincodeException("The Enrollment " + hashedID8_EC + " does not exists. " );
        }
        byte[] enrolBytes = stub.getState(hashedID8_EC);

        EnrollmentInfo enrol = EnrollmentInfo.fromJSONString(new String(enrolBytes));

        //Accumulator iniacc = new Accumulator();
        BigInteger A0 = enrol.getanot();
        BigInteger acc_old = enrol.getaccumulator();
        BigInteger[] wit_old= enrol.getwitnesses();
        BigInteger N= enrol.getn();
        BigInteger acc = acc_old.modPow(hashprimeofPC_casted, N);

        


        BigInteger[] updatedwits = {};
        if (wit_old.length > 0) {
                updatedwits = new BigInteger[wit_old.length + 1];
                for (int i = 0 ; i <= wit_old.length ; i++) {
                    if (i != (wit_old.length )){
                        updatedwits[i] = wit_old[i].modPow(hashprimeofPC_casted, N);
                    }
                    else{
                        updatedwits[i] = acc_old;
                    }
                }
        }else {

            updatedwits = new BigInteger[wit_old.length + 1];
            BigInteger product = BigInteger.ONE;
            updatedwits[0] = A0.modPow(product, N);
        }

                            
                        

        enrol.setaccumulator(acc);
        enrol.setwitnesses(updatedwits);
        
        String enrolStr = enrol.toJSONString();

        stub.putState(hashedID8_EC, enrolStr.getBytes(UTF_8));

        System.out.println("Updated enrollemnt and authorization info recorded for vehicle with " + hashedID8_EC + " and value : " + enrolStr);

        return enrolStr;

        


    }



    
    @Transaction()
    @SuppressWarnings("unchecked")
    public String QueryAssets2(Context ctx, String queryString) {
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



    

    @Transaction()
    public void processrevocation(Context ctx, String hashPrimeofPC) {
    
        ChaincodeStub stub = ctx.getStub();
        
        BigInteger hashprimeofPC_casted = new BigInteger(hashPrimeofPC);

        
        List<String> histroyOfEnrolledITSS = QueryAssets(ctx, "{\"selector\":{\"status\":\"ENROLLED\"}, \"use_index\":[\"_design/enrolIndexDoc\", \"enrolIndex\"]}");

        
        String hashedID8_EC_found = null;
        

        for (String enrollment : histroyOfEnrolledITSS) {
                
                EnrollmentInfo enroll = EnrollmentInfo.fromJSONString(enrollment);
                BigInteger acc= enroll.getaccumulator();
                BigInteger[] wits= enroll.getwitnesses();
                BigInteger N = enroll.getn();


                for (int i = 0 ; i < wits.length ; i++){
                    if ((wits[i].modPow(hashprimeofPC_casted, N)).equals(acc)) {
                        hashedID8_EC_found = enroll.getid();
                        System.out.println("To be revoked hashedid8 " + hashedID8_EC_found + ".");
                        break;
                    }
                }

                
                


                if (hashedID8_EC_found != null){
                    
                    boolean exists = this.exists(ctx, hashedID8_EC_found);
                    if (!exists) {
                        throw new ChaincodeException("The Enrollment " + hashedID8_EC_found + " does not exists. " );
                    }else{
                    
                    stub.delState(hashedID8_EC_found);
                    break;}
                    
                }
        }

        
        if (hashedID8_EC_found == null){
            
            
            throw new ChaincodeException("The Enrollment  does not exists. " );
            
            }
        



    }





    

    
    @Transaction()
    @SuppressWarnings("unchecked")
    public List<String> QueryAssets(Context ctx, String queryString) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyValue> results = stub.getQueryResult(queryString);
        

        List<String> allResults = new ArrayList<>();
        for (KeyValue result : results) {
            String key = result.getKey();
            String jsonValue = result.getStringValue();
            
            String output=jsonValue;
            allResults.add(output);
            }
        
        return allResults;
        
    }


    @Transaction()
    @SuppressWarnings("unchecked")
    public String getEnrollment(Context ctx, String hashedID8_EC) {
    
        ChaincodeStub stub = ctx.getStub();
        boolean exists = this.exists(ctx, hashedID8_EC);
        if (!exists) {
            throw new ChaincodeException("The Enrollment " + hashedID8_EC + " does not exists. " );
        }
        byte[] enrolBytes = stub.getState(hashedID8_EC);

        String enrolStr = new String(enrolBytes);
   
        return enrolStr;
    }



    @Transaction()
    @SuppressWarnings("unchecked")
    public BigInteger getAccumulator(Context ctx, String hashedID8_EC) {
    
        EnrollmentInfo enrol = getEnrollmentInfo(ctx, hashedID8_EC);

        
        return enrol.getaccumulator();
    }
    
    

    
    @Transaction()
    @SuppressWarnings("unchecked")
    public String getEnrollmentStatus(Context ctx, String hashedID8_EC) {
    
         EnrollmentInfo enrol = getEnrollmentInfo(ctx, hashedID8_EC);
        
        return enrol.getStatus();
    }


    @Transaction()
    @SuppressWarnings("unchecked")
    public String getitssid(Context ctx, String hashedID8_EC) {
    
        EnrollmentInfo enrol = getEnrollmentInfo(ctx, hashedID8_EC);
        
        return enrol.getitssid();
    }


    @Transaction()
    @SuppressWarnings("unchecked")
    public String getvalidityperiod(Context ctx, String hashedID8_EC) {
    
        EnrollmentInfo enrol = getEnrollmentInfo(ctx, hashedID8_EC);
        
        return enrol.getvalidityperiod();
    }



    @Transaction()
    @SuppressWarnings("unchecked")
    public Cert[] getcertificate(Context ctx, String hashedID8_EC) {
    
        EnrollmentInfo enrol = getEnrollmentInfo(ctx, hashedID8_EC);
        
        return enrol.getrequiredcert();
    }


    @SuppressWarnings("unchecked")
    private EnrollmentInfo getEnrollmentInfo(Context ctx, String hashedID8_EC) {
    
        ChaincodeStub stub = ctx.getStub();
        boolean exists = this.exists(ctx, hashedID8_EC);
        if (!exists) {
            throw new ChaincodeException("The Enrollment " + hashedID8_EC + " does not exists. " );
        }
        byte[] enrolBytes = stub.getState(hashedID8_EC);

        EnrollmentInfo enroll = EnrollmentInfo.fromJSONString(new String(enrolBytes));
   
        return enroll;


        
    }



    @SuppressWarnings("unchecked")
    private Map<String, Object> getCrossBorderValidationResponse(Context ctx, String home_eaId, String visited_eaId, String z) {
        
        ChaincodeStub stub = ctx.getStub();

        byte[] ichBytes = stub.getState(EnrollmentConstants.intermediaryChannelNameKey);
        if (ichBytes == null || ichBytes.length == 0) {
            throw new ChaincodeException("No intermediary channel name recorded on ledger");
        }
        String interChannel = new String(ichBytes);

        byte[] icBytes = stub.getState(EnrollmentConstants.intermediaryContractIdKey);
        if (icBytes == null || icBytes.length == 0) {
            throw new ChaincodeException("No intermediary contract id recorded on ledger");
        }
        String intermediaryChaincodeId = new String(icBytes);

        
        ArrayList<String> CBRvalidationArgs = new ArrayList<String>();
        CBRvalidationArgs.add(EnrollmentConstants.getCrossBorderValidationResponseFunc);
        CBRvalidationArgs.add(home_eaId);
        CBRvalidationArgs.add(visited_eaId);
        CBRvalidationArgs.add(z);
        Response CBRvalidationResp = ctx.getStub().invokeChaincodeWithStringArgs(intermediaryChaincodeId, CBRvalidationArgs, interChannel);
        if (CBRvalidationResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + intermediaryChaincodeId + "' chaincode, function '" + EnrollmentConstants.getCrossBorderValidationResponseFunc + "'");
        }

        String CBRvalResp = CBRvalidationResp.getStringPayload();
        

        if (CBRvalResp.isEmpty()) {
            throw new ChaincodeException("Unable to get the cross border validation response for trade ");
        }

        
        //@SuppressWarnings("unchecked")
        Map<String, Object> enrollmentObj = new HashMap<String, Object>();

        try {

            
            enrollmentObj = mapper.readValue(CBRvalResp, Map.class);

                        

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return enrollmentObj;
    }



    private Map<String, Object> getV2xInfo(Context ctx, String itssid) {
        
        ChaincodeStub stub = ctx.getStub();
        byte[] v2xcBytes = stub.getState(EnrollmentConstants.V2xContractIdKey);
        if (v2xcBytes == null || v2xcBytes.length == 0) {
            throw new ChaincodeException("No v2xcontract id recorded on ledger");
        }
        String v2xChaincodeId = new String(v2xcBytes);

        // Lookup the v2x info by invoking the v2x chaincode
        Response v2xInfoResp = ctx.getStub().invokeChaincodeWithStringArgs(v2xChaincodeId, EnrollmentConstants.getV2xInfoFunc, itssid);
        if (v2xInfoResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + v2xChaincodeId + "' chaincode, function '" + EnrollmentConstants.getV2xInfoFunc + "'");
        }

        String v2x_info = v2xInfoResp.getStringPayload();
        
        if (v2x_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate v2x service record for : " + itssid + ".");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> v2xInfoObj = genson.deserialize(v2x_info, Map.class);
        return v2xInfoObj;
    }



    




}


    
    
    

                           