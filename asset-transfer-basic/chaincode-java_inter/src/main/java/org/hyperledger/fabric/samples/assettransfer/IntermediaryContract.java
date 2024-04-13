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


import com.owlike.genson.Genson;

import java.nio.charset.Charset;
import java.util.Random;

import java.util.ArrayList;
import java.math.BigInteger;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


@Contract(name = "IntermediaryContract",
    info = @Info(title = "IntermediaryContract",
                description = "Intermediary SmartContract",
                version = "1.0.0",
                license =
                        @License(name = "Apache-2.0",
                                url = ""),
                contact =
                        @Contact(email = "IntermediaryContract@example.com",
                                name = "intermediarySC",
                                url = "http://IntermediaryContract.me")))
                                
                                
@Default
public class IntermediaryContract implements ContractInterface {

    private final Genson genson = new Genson();
    ObjectMapper mapper = new ObjectMapper();

    public  IntermediaryContract() {
    }
	    
    
    
    @Transaction()
    public void init(Context ctx, String enrollmentContractId, String enrollmentchannelName, String v2xContractId, String v2xChannelName, String enrollmentContractIdVis, String enrollmentchannelNameVis) {
        
        System.out.println("Initializing the validation contract");
        ChaincodeStub stub = ctx.getStub();
        stub.putState(IntermediaryConstants.enrollmentContractIdKey, enrollmentContractId.getBytes(UTF_8));
        stub.putState(IntermediaryConstants.enrollmentChannelDANameKey, enrollmentchannelName.getBytes(UTF_8));
        stub.putState(IntermediaryConstants.enrollmentContractIdKeyVis, enrollmentContractIdVis.getBytes(UTF_8));
        stub.putState(IntermediaryConstants.enrollmentChannelDANameKeyVis, enrollmentchannelNameVis.getBytes(UTF_8));
        stub.putState(IntermediaryConstants.V2xContractIdKey, v2xContractId.getBytes(UTF_8));
        stub.putState(IntermediaryConstants.v2xChannelDANameKey, v2xChannelName.getBytes(UTF_8));


    }
    
    
    
    @Transaction()
    public boolean exists(Context ctx, String id) {
        
        byte[] buffer = ctx.getStub().getState(id);
        return (buffer != null && buffer.length > 0);
    }

    public String getIntermediaryKey(ChaincodeStub stub, String home_eaId, String visited_eaId, String keytag) {
        CompositeKey ck = stub.createCompositeKey(home_eaId, visited_eaId, keytag);
        if (ck == null) {
            System.out.println("getValKey() stub function returned null, generating using constructor");
            ck = new CompositeKey(home_eaId, visited_eaId);
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

    


    // This transaction or function is excuted by ECA peer not of the new ITS domain at the border to put the cross border request as transaction on the blockchain
    @Transaction()
    public String crossBorderAuthValidationRequest(Context ctx, String home_eaId, String visited_eaId, String z, String hashedID8_EC) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);
        
        String InterKey = getIntermediaryKey(stub, home_eaId, visited_eaId, keytag);
        boolean exists = exists(ctx, InterKey);
        String PCAMSPID = AccessControlUtils.GetClientMspId(ctx);
        if (exists) {
            throw new ChaincodeException("The cross border authorization validation request for " + InterKey + " already exists and the current requesting PCA msp is " + PCAMSPID );
        }

        
        
        IntermediaryInfo interreq = new IntermediaryInfo(home_eaId, visited_eaId, hashedID8_EC, new HashMap<String,Object>(), "REQUESTED");
        
        String interreqStr = interreq.toJSONString();

        stub.putState(InterKey, interreqStr.getBytes(UTF_8));

        return interreqStr;
    }
    
    
    
    @Transaction()
    public String validateCrossborderAuthRequest(Context ctx, String home_eaId, String visited_eaId, String z) { 
    
        
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);
        String InterKey = getIntermediaryKey(stub, home_eaId, visited_eaId, keytag);
        boolean exists = exists(ctx, InterKey);
        if (!exists) {
            throw new ChaincodeException("The cross border authorization validation request for" + InterKey + " does not exists.");
        }
        byte[] CBvalReqBytes = stub.getState(InterKey);
        IntermediaryInfo CBvalReq = IntermediaryInfo.fromJSONString(new String(CBvalReqBytes));
        String crossBordervalidationRequestStatus = CBvalReq.getStatus();

