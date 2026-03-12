package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.*;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import fit.hutech.HuynhLeHongXuyen.repositories.*;
import fit.hutech.HuynhLeHongXuyen.services.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final IBookRepository bookRepository;
    private final ICategoryRepository categoryRepository;
    private final IInvoiceRepository invoiceRepository;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final BookService bookService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final PaymentTransactionService paymentTransactionService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        // Default: last 30 days
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        // Summary stats
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("totalCategories", categoryRepository.count());
        model.addAttribute("totalOrders", invoiceRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("summary", reportService.getSummary(from, to));

        // Chart data
        model.addAttribute("revenueByDay", reportService.getRevenueByDay(from, to));
        model.addAttribute("orderByStatus", reportService.getOrderCountByStatus(from, to));
        model.addAttribute("revenueByPayment", reportService.getRevenueByPaymentMethod(from, to));
        model.addAttribute("topBooks", reportService.getTopSellingBooks(from, to));
        model.addAttribute("topCustomers", reportService.getTopCustomers(from, to));

        // Recent orders
        model.addAttribute("recentOrders", invoiceRepository.findAll(PageRequest.of(0, 10)).getContent());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/dashboard";
    }

    @GetMapping("/books")
    public String manageBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/books";
    }

    @GetMapping("/categories")
    public String manageCategories(Model model) {
        var categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        java.util.Map<Long, Long> bookCountMap = new java.util.HashMap<>();
        for (var cat : categories) {
            bookCountMap.put(cat.getId(), bookRepository.countByCategoryId(cat.getId()));
        }
        model.addAttribute("bookCountMap", bookCountMap);
        return "admin/categories";
    }

    @GetMapping("/books/add")
    public String adminAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book-add";
    }

    @PostMapping("/books/add")
    public String adminAddBook(@Valid @ModelAttribute("book") Book book, BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/book-add";
        }
        if (imageFile != null && !imageFile.isEmpty())
            book.setImage(saveImage(imageFile));
        bookService.addBook(book);
        redirectAttributes.addFlashAttribute("success", "Thêm sách '" + book.getTitle() + "' thành công!");
        return "redirect:/admin/books";
    }

    @GetMapping("/books/edit/{id}")
    public String adminEditBookForm(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại: " + id)));
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/book-edit";
    }

    @PostMapping("/books/edit")
    public String adminEditBook(@Valid @ModelAttribute("book") Book book, BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin/book-edit";
        }
        if (imageFile != null && !imageFile.isEmpty())
            book.setImage(saveImage(imageFile));
        bookService.updateBook(book);
        redirectAttributes.addFlashAttribute("success", "Cập nhật sách '" + book.getTitle() + "' thành công!");
        return "redirect:/admin/books";
    }

    @GetMapping("/books/delete/{id}")
    public String adminDeleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBookById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa sách: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/categories/add")
    public String adminAddCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-add";
    }

    @PostMapping("/categories/add")
    public String adminAddCategory(@Valid @ModelAttribute("category") Category category, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "admin/category-add";
        categoryService.addCategory(category);
        redirectAttributes.addFlashAttribute("success", "Thêm danh mục '" + category.getName() + "' thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String adminEditCategoryForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại: " + id)));
        return "admin/category-edit";
    }

    @PostMapping("/categories/edit")
    public String adminEditCategory(@Valid @ModelAttribute("category") Category category, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "admin/category-edit";
        categoryService.updateCategory(category);
        redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục '" + category.getName() + "' thành công!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String adminDeleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategoryById(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa danh mục thành công! Sách liên quan giữ nguyên.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads/images}")
    private String uploadDir;

    private String saveImage(MultipartFile file) {
        try {
            if (file.getSize() > MAX_FILE_SIZE)
                throw new IllegalArgumentException("File quá lớn. Tối đa 5MB.");

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType))
                throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF).");

            // Use UUID only for filename to prevent path traversal
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                default -> ".jpg";
            };
            String fileName = java.util.UUID.randomUUID() + extension;

            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(uploadPath))
                java.nio.file.Files.createDirectories(uploadPath);
            java.nio.file.Path filePath = uploadPath.resolve(fileName).normalize();

            // Ensure file stays within upload directory
            if (!filePath.startsWith(uploadPath))
                throw new SecurityException("Path traversal detected.");

            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (java.io.IOException e) {
            throw new RuntimeException("Không thể lưu ảnh. Vui lòng thử lại.");
        }
    }

    @GetMapping("/orders")
    public String manageOrders(@RequestParam(required = false) String status, Model model) {
        List<Invoice> orders;
        if (status != null && !status.isBlank()) {
            try {
                orders = orderService.getOrdersByStatus(OrderStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                orders = orderService.getAllOrders();
            }
        } else {
            orders = orderService.getAllOrders();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("currentStatus", status);
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Invoice order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("history", orderService.getOrderHistory(id));
        model.addAttribute("transactions", paymentTransactionService.getTransactionsByOrder(id));
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
            @RequestParam OrderStatus newStatus,
            @RequestParam(required = false) String note,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User admin = userRepository.findByUsername(authentication.getName()).orElse(null);
            orderService.updateOrderStatus(id, newStatus, note, admin);
            redirectAttributes.addFlashAttribute("success",
                    "Đã cập nhật trạng thái đơn #" + id + " thành '" + newStatus.getDisplayName() + "'");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/tracking")
    public String updateTracking(@PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam(required = false) String shippingProvider,
            RedirectAttributes redirectAttributes) {
        orderService.updateTracking(id, trackingNumber, shippingProvider);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin vận chuyển đơn #" + id);
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/users")
    public String manageUsers(Model model, Authentication authentication) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("allRoles", roleRepository.findAll());
        model.addAttribute("currentAdmin", authentication.getName());
        return "admin/users";
    }

    @PostMapping("/users/{userId}/roles")
    public String updateUserRoles(@PathVariable Long userId,
            @RequestParam(value = "roles", required = false) List<String> roleNames,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        if (targetUser.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"))) {
            redirectAttributes.addFlashAttribute("error",
                    "Không thể chỉnh sửa vai trò của Admin cùng cấp: " + targetUser.getUsername());
            return "redirect:/admin/users";
        }
        if (targetUser.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Không thể chỉnh sửa vai trò của chính bạn!");
            return "redirect:/admin/users";
        }
        Set<Role> newRoles = new HashSet<>();
        if (roleNames != null) {
            for (String roleName : roleNames) {
                // Prevent assigning ADMIN role through UI
                if ("ADMIN".equals(roleName)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Không thể gán vai trò ADMIN qua giao diện quản trị!");
                    return "redirect:/admin/users";
                }
                roleRepository.findByName(roleName).ifPresent(newRoles::add);
            }
        }
        if (newRoles.isEmpty())
            roleRepository.findByName("USER").ifPresent(newRoles::add);
        targetUser.setRoles(newRoles);
        userRepository.save(targetUser);
        redirectAttributes.addFlashAttribute("success",
                "Đã cập nhật vai trò cho " + targetUser.getUsername() + " thành công!");
        return "redirect:/admin/users";
    }

    @GetMapping("/coupons")
    public String manageCoupons(Model model) {
        model.addAttribute("coupons", couponService.getAllCoupons());
        model.addAttribute("newCoupon", new Coupon());
        model.addAttribute("users", userRepository.findAll());
        return "admin/coupons";
    }

    @PostMapping("/coupons/add")
    public String addCoupon(@RequestParam String code, @RequestParam String description,
            @RequestParam Double discountPercent,
            @RequestParam(required = false) Double maxDiscount, @RequestParam(required = false) Double minOrderAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer usageLimit, RedirectAttributes redirectAttributes) {
        if (couponService.getCouponByCode(code).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Mã giảm giá '" + code + "' đã tồn tại!");
            return "redirect:/admin/coupons";
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code.toUpperCase().trim());
        coupon.setDescription(description);
        coupon.setDiscountPercent(discountPercent);
        coupon.setMaxDiscount(maxDiscount);
        coupon.setMinOrderAmount(minOrderAmount);
        coupon.setStartDate(startDate != null ? startDate : LocalDateTime.now());
        coupon.setEndDate(endDate);
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        couponService.saveCoupon(coupon);
        redirectAttributes.addFlashAttribute("success", "Thêm mã giảm giá '" + coupon.getCode() + "' thành công!");
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/toggle")
    public String toggleCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        couponService.getCouponById(id).ifPresent(coupon -> {
            coupon.setActive(!coupon.getActive());
            couponService.saveCoupon(coupon);
            redirectAttributes.addFlashAttribute("success",
                    (coupon.getActive() ? "Kích hoạt" : "Tắt") + " mã '" + coupon.getCode() + "' thành công!");
        });
        return "redirect:/admin/coupons";
    }

    @GetMapping("/coupons/delete/{id}")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        couponService.getCouponById(id).ifPresent(coupon -> {
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã '" + coupon.getCode() + "'");
        });
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/send")
    public String sendCouponToUser(@PathVariable Long id, @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean sendToAll, RedirectAttributes redirectAttributes) {
        var couponOpt = couponService.getCouponById(id);
        if (couponOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Mã giảm giá không tồn tại");
            return "redirect:/admin/coupons";
        }
        Coupon coupon = couponOpt.get();
        if (sendToAll) {
            notificationService.sendCouponToAll(coupon.getCode(), coupon.getDescription());
            redirectAttributes.addFlashAttribute("success",
                    "Đã gửi mã '" + coupon.getCode() + "' cho tất cả người dùng!");
        } else if (userId != null) {
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                notificationService.sendCouponToUser(userOpt.get(), coupon.getCode(), coupon.getDescription());
                redirectAttributes.addFlashAttribute("success",
                        "Đã gửi mã '" + coupon.getCode() + "' cho " + userOpt.get().getUsername() + "!");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn người nhận!");
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title, @RequestParam String message,
            @RequestParam(required = false) Long userId, @RequestParam(defaultValue = "false") boolean sendToAll,
            RedirectAttributes redirectAttributes) {
        if (sendToAll) {
            notificationService.sendToAll(title, message, "INFO", null);
            redirectAttributes.addFlashAttribute("success", "Đã gửi thông báo cho tất cả người dùng!");
        } else if (userId != null) {
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                notificationService.sendToUser(userOpt.get(), title, message, "INFO", null);
                redirectAttributes.addFlashAttribute("success",
                        "Đã gửi thông báo cho " + userOpt.get().getUsername() + "!");
            }
        }
        return "redirect:/admin/coupons";
    }

    @GetMapping("/reports/export")
    public void exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws Exception {
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=orders_" + from + "_" + to + ".csv");
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // BOM for Excel

        PrintWriter writer = response.getWriter();
        writer.println("Mã đơn,Khách hàng,Email,SĐT,Ngày đặt,Tổng tiền,Phí ship,Giảm giá,Trạng thái,Thanh toán,Phương thức TT");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Invoice> orders = reportService.getOrdersForExport(from, to);
        for (Invoice o : orders) {
            writer.printf("%s,%s,%s,%s,%s,%.0f,%.0f,%.0f,%s,%s,%s%n",
                    csvSafe(o.getOrderCode()),
                    csvSafe(o.getCustomerName()),
                    csvSafe(o.getEmail()),
                    csvSafe(o.getPhone()),
                    o.getOrderDate() != null ? o.getOrderDate().format(fmt) : "",
                    o.getTotalPrice() != null ? o.getTotalPrice() : 0,
                    o.getShippingFee() != null ? o.getShippingFee() : 0,
                    o.getDiscount() != null ? o.getDiscount() : 0,
                    o.getStatus() != null ? o.getStatus().getDisplayName() : "",
                    o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "",
                    o.getPaymentMethod() != null ? o.getPaymentMethod().getDisplayName() : "");
        }
        writer.flush();
    }

    private String csvSafe(String value) {
        if (value == null) return "";
        // Prevent CSV injection
        if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@"))
            value = "'" + value;
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
}
