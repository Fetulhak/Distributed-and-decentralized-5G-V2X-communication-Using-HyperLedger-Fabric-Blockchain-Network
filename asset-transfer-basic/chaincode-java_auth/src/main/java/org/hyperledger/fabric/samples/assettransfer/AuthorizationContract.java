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


@Contract(name = "AuthorizationContract",
    info = @Info(title = "AuthorizationContract",
                description = "Authorization SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "AuthorizationContract@example.com",
                                name = "authorizationSC",
                                url = "http://AuthorizationContract.me")))
                                
                                
@Default
public class AuthorizationContract implements ContractInterface {

    private final Genson genson = new Genson();
    ObjectMapper mapper = new ObjectMapper();

    public  AuthorizationContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx, String enrollmentContractId, String validationContractId) {
        
        System.out.println("Initializing the authorization contract");
        ChaincodeStub stub = ctx.getStub();
        stub.putState(AuthConstants.enrollmentContractIdKey, enrollmentContractId.getBytes(UTF_8));
        stub.putState(AuthConstants.validationContractIdKey, validationContractId.getBytes(UTF_8));

    }
    
    
    
    @Transaction()
    public boolean exists(Context ctx, String eaId) {
        // Check if authorization information exists
        byte[] buffer = ctx.getStub().getState(eaId);
        return (buffer != null && buffer.length > 0);
    }




    public String getAuthKey(ChaincodeStub stub, String eaId, String aaId) {
        CompositeKey ck = stub.createCompositeKey(eaId, aaId);
        if (ck == null) {
            System.out.println("getAuthKey() stub function returned null, generating using constructor");
            ck = new CompositeKey(eaId, aaId);
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

    

    public static BigInteger getNumberId(final String value) {
        return new BigInteger(value.getBytes(Charset.availableCharsets().get("UTF-8")));
    }

    public String generatehashedID8PC(ChaincodeStub stub, String PC) {
        //Canonical identity = H(SUPI|y)
        // how much time it will take to generate H(SUPI | Y)
        //Assumption == the 5CN peer has access to SUPI and y
        //String ITSSID = SUPI + y; // in real implementation it should be the hash of the two values
        MessageDigest md = null;
        try {
        md = MessageDigest.getInstance("SHA-256");

            }catch(NoSuchAlgorithmException e) {
                 System.out.println("Something is wrong");
        }
       
        md.update(PC.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        String hex = String.format("%064x", new BigInteger(1, digest));
        
        
        return hex;
    }


    @Transaction()
    public void retrieveValidationResponse(Context ctx, String eaId, String aaid, String z) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);


        List<Map<String, Object>> validationObj = retriveValidationResponse(ctx, eaId, aaid,keytag);
        
        


    }


    @Transaction()
    
    public void uploadAuthorizationInfo(Context ctx, String eaId, String aaid, String ecsignature, String hashedID8_PC, String z) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);


        List<Map<String, Object>> validationObj = retriveValidationResponse(ctx, eaId, aaid,keytag);

        

        for (Map<String, Object> valresp : validationObj) {
            

            //from the validation response object you can access the status of the response ( if it is oK generate certificate else cancel request)
            String validResponseStatus = (String) valresp.get("status");
            if (!validResponseStatus.equals("RESPONSEOK")) {
                throw new ChaincodeException("The validation response for " + eaId + " and " + aaid+ "is in status" + validResponseStatus + " state. Expected RESPONSEOK status.");
            }

            ArrayList<String> confirmedattributes = (ArrayList<String>) valresp.get("confirmedattributes");

            //based on the confirmed attributes generate PC certificate locally and provide it to the requesting entity
            // after providing PC cerificates calculate their hashedID8 localy which will be stored as accumulator
            // as stated in ETSI ITS PKI an ITS station can request only one PC certificate at a time to avoid likage of information to the PCA

            Accumulator initacc = new Accumulator();
            String hashid8pc = generatehashedID8PC(stub,hashedID8_PC);
            BigInteger converted_hashedID8_PC = getNumberId(hashid8pc);
            

            int primesize = initacc.getPRIMESIZE();
            
            Pair<BigInteger> bigIntegerPair = Util.hashToPrime(converted_hashedID8_PC, primesize);
            BigInteger hashPrime = bigIntegerPair.getFirst();
            String b1String = hashPrime.toString();

            getupdateAuthInfoFunc(ctx, ecsignature, b1String);




        }
        

        
    }


    


    @Transaction()
    public void initiateRevocation(Context ctx, String hashedID8_PC) {
    
        ChaincodeStub stub = ctx.getStub();

        Accumulator initacc = new Accumulator();
        String hashid8pc = generatehashedID8PC(stub,hashedID8_PC);
        BigInteger converted_hashedID8_PC = getNumberId(hashid8pc);
        

        int primesize = initacc.getPRIMESIZE();
        
        Pair<BigInteger> bigIntegerPair = Util.hashToPrime(converted_hashedID8_PC, primesize);
        BigInteger hashPrime = bigIntegerPair.getFirst();
        String b1String = hashPrime.toString();
    
        
        getprocessRevocationFun(ctx, b1String);
         
        

    }


    private List<Map<String, Object>> retriveValidationResponse(Context ctx, String eaId, String aaid, String keytag) {
        
        // Look up the validation contract name
        ChaincodeStub stub = ctx.getStub();
        byte[] vcBytes = stub.getState(AuthConstants.validationContractIdKey);
        if (vcBytes == null || vcBytes.length == 0) {
            throw new ChaincodeException("No validation contract id recorded on ledger");
        }
        String validationChaincodeId = new String(vcBytes);

        
        String[] authArgs = new String[2];
        authArgs[0]= eaId;
        authArgs[1]= aaid;
        

        // Lookup the validation response info by invoking the validation smart contract
        Response validationResp = ctx.getStub().invokeChaincodeWithStringArgs(validationChaincodeId, AuthConstants.getvalidationFunc, eaId,aaid,keytag);
        if (validationResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking  " + validationChaincodeId + " chaincode, function " + AuthConstants.getvalidationFunc + ".");
        }

        String validation_info = validationResp.getStringPayload();


        if (validation_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate validation response record for the given request");
        }

        
        String validation_info2 = "["+validation_info+"]";
        

        if (validation_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate validation response for : " + eaId + ".");
        }

        


        
        //@SuppressWarnings("unchecked")
        List<Map<String, Object>> enrollmentObj = new ArrayList<Map<String, Object>>();

        try {

            
            enrollmentObj = mapper.readValue(validation_info2, new TypeReference<List<Map<String, Object>>>(){});

                    

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return enrollmentObj;
    }



    private void getupdateAuthInfoFunc(Context ctx, String eCsignature, String hashedid8ofPC) {
        
        // Look up the validation contract name
        ChaincodeStub stub = ctx.getStub();
        byte[] ecBytes = stub.getState(AuthConstants.enrollmentContractIdKey);
        if (ecBytes == null || ecBytes.length == 0) {
            throw new ChaincodeException("No validation contract id recorded on ledger");
        }
        String enrollmentChaincodeId = new String(ecBytes);

        

        // Lookup the validation response info by invoking the validation smart contract
        Response updateAuthResp = ctx.getStub().invokeChaincodeWithStringArgs(enrollmentChaincodeId, AuthConstants.getupdateAuthInfoFunc, eCsignature, hashedid8ofPC);
        if (updateAuthResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking  " + enrollmentChaincodeId + " chaincode, function " + AuthConstants.getupdateAuthInfoFunc + ".");
        }

        String updateAuthResp_info = updateAuthResp.getStringPayload();

        if (updateAuthResp_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate the update authorization function");
        }
    }





    private void getprocessRevocationFun(Context ctx, String hashedid8ofPC) {
    
    // Look up the validation contract name
    ChaincodeStub stub = ctx.getStub();
    byte[] ecBytes = stub.getState(AuthConstants.enrollmentContractIdKey);
    if (ecBytes == null || ecBytes.length == 0) {
        throw new ChaincodeException("No validation contract id recorded on ledger");
    }
    String enrollmentChaincodeId = new String(ecBytes);

    

    // Lookup the validation response info by invoking the validation smart contract
    Response updateAuthResp = ctx.getStub().invokeChaincodeWithStringArgs(enrollmentChaincodeId, AuthConstants.getprocessRevocationFun, hashedid8ofPC);
    if (updateAuthResp.getStatus() != Response.Status.SUCCESS) {
        throw new ChaincodeException("Error invoking  " + enrollmentChaincodeId + " chaincode, function " + AuthConstants.getupdateAuthInfoFunc + ".");
        
    }

        
    }


}




    








    
    
    
    




    
    
    

                           