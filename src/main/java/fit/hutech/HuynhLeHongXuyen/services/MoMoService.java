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
import java.util.*;

@Service
@Slf4j
public class MoMoService {

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.return-url}")
    private String returnUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    @Value("${momo.query-url}")
    private String queryUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create MoMo payment order
     * 
     * @param orderId   unique order ID
     * @param amount    payment amount (VND)
     * @param orderInfo description of the order
     * @return payUrl - URL to redirect user to MoMo payment page, or null if failed
     */
    public String createPayment(String orderId, long amount, String orderInfo) {
        try {
            String requestId = UUID.randomUUID().toString();
            String extraData = "";
            String requestType = "captureWallet";

            // Build raw signature string (alphabetical order)
            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + amount
                    + "&extraData=" + extraData
                    + "&ipnUrl=" + ipnUrl
                    + "&orderId=" + orderId
                    + "&orderInfo=" + orderInfo
                    + "&partnerCode=" + partnerCode
                    + "&redirectUrl=" + returnUrl
                    + "&requestId=" + requestId
                    + "&requestType=" + requestType;

            String signature = signHmacSHA256(rawSignature, secretKey);
            log.info("[MoMo] rawSignature: {}", rawSignature);
            log.info("[MoMo] signature: {}", signature);

            // Build request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("amount", amount);
            requestBody.put("orderId", orderId);
            requestBody.put("orderInfo", orderInfo);
            requestBody.put("redirectUrl", returnUrl);
            requestBody.put("ipnUrl", ipnUrl);
            requestBody.put("extraData", extraData);
            requestBody.put("requestType", requestType);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("[MoMo] Request body: {}", jsonBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, String.class);

            log.info("[MoMo] Response: {}", response.getBody());

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            int resultCode = jsonResponse.get("resultCode").asInt();

            if (resultCode == 0) {
                return jsonResponse.get("payUrl").asText();
            } else {
                log.error("[MoMo] Payment creation failed. resultCode={}, message={}",
                        resultCode, jsonResponse.get("message").asText());
                return null;
            }
        } catch (Exception e) {
            log.error("[MoMo] Error creating payment: ", e);
            return null;
        }
    }

    /**
     * Query MoMo transaction status
     * 
     * @param orderId the order ID to query
     * @return resultCode (0 = success, 1000 = pending, etc.)
     */
    public int queryTransactionStatus(String orderId) {
        try {
            String requestId = UUID.randomUUID().toString();

            String rawSignature = "accessKey=" + accessKey
                    + "&orderId=" + orderId
                    + "&partnerCode=" + partnerCode
                    + "&requestId=" + requestId;

            String signature = signHmacSHA256(rawSignature, secretKey);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("partnerCode", partnerCode);
            requestBody.put("accessKey", accessKey);
            requestBody.put("requestId", requestId);
            requestBody.put("orderId", orderId);
            requestBody.put("signature", signature);
            requestBody.put("lang", "vi");

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    queryUrl, HttpMethod.POST, entity, String.class);

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return jsonResponse.get("resultCode").asInt();
        } catch (Exception e) {
            log.error("[MoMo] Error querying status: ", e);
            return -1;
        }
    }

    /**
     * Verify MoMo IPN/callback signature
     */
    public boolean verifySignature(Map<String, String> params) {
        try {
            String receivedSignature = params.get("signature");
            if (receivedSignature == null)
                return false;

            String rawSignature = "accessKey=" + accessKey
                    + "&amount=" + params.get("amount")
                    + "&extraData=" + params.get("extraData")
                    + "&message=" + params.get("message")
                    + "&orderId=" + params.get("orderId")
                    + "&orderInfo=" + params.get("orderInfo")
                    + "&orderType=" + params.get("orderType")
                    + "&partnerCode=" + params.get("partnerCode")
                    + "&payType=" + params.get("payType")
                    + "&requestId=" + params.get("requestId")
                    + "&responseTime=" + params.get("responseTime")
                    + "&resultCode=" + params.get("resultCode")
                    + "&transId=" + params.get("transId");

            String computedSignature = signHmacSHA256(rawSignature, secretKey);
            return computedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("[MoMo] Error verifying signature: ", e);
            return false;
        }
    }

    /**
     * HMAC SHA256 signing
     */
    private String signHmacSHA256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKeySpec);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
