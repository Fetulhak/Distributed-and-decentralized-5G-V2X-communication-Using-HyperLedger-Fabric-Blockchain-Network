/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.assettransfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public final class AssetTransferTest {

    private final class MockKeyValue implements KeyValue {

        private final String key;
        private final String value;

        MockKeyValue(final String key, final String value) {
            super();
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public String getStringValue() {
            return this.value;
        }

        @Override
        public byte[] getValue() {
            return this.value.getBytes();
        }

    }

    private final class MockAssetResultsIterator implements QueryResultsIterator<KeyValue> {

        private final List<KeyValue> assetList;

        MockAssetResultsIterator() {
            super();

            assetList = new ArrayList<KeyValue>();

            assetList.add(new MockKeyValue("asset1",
                    "{ \"assetID\": \"asset1\", \"color\": \"blue\", \"size\": 5, \"owner\": \"Tomoko\", \"appraisedValue\": 300 }"));
            assetList.add(new MockKeyValue("asset2",
                    "{ \"assetID\": \"asset2\", \"color\": \"red\", \"size\": 5,\"owner\": \"Brad\", \"appraisedValue\": 400 }"));
            assetList.add(new MockKeyValue("asset3",
                    "{ \"assetID\": \"asset3\", \"color\": \"green\", \"size\": 10,\"owner\": \"Jin Soo\", \"appraisedValue\": 500 }"));
            assetList.add(new MockKeyValue("asset4",
                    "{ \"assetID\": \"asset4\", \"color\": \"yellow\", \"size\": 10,\"owner\": \"Max\", \"appraisedValue\": 600 }"));
            assetList.add(new MockKeyValue("asset5",
                    "{ \"assetID\": \"asset5\", \"color\": \"black\", \"size\": 15,\"owner\": \"Adrian\", \"appraisedValue\": 700 }"));
            assetList.add(new MockKeyValue("asset6",
                    "{ \"assetID\": \"asset6\", \"color\": \"white\", \"size\": 15,\"owner\": \"Michel\", \"appraisedValue\": 800 }"));
        }

        @Override
        public Iterator<KeyValue> iterator() {
            return assetList.iterator();
        }

        @Override
        public void close() throws Exception {
            // do nothing
        }

    }

   
}
