package com.hirecheck.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageKitService {
    @Value("${app.imagekit.private-key:}") private String privateKey;

    public Map<String, Object> getAuthParameters() {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expire = (System.currentTimeMillis() / 1000) + 2400;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String signature = HexFormat.of().formatHex(mac.doFinal((token + expire).getBytes(StandardCharsets.UTF_8)));
            return Map.of("token", token, "expire", expire, "signature", signature);
        } catch (Exception e) {
            throw new RuntimeException("ImageKit auth failed", e);
        }
    }
}
