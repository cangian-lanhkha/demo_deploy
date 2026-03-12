package fit.hutech.HuynhLeHongXuyen.entities.enums;

public enum PaymentMethod {
    QR_BANK("QR Chuyển khoản"),
    MOMO("Ví MoMo"),
    VNPAY("VNPay"),
    ZALOPAY("ZaloPay"),
    COD("Thanh toán khi nhận hàng");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
