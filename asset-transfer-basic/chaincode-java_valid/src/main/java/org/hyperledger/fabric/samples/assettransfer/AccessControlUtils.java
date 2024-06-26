/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import java.util.Arrays;
import java.util.Map;

import java.util.HashMap;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.contract.Context;


@DataType()
class ACLSubject3 {
    @Property()
    private String mspId;

    @Property()
    private String role;

    public ACLSubject3(String mspId, String role) {
        this.mspId = mspId;
        this.role = role;
    }

    public String getMspId() {
        return mspId;
    }

    public void setMspId(String mspId) {
        this.mspId = mspId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == ACLSubject3.class) {
            ACLSubject3 aclsubject1 = (ACLSubject3) obj;
            return aclsubject1.getMspId().equals(this.getMspId()) && aclsubject1.getRole().equals(this.getRole());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.getMspId() + "," + this.getRole()).hashCode();
    }

    @Override
    public String toString() {
        return "MSP ID: " + this.getMspId() + ", Role: " + this.getRole();
    }
 }


public class AccessControlUtils {
    public final static String BUSINESS_ROLE_ATTR = "BUSINESS_ROLE";
    private final static Map<ACLSubject3,String[]> aclRules = new HashMap<ACLSubject3,String[]>();
    

    public static String GetClientMspId(Context ctx) {
        return ctx.getClientIdentity().getMSPID();
    }

    public static String GetClientRole(Context ctx) {
        return ctx.getClientIdentity().getAttributeValue(AccessControlUtils.BUSINESS_ROLE_ATTR);
    }


    public static boolean checkAccess(Context ctx, String mspId, String role, String function) {
        ACLSubject3 aclsubject = new ACLSubject3(mspId, role);
        if (!aclRules.containsKey(aclsubject)) {
            throw new ChaincodeException("The participant " + mspId + " role " + role + " is not recognized");
        } else {
            return !Arrays.asList(aclRules.get(aclsubject)).contains(function);
        }
    }

   

}