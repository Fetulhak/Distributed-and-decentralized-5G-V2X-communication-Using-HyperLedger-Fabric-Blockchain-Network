package org.hyperledger.fabric.samples.assettransfer;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import java.math.BigInteger;
import java.util.Random;


public class Util {
    private static final int PRIME_CERTAINTY = 5;

    public static String toHexString(byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }

    public static BigInteger generateLargePrime(int bitLength) {
        Random random = new java.util.Random();
        random.setSeed(3456789);
        return BigInteger.probablePrime(bitLength, random);
    }

    public static BigInteger generateLargePrime2(int bitLength) {
        Random random = new java.util.Random();
        random.setSeed(34569);
        return BigInteger.probablePrime(bitLength, random);
    }

    public static Pair<BigInteger> generateTwoLargeDistinctPrimes(int bitLength) {
        BigInteger first = generateLargePrime(bitLength);
        while (true) {
            BigInteger second = generateLargePrime2(bitLength);
            if (first.compareTo(second) != 0) {
                return new Pair<>(first, second);
            }
        }
    }

    public static Pair<BigInteger> hashToPrime(BigInteger x, int bitLength) {
        return hashToPrime(x, bitLength, BigInteger.ZERO);
    }

    public static Pair<BigInteger> hashToPrime(BigInteger x) {
        return hashToPrime(x, 120, BigInteger.ZERO);
    }

    
    public static Pair<BigInteger> hashToPrime(BigInteger x, int bitLength, BigInteger initNonce) {
        BigInteger nonce = initNonce;
        while (true) {
            
            BigInteger num = hashToLength(x.add(nonce), bitLength);
            if (num.isProbablePrime(PRIME_CERTAINTY)) {
                return new Pair<>(num, nonce);
            }
            nonce = nonce.add(BigInteger.ONE);
        }
    }

    
    
    public static BigInteger hashToLength(BigInteger x, int bitLength) {
        StringBuilder randomHexString = new StringBuilder();
        int           numOfBlocks     = (int) Math.ceil(bitLength / 256.00);

        for (int i = 0; i < numOfBlocks; i++) {
            final BigInteger bigIntI = new BigInteger(Integer.toString(i));
            randomHexString.append(
                    toHexString(Hashing.sha256().hashBytes(
                            (x.add(bigIntI))
                                    .toString(10)
                                    .getBytes()).asBytes()));

        }

        if (bitLength % 256 > 0) {
            randomHexString = new StringBuilder(randomHexString.substring((bitLength % 256) / 4));
        }
        return new BigInteger(randomHexString.toString(), 16);
    }

}
