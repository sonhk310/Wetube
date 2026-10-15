package com.wetube.wetube_service.utility;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacSHA256Util {
    public static String sign(String rawData, String secretKey) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(rawData.getBytes());
        return bytesToHex(rawHmac);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }
}