        if (!crossBordervalidationRequestStatus.equals("REQUESTED")) {
            throw new ChaincodeException("The cross border validation request for " + InterKey + " is in status" + crossBordervalidationRequestStatus + " state. Expected REQUESTED status.");
        }

        String hashedID8_EC = CBvalReq.gethashedID8EC();

        
        Map<String, Object> enrollmentObj = getEnrollmentInfo(ctx, hashedID8_EC);
        

        

 
        String itssid = (String) enrollmentObj.get("itssid");
        
        Map<String, Object> v2xInfoObj = getV2xInfo(ctx, itssid);

        //extract the V2X Information and check for its validity before sending cross border validation response 
        String statusOfV2Xservice = (String) v2xInfoObj.get("status");
        
        if (!statusOfV2Xservice.equals("OK")) {
            throw new ChaincodeException("The V2X service status for " + itssid + " is in status " + statusOfV2Xservice + " state. Expected OK status.");
        }

        // if everything is OK i.e after checking the enrollemnt status of the requested hashedID8_EC and the V2X service the home ECA respond either positive or negative 
        
        CBvalReq.setEnrollmentInfo(enrollmentObj);
        CBvalReq.setStatus("OK");

        String crossBordervalresStr = CBvalReq.toJSONString();
        
        stub.putState(InterKey, crossBordervalresStr.getBytes(UTF_8));

