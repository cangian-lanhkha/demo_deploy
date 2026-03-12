package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.CartItem;
import fit.hutech.HuynhLeHongXuyen.entities.Coupon;
import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.services.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final SepayService sepayService;
    private final CartService cartService;
    private final InvoiceService invoiceService;
    private final CouponService couponService;
    private final MoMoService moMoService;
    private final VNPayService vnPayService;
    private final ZaloPayService zaloPayService;
    private final PaymentTransactionService txService;
    private final BookService bookService;

    // ========== Unified Checkout Entry Point ==========

    @PostMapping("/checkout")
    public String showPayment(@RequestParam String customerName, @RequestParam String phone,
            @RequestParam String email, @RequestParam String address,
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false, defaultValue = "qr") String paymentMethod,
            HttpSession session, HttpServletRequest request, Model model,
            RedirectAttributes redirectAttributes) {
        List<CartItem> cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            log.warn("Checkout failed: cart is empty");
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống, vui lòng thêm sách trước khi thanh toán");
            return "redirect:/cart";
        }

        // Validate stock availability before checkout
        for (CartItem item : cart) {
            if (!bookService.hasStock(item.getBook().getId(), item.getQuantity())) {
                log.warn("Checkout failed: book '{}' (id={}) insufficient stock for qty={}",
                        item.getBook().getTitle(), item.getBook().getId(), item.getQuantity());
                redirectAttributes.addFlashAttribute("error",
                        "Sách '" + item.getBook().getTitle() + "' không đủ số lượng trong kho");
                return "redirect:/cart";
            }
        }
        double total = cartService.getTotal(session);
        double discount = 0;
        Coupon appliedCoupon = null;
        String couponMessage = null;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            CouponService.CouponResult result = couponService.applyCoupon(couponCode, total);
            if (result.success()) {
                discount = result.discount();
                appliedCoupon = result.coupon();
                couponMessage = result.message();
            } else {
                couponMessage = result.message();
            }
        }
        double finalTotal = Math.max(total - discount, 0);
        String orderCode = "DH" + System.currentTimeMillis();

        // Save payment info to session
        session.setAttribute("payment_customerName", customerName);
        session.setAttribute("payment_phone", phone);
        session.setAttribute("payment_email", email);
        session.setAttribute("payment_address", address);
        session.setAttribute("payment_orderCode", orderCode);
        session.setAttribute("payment_total", finalTotal);
        session.setAttribute("payment_originalTotal", total);
        session.setAttribute("payment_discount", discount);
        if (appliedCoupon != null) {
            session.setAttribute("payment_couponId", appliedCoupon.getId());
            session.setAttribute("payment_couponCode", appliedCoupon.getCode());
        }

        // Route to the correct payment gateway
        return switch (paymentMethod) {
            case "momo" -> processMoMoPayment(orderCode, finalTotal, cart, session, model,
                    customerName, phone, email, address, total, discount, couponCode, couponMessage, appliedCoupon);
            case "vnpay" -> processVNPayPayment(orderCode, finalTotal, session, request, redirectAttributes);
            case "zalopay" -> processZaloPayPayment(orderCode, finalTotal, session, redirectAttributes);
            case "cod" -> processCODPayment(orderCode, finalTotal, session, model);
            default -> processQRPayment(orderCode, finalTotal, cart, session, model,
                    customerName, phone, email, address, total, discount, couponCode, couponMessage, appliedCoupon);
        };
    }

    // ========== QR Bank Transfer (SePay) ==========

    private String processQRPayment(String orderCode, double finalTotal, List<CartItem> cart,
            HttpSession session, Model model,
            String customerName, String phone, String email, String address,
            double total, double discount, String couponCode, String couponMessage, Coupon appliedCoupon) {
        String qrCodeUrl = sepayService.generateQRCodeUrl(finalTotal, orderCode);
        model.addAttribute("cart", cart);
        model.addAttribute("total", total);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("qrCodeUrl", qrCodeUrl);
        model.addAttribute("customerName", customerName);
        model.addAttribute("phone", phone);
        model.addAttribute("email", email);
        model.addAttribute("address", address);
        model.addAttribute("accountNumber", sepayService.getAccountNumber());
        model.addAttribute("bankCode", sepayService.getBankCode());
        model.addAttribute("accountName", sepayService.getAccountName());
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("couponMessage", couponMessage);
        model.addAttribute("appliedCoupon", appliedCoupon);
        return "payment/qr-checkout";
    }

    @GetMapping("/check-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(HttpSession session) {
        String orderCode = (String) session.getAttribute("payment_orderCode");
        Double total = (Double) session.getAttribute("payment_total");
        if (orderCode == null || total == null)
            return ResponseEntity.ok(Map.of("paid", false, "message", "Không tìm thấy thông tin đơn hàng"));
        boolean paid = sepayService.checkPaymentStatus(orderCode, total);
        return paid ? ResponseEntity.ok(Map.of("paid", true, "message", "Thanh toán thành công!"))
                : ResponseEntity.ok(Map.of("paid", false, "message", "Đang chờ thanh toán..."));
    }

    @PostMapping("/confirm")
    public String confirmPayment(HttpSession session) {
        String orderCode = (String) session.getAttribute("payment_orderCode");
        Double total = (Double) session.getAttribute("payment_total");
        if (orderCode == null || total == null)
            return "redirect:/cart";
        if (!sepayService.checkPaymentStatus(orderCode, total)) {
            session.setAttribute("qr_result_success", false);
            session.setAttribute("qr_result_message", "Chưa nhận được thanh toán. Vui lòng kiểm tra lại.");
            return "redirect:/payment/qr/result";
        }

        String customerName = (String) session.getAttribute("payment_customerName");
        Double discount = (Double) session.getAttribute("payment_discount");

        Invoice invoice = invoiceService.createInvoice(customerName,
                (String) session.getAttribute("payment_phone"),
                (String) session.getAttribute("payment_email"),
                (String) session.getAttribute("payment_address"),
                orderCode, PaymentMethod.QR_BANK, session);
        markCouponUsed(session);
        if (invoice != null) {
            txService.createTransaction(invoice, PaymentMethod.QR_BANK, total, orderCode);
        }

        session.setAttribute("qr_result_success", true);
        session.setAttribute("qr_result_orderCode", orderCode);
        session.setAttribute("qr_result_total", total);
        session.setAttribute("qr_result_discount", discount);
        session.setAttribute("qr_result_customerName", customerName);

        clearPaymentSession(session);
        return "redirect:/payment/qr/result";
    }

    @GetMapping("/qr/result")
    public String qrResult(HttpSession session, Model model) {
        Boolean success = (Boolean) session.getAttribute("qr_result_success");
        model.addAttribute("success", success != null ? success : false);
        model.addAttribute("orderCode", session.getAttribute("qr_result_orderCode"));
        model.addAttribute("total", session.getAttribute("qr_result_total"));
        model.addAttribute("discount", session.getAttribute("qr_result_discount"));
        model.addAttribute("customerName", session.getAttribute("qr_result_customerName"));
        model.addAttribute("message", session.getAttribute("qr_result_message"));

        for (String key : List.of("qr_result_success", "qr_result_orderCode", "qr_result_total",
                "qr_result_discount", "qr_result_customerName", "qr_result_message"))
            session.removeAttribute(key);

        return "payment/qr-result";
    }

    // ========== MoMo Payment ==========

    private String processMoMoPayment(String orderCode, double finalTotal, List<CartItem> cart,
            HttpSession session, Model model,
            String customerName, String phone, String email, String address,
            double total, double discount, String couponCode, String couponMessage, Coupon appliedCoupon) {
        long amount = Math.round(finalTotal);
        String orderInfo = "BookStore - Thanh toan don hang " + orderCode;

        String payUrl = moMoService.createPayment(orderCode, amount, orderInfo);

        if (payUrl != null) {
            session.setAttribute("payment_method", "momo");
            return "redirect:" + payUrl;
        }

        model.addAttribute("cart", cart);
        model.addAttribute("total", total);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("phone", phone);
        model.addAttribute("email", email);
        model.addAttribute("address", address);
        model.addAttribute("couponCode", couponCode);
        model.addAttribute("couponMessage", couponMessage);
        model.addAttribute("appliedCoupon", appliedCoupon);
        model.addAttribute("momoError", "Không thể kết nối đến MoMo. Vui lòng thử lại hoặc chọn phương thức khác.");
        return "payment/momo-checkout";
    }

    @GetMapping("/momo/callback")
    public String momoCallback(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        String resultCode = params.get("resultCode");
        String orderId = params.get("orderId");
        String message = params.get("message");

        session.setAttribute("momo_resultCode", resultCode);
        session.setAttribute("momo_orderId", orderId);
        session.setAttribute("momo_message", message);

        if ("0".equals(resultCode) && moMoService.verifySignature(params)) {
            String orderCode = (String) session.getAttribute("payment_orderCode");
            Invoice invoice = invoiceService.createInvoice(
                    (String) session.getAttribute("payment_customerName"),
                    (String) session.getAttribute("payment_phone"),
                    (String) session.getAttribute("payment_email"),
                    (String) session.getAttribute("payment_address"),
                    orderCode, PaymentMethod.MOMO, session);
            markCouponUsed(session);
            if (invoice != null) {
                txService.createTransaction(invoice, PaymentMethod.MOMO,
                        invoice.getTotalPrice(), params.get("transId"));
            }
            clearPaymentSession(session);
            return "redirect:/?success";
        }

        return "redirect:/payment/momo/result";
    }

    @GetMapping("/momo/result")
    public String momoResult(HttpSession session, Model model) {
        model.addAttribute("resultCode", session.getAttribute("momo_resultCode"));
        model.addAttribute("orderId", session.getAttribute("momo_orderId"));
        model.addAttribute("message", session.getAttribute("momo_message"));
        session.removeAttribute("momo_resultCode");
        session.removeAttribute("momo_orderId");
        session.removeAttribute("momo_message");
        return "payment/momo-result";
    }

    @PostMapping("/momo/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> momoIPN(@RequestBody Map<String, String> params) {
        boolean valid = moMoService.verifySignature(params);
        if (valid && "0".equals(params.get("resultCode"))) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        return ResponseEntity.ok(Map.of("status", "failed"));
    }

    // ========== VNPay Payment ==========

    private String processVNPayPayment(String orderCode, double finalTotal,
            HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        long amount = Math.round(finalTotal);
        String orderInfo = "BookStore - Thanh toan don hang " + orderCode;
        String ipAddress = getClientIp(request);

        String paymentUrl = vnPayService.createPaymentUrl(orderCode, amount, orderInfo, ipAddress);

        if (paymentUrl != null) {
            session.setAttribute("payment_method", "vnpay");
            return "redirect:" + paymentUrl;
        }

        log.warn("VNPay payment URL creation failed for order: {}", orderCode);
        redirectAttributes.addFlashAttribute("error", "Không thể kết nối đến VNPay. Vui lòng thử lại.");
        return "redirect:/cart";
    }

    @GetMapping("/vnpay/callback")
    public String vnpayCallback(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        boolean validSignature = vnPayService.verifySignature(params);
        boolean paymentSuccess = vnPayService.isPaymentSuccess(params);

        if (validSignature && paymentSuccess) {
            String orderCode = (String) session.getAttribute("payment_orderCode");
            Double total = (Double) session.getAttribute("payment_total");

            Invoice invoice = invoiceService.createInvoice(
                    (String) session.getAttribute("payment_customerName"),
                    (String) session.getAttribute("payment_phone"),
                    (String) session.getAttribute("payment_email"),
                    (String) session.getAttribute("payment_address"),
                    orderCode, PaymentMethod.VNPAY, session);
            markCouponUsed(session);
            if (invoice != null) {
                txService.createTransaction(invoice, PaymentMethod.VNPAY,
                        total, params.get("vnp_TransactionNo"));
            }

            session.setAttribute("vnpay_result_success", true);
            session.setAttribute("vnpay_result_orderCode", orderCode);
            session.setAttribute("vnpay_result_total", total);
            clearPaymentSession(session);
        } else {
            session.setAttribute("vnpay_result_success", false);
            session.setAttribute("vnpay_result_message",
                    "Thanh toán VNPay thất bại. Mã lỗi: " + params.get("vnp_ResponseCode"));
        }

        return "redirect:/payment/vnpay/result";
    }

    @GetMapping("/vnpay/result")
    public String vnpayResult(HttpSession session, Model model) {
        Boolean success = (Boolean) session.getAttribute("vnpay_result_success");
        model.addAttribute("success", success != null ? success : false);
        model.addAttribute("orderCode", session.getAttribute("vnpay_result_orderCode"));
        model.addAttribute("total", session.getAttribute("vnpay_result_total"));
        model.addAttribute("message", session.getAttribute("vnpay_result_message"));

        for (String key : List.of("vnpay_result_success", "vnpay_result_orderCode",
                "vnpay_result_total", "vnpay_result_message"))
            session.removeAttribute(key);

        return "payment/vnpay-result";
    }

    // ========== ZaloPay Payment ==========

    private String processZaloPayPayment(String orderCode, double finalTotal, HttpSession session,
            RedirectAttributes redirectAttributes) {
        long amount = Math.round(finalTotal);
        String description = "BookStore - Thanh toan don hang " + orderCode;

        String orderUrl = zaloPayService.createPayment(orderCode, amount, description);

        if (orderUrl != null) {
            session.setAttribute("payment_method", "zalopay");
            return "redirect:" + orderUrl;
        }

        log.warn("ZaloPay payment URL creation failed for order: {}", orderCode);
        redirectAttributes.addFlashAttribute("error", "Không thể kết nối đến ZaloPay. Vui lòng thử lại.");
        return "redirect:/cart";
    }

    @PostMapping("/zalopay/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> zalopayCallback(@RequestBody Map<String, Object> payload) {
        try {
            String dataStr = (String) payload.get("data");
            String mac = (String) payload.get("mac");

            if (zaloPayService.verifyCallback(dataStr, mac)) {
                log.info("[ZaloPay] Callback verified successfully");
                return ResponseEntity.ok(Map.of("return_code", 1, "return_message", "success"));
            }
            return ResponseEntity.ok(Map.of("return_code", -1, "return_message", "mac not equal"));
        } catch (Exception e) {
            log.error("[ZaloPay] Callback error: ", e);
            return ResponseEntity.ok(Map.of("return_code", 0, "return_message", "exception"));
        }
    }

    @GetMapping("/zalopay/result")
    public String zalopayResult(HttpSession session, Model model) {
        // ZaloPay redirects here after payment
        String orderCode = (String) session.getAttribute("payment_orderCode");
        Double total = (Double) session.getAttribute("payment_total");

        if (orderCode != null) {
            Invoice invoice = invoiceService.createInvoice(
                    (String) session.getAttribute("payment_customerName"),
                    (String) session.getAttribute("payment_phone"),
                    (String) session.getAttribute("payment_email"),
                    (String) session.getAttribute("payment_address"),
                    orderCode, PaymentMethod.ZALOPAY, session);
            markCouponUsed(session);
            if (invoice != null) {
                txService.createTransaction(invoice, PaymentMethod.ZALOPAY, total, orderCode);
            }

            model.addAttribute("success", true);
            model.addAttribute("orderCode", orderCode);
            model.addAttribute("total", total);
            clearPaymentSession(session);
        } else {
            model.addAttribute("success", false);
            model.addAttribute("message", "Không tìm thấy thông tin đơn hàng.");
        }

        return "payment/zalopay-result";
    }

    // ========== COD (Cash on Delivery) ==========

    private String processCODPayment(String orderCode, double finalTotal,
            HttpSession session, Model model) {
        Invoice invoice = invoiceService.createInvoice(
                (String) session.getAttribute("payment_customerName"),
                (String) session.getAttribute("payment_phone"),
                (String) session.getAttribute("payment_email"),
                (String) session.getAttribute("payment_address"),
                orderCode, PaymentMethod.COD, session);
        markCouponUsed(session);
        if (invoice != null) {
            txService.createTransaction(invoice, PaymentMethod.COD, finalTotal, orderCode);
        }

        session.setAttribute("cod_result_orderCode", orderCode);
        session.setAttribute("cod_result_total", finalTotal);
        session.setAttribute("cod_result_discount", session.getAttribute("payment_discount"));
        session.setAttribute("cod_result_customerName", session.getAttribute("payment_customerName"));
        session.setAttribute("cod_result_phone", session.getAttribute("payment_phone"));
        session.setAttribute("cod_result_address", session.getAttribute("payment_address"));

        clearPaymentSession(session);
        return "redirect:/payment/cod/result";
    }

    @GetMapping("/cod/result")
    public String codResult(HttpSession session, Model model) {
        model.addAttribute("orderCode", session.getAttribute("cod_result_orderCode"));
        model.addAttribute("total", session.getAttribute("cod_result_total"));
        model.addAttribute("discount", session.getAttribute("cod_result_discount"));
        model.addAttribute("customerName", session.getAttribute("cod_result_customerName"));
        model.addAttribute("phone", session.getAttribute("cod_result_phone"));
        model.addAttribute("address", session.getAttribute("cod_result_address"));

        for (String key : List.of("cod_result_orderCode", "cod_result_total", "cod_result_discount",
                "cod_result_customerName", "cod_result_phone", "cod_result_address"))
            session.removeAttribute(key);

        return "payment/cod-result";
    }

    // ========== Helpers ==========

    private void markCouponUsed(HttpSession session) {
        Long couponId = (Long) session.getAttribute("payment_couponId");
        if (couponId != null)
            couponService.getCouponById(couponId).ifPresent(couponService::markCouponUsed);
    }

    private void clearPaymentSession(HttpSession session) {
        for (String key : List.of("payment_customerName", "payment_phone", "payment_email", "payment_address",
                "payment_orderCode", "payment_total", "payment_originalTotal", "payment_discount",
                "payment_couponId", "payment_couponCode", "payment_method"))
            session.removeAttribute(key);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
