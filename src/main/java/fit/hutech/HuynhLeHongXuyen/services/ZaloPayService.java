package fit.hutech.HuynhLeHongXuyen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class ZaloPayService {

    @Value("${zalopay.app-id:}")
    private String appId;

    @Value("${zalopay.key1:}")
    private String key1;

    @Value("${zalopay.key2:}")
    private String key2;

    @Value("${zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String endpoint;

    @Value("${zalopay.callback-url:http://localhost:8081/payment/zalopay/callback}")
    private String callbackUrl;

    @Value("${zalopay.redirect-url:http://localhost:8081/payment/zalopay/result}")
    private String redirectUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create ZaloPay order
     * @param orderId app order code
     * @param amount amount in VND
     * @param description order description
     * @return redirect URL to ZaloPay payment page, or null if failed
     */
    public String createPayment(String orderId, long amount, String description) {
        try {
            String appTransId = getCurrentDateString("yyMMdd") + "_" + orderId;
            long appTime = System.currentTimeMillis();

            // Build embed_data with proper JSON encoding
            Map<String, String> embedDataMap = new LinkedHashMap<>();
            embedDataMap.put("redirecturl", redirectUrl);
            String embedDataJson = objectMapper.writeValueAsString(embedDataMap);

            Map<String, Object> order = new LinkedHashMap<>();
            order.put("app_id", Integer.parseInt(appId));
            order.put("app_trans_id", appTransId);
            order.put("app_time", appTime);
            order.put("app_user", "bookstore_user");
            order.put("amount", amount);
            order.put("description", description);
            order.put("bank_code", "");
            order.put("item", "[]");
            order.put("embed_data", embedDataJson);
            order.put("callback_url", callbackUrl);

            // Calculate MAC: appId|appTransId|appUser|amount|appTime|embedData|item
            String data = appId + "|" + appTransId + "|bookstore_user|" + amount + "|"
                    + appTime + "|" + embedDataJson + "|[]";
            String mac = hmacSHA256(key1, data);
            order.put("mac", mac);

            log.info("[ZaloPay] MAC input: {}", data);

            String jsonBody = objectMapper.writeValueAsString(order);
            log.info("[ZaloPay] Request: {}", jsonBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, String.class);

            log.info("[ZaloPay] Response: {}", response.getBody());

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            int returnCode = jsonResponse.get("return_code").asInt();

            if (returnCode == 1) {
                // Use order_token with sandbox web payment page for desktop browser
                // /openinapp is for mobile deep link and hangs on desktop browsers
                // /pay?token= is the correct web payment gateway URL
                JsonNode orderToken = jsonResponse.get("order_token");
                if (orderToken != null && !orderToken.asText().isEmpty()) {
                    String webUrl = "https://sbgateway.zalopay.vn/pay?token=" + orderToken.asText();
                    log.info("[ZaloPay] Using sandbox web payment URL: {}", webUrl);
                    return webUrl;
                }
                String orderUrl = jsonResponse.get("order_url").asText();
                log.info("[ZaloPay] Fallback to order_url: {}", orderUrl);
                return orderUrl;
            } else {
                log.error("[ZaloPay] Payment creation failed. return_code={}, return_message={}",
                        returnCode, jsonResponse.get("return_message").asText());
                return null;
            }
        } catch (Exception e) {
            log.error("[ZaloPay] Error creating payment: ", e);
            return null;
        }
    }

    /**
     * Verify ZaloPay callback MAC using key2
     */
    public boolean verifyCallback(String dataStr, String reqMac) {
        try {
            String computedMac = hmacSHA256(key2, dataStr);
            return computedMac.equals(reqMac);
        } catch (Exception e) {
            log.error("[ZaloPay] Error verifying callback: ", e);
            return false;
        }
    }

    private String getCurrentDateString(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(new Date());
    }

    private String hmacSHA256(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
