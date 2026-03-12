package fit.hutech.HuynhLeHongXuyen.entities.enums;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPING("Đang giao hàng"),
    DELIVERED("Đã giao hàng"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
