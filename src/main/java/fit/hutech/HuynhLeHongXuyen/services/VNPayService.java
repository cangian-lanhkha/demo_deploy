package fit.hutech.HuynhLeHongXuyen.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPayService {

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayUrl;

    @Value("${vnpay.return-url:http://localhost:8081/payment/vnpay/callback}")
    private String returnUrl;

    @Value("${vnpay.api-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create VNPay payment URL
     * @param orderId unique order code
     * @param amount amount in VND (will be multiplied by 100 per VNPay spec)
     * @param orderInfo description
     * @param ipAddress client IP
     * @return redirect URL to VNPay payment page
     */
    public String createPaymentUrl(String orderId, long amount, String orderInfo, String ipAddress) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", tmnCode);
            params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay requires amount * 100
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", orderId);
            params.put("vnp_OrderInfo", orderInfo);
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", ipAddress);

            // Vietnam timezone: Asia/Ho_Chi_Minh (UTC+7)
            // Note: Etc/GMT+7 is actually UTC-7 (POSIX convention is inverted)
            TimeZone vnTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            Calendar calendar = Calendar.getInstance(vnTz);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(vnTz);
            String createDate = formatter.format(calendar.getTime());
            params.put("vnp_CreateDate", createDate);

            calendar.add(Calendar.MINUTE, 15);
            String expireDate = formatter.format(calendar.getTime());
            params.put("vnp_ExpireDate", expireDate);

            // Build hash data and query string from sorted params
            // VNPay official docs: hashData uses URL-encoded values (UTF-8), sorted a-z
            StringBuilder query = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!first) {
                        hashData.append('&');
                        query.append('&');
                    }
                    // hashData: field name NOT encoded, value IS URL-encoded (per VNPay spec)
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    // query: both name and value URL-encoded
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    first = false;
                }
            }

            log.info("[VNPay] HashData: {}", hashData);
            log.info("[VNPay] HashSecret: {}...{}", hashSecret.substring(0, 4), hashSecret.substring(hashSecret.length() - 4));
            String secureHash = hmacSHA512(hashSecret, hashData.toString());
            log.info("[VNPay] SecureHash: {}", secureHash);
            query.append("&vnp_SecureHash=").append(secureHash);

            String paymentUrl = vnpayUrl + "?" + query;
            log.info("[VNPay] Payment URL created for order: {}", orderId);
            log.info("[VNPay] TmnCode={}, ReturnUrl={}", tmnCode, returnUrl);
            log.info("[VNPay] Full URL length={}", paymentUrl.length());
            return paymentUrl;
        } catch (Exception e) {
            log.error("[VNPay] Error creating payment URL: ", e);
            return null;
        }
    }

    /**
     * Verify VNPay callback signature
     */
    public boolean verifySignature(Map<String, String> params) {
        try {
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) return false;

            Map<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.remove("vnp_SecureHash");
            sortedParams.remove("vnp_SecureHashType");

            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    if (!first) hashData.append('&');
                    // URL-encode values to match hash data format (Spring MVC decoded them)
                    hashData.append(entry.getKey()).append('=')
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                    first = false;
                }
            }

            String computedHash = hmacSHA512(hashSecret, hashData.toString());
            return computedHash.equalsIgnoreCase(vnpSecureHash);
        } catch (Exception e) {
            log.error("[VNPay] Error verifying signature: ", e);
            return false;
        }
    }

    /**
     * Check if payment was successful from callback params
     */
    public boolean isPaymentSuccess(Map<String, String> params) {
        return "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
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
