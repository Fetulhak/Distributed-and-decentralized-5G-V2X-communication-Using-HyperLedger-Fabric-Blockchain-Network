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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.owlike.genson.Genson;

import java.nio.charset.Charset;
import java.util.Random;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


@Contract(name = "ValidationContract",
    info = @Info(title = "ValidationContract",
                description = "Validation SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "ValidationContract@example.com",
                                name = "validationSC",
                                url = "http://ValidationContract.me")))
                                
                                
@Default
public class ValidationContract implements ContractInterface {

    private final Genson genson = new Genson();
    ObjectMapper mapper = new ObjectMapper();


    public  ValidationContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx, String enrollmentContractId, String v2xContractId) {
        
        System.out.println("Initializing the validation contract");
        ChaincodeStub stub = ctx.getStub();
        stub.putState(ValConstants.enrollmentContractIdKey, enrollmentContractId.getBytes(UTF_8));
        stub.putState(ValConstants.V2xContractIdKey, v2xContractId.getBytes(UTF_8));


    }
    
    
    
    @Transaction()
    public boolean exists(Context ctx, String id) {
        // Check if validation request exists
        byte[] buffer = ctx.getStub().getState(id);
        return (buffer != null && buffer.length > 0);
    }

    
    public String getValKey(ChaincodeStub stub, String eaId, String aaId, String keytag) {
        CompositeKey ck = stub.createCompositeKey(eaId, aaId, keytag);
        if (ck == null) {
            System.out.println("getValKey() stub function returned null, generating using constructor");
            ck = new CompositeKey(eaId, aaId, keytag);
        }
        return ck.toString();
        

    }

    public String getRandom(ChaincodeStub stub, int q) {
        
        Random random;
        random = new Random();
        random.setSeed(1234589);
        int n = 100000 + random.nextInt(90000000)+q;
        String x= String.valueOf(n);

        return x;
    }


    // This transaction or function is excuted by PCA peer node not ECA == this can be implemented by specifying access control rule
    @Transaction()
    public String validationAtRequest(Context ctx, String eaId, String aaId, String z, String[] ecsign ) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);
        String ValKey = getValKey(stub, eaId, aaId, keytag);
        boolean exists = exists(ctx, ValKey);
        String PCAMSPID = AccessControlUtils.GetClientMspId(ctx);
        if (exists) {
            throw new ChaincodeException("The validation request for " + ValKey + " already exists and the current requesting PCA msp is " + PCAMSPID );
        }

        
        String[] sharedat = {keytag};

        EcSignature[] EcsignDocs = {};
        SharedAtRequest[] SharedAtreqDocs = {};
        if (ecsign.length > 0) {
                EcsignDocs = new EcSignature[ecsign.length];
                for (int i = 0 ; i < ecsign.length ; i++) {
                    EcsignDocs[i] = new EcSignature(ecsign[i]);
                }
            }

        if (sharedat.length > 0) {
            SharedAtreqDocs = new SharedAtRequest[sharedat.length];
            for (int i = 0 ; i < sharedat.length ; i++) {
                SharedAtreqDocs[i] = new SharedAtRequest(sharedat[i]);
            }
        }

        
        
        ValidationInfo valreq = new ValidationInfo(ValKey,eaId, aaId, EcsignDocs, SharedAtreqDocs, new ConfirmedSubjectAttributes[]{}, "REQUESTED");
        
        String valreqStr = valreq.toJSONString();

        stub.putState(ValKey, valreqStr.getBytes(UTF_8));

        return valreqStr;
    }
    
    
    // this function access control is only to ECA == since it holds all enrollment info which should be private to ECA and ITSS itself
    
    @Transaction()
    public void validateAtRequest(Context ctx, String eaId, String d) { 
    
        ChaincodeStub stub = ctx.getStub();
        
        List<String> queryResults = QueryAssets(ctx, "{\"selector\":{\"eaid\":\"" + eaId + "\", \"status\":\"REQUESTED\", \"requiredecsignature\":[{\"docType\":\"" + d + "\"}]}, \"use_index\":[\"_design/validIndexDoc\", \"validIndex\"]}");
        
        

        for (String st : queryResults) {
                ValidationInfo val = ValidationInfo.fromJSONString(st);
                String aaid = val.getaaid();
                SharedAtRequest[] SharedAtreq2= val.getrequiredsharedatreq();
                String keytag = SharedAtreq2[0].getDocType();
                
                String ValKey = getValKey(stub, eaId, aaid, keytag);



                EcSignature [] Ecsignature= val.getrequiredecsignature();
                String hashedID8_EC = Ecsignature[0].getDocType(); 
                
                
                Map<String, Object> enrollmentObj = getEnrollmentInfo(ctx, hashedID8_EC);
                ArrayList<String> EC = (ArrayList<String>) enrollmentObj.get("requiredcert");

                
                String itssid = (String) enrollmentObj.get("itssid");
                
                Map<String, Object> v2xInfoObj = getV2xInfo(ctx, itssid);

                
                //extract the V2X Information and check for its validity before generating validation response

                ArrayList<String> availableITSAID = (ArrayList<String>) v2xInfoObj.get("availableitsaid");
                ArrayList<String> availableGeographicRegion = (ArrayList<String>) v2xInfoObj.get("geographicregion");
                String statusOfV2Xservice = (String) v2xInfoObj.get("status");
                

                if (!statusOfV2Xservice.equals("OK")) {
                    throw new ChaincodeException("The V2X service status for " + itssid + " is in status " + statusOfV2Xservice + " state. Expected OK status.");
                }

                // if everything is OK (i.e the validation process locally) provide validation response to PCA = it will be positive or negative
                String[] confirmedattr = {"dummyconfirmedattributes"}; 
                ConfirmedSubjectAttributes[] ConfirmedatrDocs = {};
                if (confirmedattr.length > 0) {
                        ConfirmedatrDocs = new ConfirmedSubjectAttributes[confirmedattr.length];
                        for (int i = 0 ; i < confirmedattr.length ; i++) {
                            ConfirmedatrDocs[i] = new ConfirmedSubjectAttributes(confirmedattr[i]);
                        }
                    }
                
                val.setconfirmedattributes(ConfirmedatrDocs);
                val.setStatus("RESPONSEOK");

                String valresStr = val.toJSONString();
                System.out.println(valresStr);

                stub.putState(ValKey, valresStr.getBytes(UTF_8));

                
            }

        
    }


    @Transaction()
    public String getValidationResponse(Context ctx, String eaId, String aaid, String keytag) {
    
        ChaincodeStub stub = ctx.getStub();
        
        
        String queryResults = QueryAssets2(ctx, "{\"selector\":{\"eaid\":\"" + eaId + "\", \"status\":\"RESPONSEOK\", \"requiredsharedatreq\":[{\"docType\":\""+ keytag+ "\"}], \"aaid\":\"" + aaid +"\"}, \"use_index\":[\"_design/validIndexDoc\", \"validIndex\"]}");
        
        
        
        return queryResults;
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



    private Map<String, Object> getEnrollmentInfo(Context ctx, String hashedID8_EC) {
        // Look up the enrollment contract name
        ChaincodeStub stub = ctx.getStub();
        byte[] ecBytes = stub.getState(ValConstants.enrollmentContractIdKey);
        if (ecBytes == null || ecBytes.length == 0) {
            throw new ChaincodeException("No enrollemnt contract id recorded on ledger");
        }
        String enrollmentChaincodeId = new String(ecBytes);

        // Lookup the enrollment info by invoking the enrollemtn chaincode
        Response enrollmentResp = ctx.getStub().invokeChaincodeWithStringArgs(enrollmentChaincodeId, ValConstants.getEnrollmentInfoFunc, hashedID8_EC);
        if (enrollmentResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + enrollmentChaincodeId + "' chaincode, function '" + ValConstants.getEnrollmentInfoFunc + "'");
        }

        String enrollment_info = enrollmentResp.getStringPayload();
        
        
        if (enrollment_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate enrollment for : " + hashedID8_EC + ".");
        }

        //@SuppressWarnings("unchecked")
        Map<String, Object> enrollmentObj = new HashMap<String, Object>();

        try {

            
            enrollmentObj = mapper.readValue(enrollment_info, Map.class);

                        

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return enrollmentObj;

        
        
    }



    private Map<String, Object> getV2xInfo(Context ctx, String itssid) {
        // Look up the v2x contract name
        ChaincodeStub stub = ctx.getStub();
        byte[] v2xcBytes = stub.getState(ValConstants.V2xContractIdKey);
        if (v2xcBytes == null || v2xcBytes.length == 0) {
            throw new ChaincodeException("No v2xcontract id recorded on ledger");
        }
        String v2xChaincodeId = new String(v2xcBytes);

        // Lookup the v2x info by invoking the v2x chaincode
        Response v2xInfoResp = ctx.getStub().invokeChaincodeWithStringArgs(v2xChaincodeId, ValConstants.getV2xfoFunc, itssid);
        if (v2xInfoResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + v2xChaincodeId + "' chaincode, function '" + ValConstants.getV2xfoFunc + "'");
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


    
    
    

                           