        return crossBordervalresStr;
    }

    


    @Transaction()
    public void initiateEnrollmentofNewITSS(Context ctx, String home_eaId, String visited_eaId, String z) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);

        String InterKey = getIntermediaryKey(stub, home_eaId, visited_eaId, keytag);
        boolean exists = exists(ctx, InterKey);
        if (!exists) {
            throw new ChaincodeException("The cross border authorization validation request for" + InterKey + " does not exists.");
        }
        byte[] CBvalReqBytes = stub.getState(InterKey);
        
        String CRBvalRespStr = new String(CBvalReqBytes);
        



        getnewITSSenrollmentFun(ctx,CRBvalRespStr);
         
        

    }


    //this transaction can be executed by both cross border ECA peer nodes
    @Transaction()
    public String getcrossborderValidationResponse(Context ctx, String home_eaId, String visited_eaId, String z) {
    
        ChaincodeStub stub = ctx.getStub();
        int rand=Integer.parseInt(z);  
        String keytag = getRandom(stub,rand);

        String InterKey = getIntermediaryKey(stub, home_eaId, visited_eaId, keytag);
        boolean exists = exists(ctx, InterKey);
        if (!exists) {
            throw new ChaincodeException("The cross border authorization validation request for" + InterKey + " does not exists.");
        }
        byte[] CBRvalResBytes = stub.getState(InterKey);
        IntermediaryInfo inter = IntermediaryInfo.fromJSONString(new String(CBRvalResBytes));
        String CBRvalidationResponseStatus = inter.getStatus();

        if (!CBRvalidationResponseStatus.equals("OK")) {
            throw new ChaincodeException("The cross border request validation response for " + InterKey + " is in status" + CBRvalidationResponseStatus + " state. Expected OK status.");
        }


        String CRBvalRespStr = new String(CBRvalResBytes);
   
        return CRBvalRespStr;


    }

    



    private Map<String, Object> getEnrollmentInfo(Context ctx, String hashedID8_EC) {
        // Look up the enrollment contract name
        ChaincodeStub stub = ctx.getStub();
        byte[] ecBytes = stub.getState(IntermediaryConstants.enrollmentContractIdKey);
        if (ecBytes == null || ecBytes.length == 0) {
            throw new ChaincodeException("No enrollemnt contract id recorded on ledger");
        }
        String enrollmentChaincodeId = new String(ecBytes);

        byte[] echBytes = stub.getState(IntermediaryConstants.enrollmentChannelDANameKey);
        if (echBytes == null || echBytes.length == 0) {
            throw new ChaincodeException("No shipping channel name recorded on ledger");
        }
        String enrollmentChannel = new String(echBytes);

        

        // Lookup the enrollment info by invoking the enrollemtn chaincode
        ArrayList<String> crossBordervalReqArgs = new ArrayList<String>();
        crossBordervalReqArgs.add(IntermediaryConstants.getEnrollmentInfoFunc);
        crossBordervalReqArgs.add(hashedID8_EC);
        Response enrollmentResp = ctx.getStub().invokeChaincodeWithStringArgs(enrollmentChaincodeId, crossBordervalReqArgs, enrollmentChannel);
        if (enrollmentResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + enrollmentChaincodeId + "' chaincode, function '" + IntermediaryConstants.getEnrollmentInfoFunc + "'");
        }

        String enrollment_info = enrollmentResp.getStringPayload();

        
        System.out.println(enrollment_info);

        
        if (enrollment_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate enrollment for : " + hashedID8_EC + " . May be it is wrong hashedID8_EC or the Enrolment information is revoked");
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
        byte[] v2xcBytes = stub.getState(IntermediaryConstants.V2xContractIdKey);
        if (v2xcBytes == null || v2xcBytes.length == 0) {
            throw new ChaincodeException("No v2xcontract id recorded on ledger");
        }
        String v2xChaincodeId = new String(v2xcBytes);

        byte[] v2xchBytes = stub.getState(IntermediaryConstants.v2xChannelDANameKey);
        if (v2xchBytes == null || v2xchBytes.length == 0) {
            throw new ChaincodeException("No V2X channel name recorded on ledger");
        }
        String v2xChannel = new String(v2xchBytes);

        
        
        // Lookup the v2x info by invoking the v2x chaincode
        ArrayList<String> crossBordervalReqArgs = new ArrayList<String>();
        crossBordervalReqArgs.add(IntermediaryConstants.getV2xfoFunc);
        crossBordervalReqArgs.add(itssid);

        
        Response v2xInfoResp = ctx.getStub().invokeChaincodeWithStringArgs(v2xChaincodeId, crossBordervalReqArgs, v2xChannel);
        if (v2xInfoResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + v2xChaincodeId + "' chaincode, function '" + IntermediaryConstants.getV2xfoFunc + "'");
        }

        String v2x_info = v2xInfoResp.getStringPayload();

        
        System.out.println(v2x_info);

        
        if (v2x_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate v2x service record for : " + itssid + " . May be subscription is revoked or it is wrong itssid");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> v2xInfoObj = genson.deserialize(v2x_info, Map.class);
        return v2xInfoObj;
    }



    private void getnewITSSenrollmentFun(Context ctx, String interinfo) {
    
    // Look up the validation contract name
    ChaincodeStub stub = ctx.getStub();
    byte[] ecBytes = stub.getState(IntermediaryConstants.enrollmentContractIdKeyVis);
    if (ecBytes == null || ecBytes.length == 0) {
        throw new ChaincodeException("No enrollemnt contract id recorded on ledger");
    }
    String enrollmentChaincodeId = new String(ecBytes);

    byte[] echBytes = stub.getState(IntermediaryConstants.enrollmentChannelDANameKeyVis);
    if (echBytes == null || echBytes.length == 0) {
        throw new ChaincodeException("No enrollment channel name recorded on ledger");
    }
    String enrollmentChannel = new String(echBytes);

    

    // Lookup the enrollment info by invoking the enrollemtn chaincode
    ArrayList<String> crossBordervalReqArgs = new ArrayList<String>();
    crossBordervalReqArgs.add(IntermediaryConstants.getnewITSSenrollingfoFunc);
    crossBordervalReqArgs.add(interinfo);


    Response enrollmentResp = ctx.getStub().invokeChaincodeWithStringArgs(enrollmentChaincodeId, crossBordervalReqArgs, enrollmentChannel);
        if (enrollmentResp.getStatus() != Response.Status.SUCCESS) {
            throw new ChaincodeException("Error invoking '" + enrollmentChaincodeId + "' chaincode, function '" + IntermediaryConstants.getnewITSSenrollingfoFunc + "'");
        }

        String enrollment_info = enrollmentResp.getStringPayload();
        
        if (enrollment_info.isEmpty()) {
            throw new ChaincodeException("Unable to locate enrollment. May be it is wrong hashedID8_EC or the Enrolment information is revoked");
        }
         } 
}


    
    