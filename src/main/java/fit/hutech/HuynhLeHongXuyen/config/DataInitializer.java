package fit.hutech.HuynhLeHongXuyen.config;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Category;
import fit.hutech.HuynhLeHongXuyen.entities.Coupon;
import fit.hutech.HuynhLeHongXuyen.entities.Role;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.ICategoryRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.ICouponRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IRoleRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("dev")
public class DataInitializer {
        private final IRoleRepository roleRepository;
        private final IUserRepository userRepository;
        private final ICategoryRepository categoryRepository;
        private final IBookRepository bookRepository;
        private final ICouponRepository couponRepository;
        private final PasswordEncoder passwordEncoder;

        @Bean
        public CommandLineRunner initData() {
                return args -> {
                        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                                Role role = new Role();
                                role.setName("USER");
                                return roleRepository.save(role);
                        });

                        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                                Role role = new Role();
                                role.setName("ADMIN");
                                return roleRepository.save(role);
                        });

                        if (userRepository.findByUsername("admin").isEmpty()) {
                                User admin = new User();
                                admin.setUsername("admin");
                                admin.setPassword(passwordEncoder.encode("admin123"));
                                admin.setEmail("admin@bookstore.com");
                                admin.setPhone("0901234567");
                                Set<Role> adminRoles = new HashSet<>();
                                adminRoles.add(adminRole);
                                adminRoles.add(userRole);
                                admin.setRoles(adminRoles);
                                userRepository.save(admin);
                        }

                        if (userRepository.findByUsername("user1").isEmpty()) {
                                User user1 = new User();
                                user1.setUsername("user1");
                                user1.setPassword(passwordEncoder.encode("user123"));
                                user1.setEmail("user1@bookstore.com");
                                user1.setPhone("0912345678");
                                Set<Role> user1Roles = new HashSet<>();
                                user1Roles.add(userRole);
                                user1.setRoles(user1Roles);
                                userRepository.save(user1);
                        }

                        if (userRepository.findByUsername("user2").isEmpty()) {
                                User user2 = new User();
                                user2.setUsername("user2");
                                user2.setPassword(passwordEncoder.encode("user123"));
                                user2.setEmail("user2@bookstore.com");
                                user2.setPhone("0923456789");
                                Set<Role> user2Roles = new HashSet<>();
                                user2Roles.add(userRole);
                                user2.setRoles(user2Roles);
                                userRepository.save(user2);
                        }

                        // ===== DANH MỤC MỚI 100% =====
                        Category catTrinhTham = findOrCreateCategory("Trinh thám");
                        Category catTamLy = findOrCreateCategory("Tâm lý học");
                        Category catThienVan = findOrCreateCategory("Thiên văn");
                        Category catAmThuc = findOrCreateCategory("Ẩm thực");
                        Category catDuLich = findOrCreateCategory("Du lịch");
                        Category catYHoc = findOrCreateCategory("Y học");
                        Category catAmNhac = findOrCreateCategory("Âm nhạc");
                        Category catTrietHoc = findOrCreateCategory("Triết học");

                        // Patch: fix existing books with null quantity
                        for (Book b : bookRepository.findAll()) {
                                if (b.getQuantity() == null || b.getQuantity() == 0) {
                                        b.setQuantity(100);
                                        b.setSoldCount(0);
                                        bookRepository.save(b);
                                }
                        }

                        if (bookRepository.count() == 0) {
                                // Trinh thám
                                bookRepository.save(Book.builder().title("Mật mã bóng đêm")
                                                .author("Huỳnh Khánh Duy")
                                                .price(142000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrinhTham).build());
                                bookRepository.save(Book.builder().title("Vụ án trên sông Hương")
                                                .author("Đặng Quốc Hưng")
                                                .price(178000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrinhTham).build());
                                bookRepository.save(Book.builder().title("Kẻ giấu mặt giữa Sài Gòn")
                                                .author("Lý Minh Châu")
                                                .price(199000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrinhTham).build());

                                // Tâm lý học
                                bookRepository.save(Book.builder().title("Bản đồ tâm trí con người")
                                                .author("Trương Hải Đăng")
                                                .price(215000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTamLy).build());
                                bookRepository.save(Book.builder().title("Hiểu mình để sống tốt hơn")
                                                .author("Bùi Ngọc Trâm")
                                                .price(168000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTamLy).build());
                                bookRepository.save(Book.builder().title("Nghệ thuật đọc vị cảm xúc")
                                                .author("Hoàng Gia Bảo")
                                                .price(189000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTamLy).build());

                                // Thiên văn
                                bookRepository.save(Book.builder().title("Hành trình xuyên vũ trụ")
                                                .author("Phan Thiên Long")
                                                .price(298000.0).image("picture.jpg").quantity(100).soldCount(0).category(catThienVan).build());
                                bookRepository.save(Book.builder().title("Bí ẩn hố đen và sao neutron")
                                                .author("Vương Quốc Thắng")
                                                .price(265000.0).image("picture.jpg").quantity(100).soldCount(0).category(catThienVan).build());
                                bookRepository.save(Book.builder().title("Sao Hỏa - Giấc mơ di cư")
                                                .author("Đinh Nhật Minh")
                                                .price(232000.0).image("picture.jpg").quantity(100).soldCount(0).category(catThienVan).build());

                                // Ẩm thực
                                bookRepository.save(Book.builder().title("100 món ngon đường phố Việt Nam")
                                                .author("Mai Thùy Linh")
                                                .price(155000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmThuc).build());
                                bookRepository.save(Book.builder().title("Nghệ thuật nấu phở truyền thống")
                                                .author("Đỗ Quang Vinh")
                                                .price(188000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmThuc).build());
                                bookRepository.save(Book.builder().title("Bánh mì Sài Gòn - Hương vị thời gian")
                                                .author("Tôn Nữ Hạnh Phúc")
                                                .price(127000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmThuc).build());

                                // Du lịch
                                bookRepository.save(Book.builder().title("Khám phá Tây Bắc bằng xe máy")
                                                .author("Cao Sơn Tùng")
                                                .price(175000.0).image("picture.jpg").quantity(100).soldCount(0).category(catDuLich).build());
                                bookRepository.save(Book.builder().title("Phú Quốc - Đảo ngọc phương Nam")
                                                .author("Ngô Bích Ngọc")
                                                .price(148000.0).image("picture.jpg").quantity(100).soldCount(0).category(catDuLich).build());
                                bookRepository.save(Book.builder().title("Đà Lạt mộng mơ qua ống kính")
                                                .author("Trịnh Hoàng Nam")
                                                .price(205000.0).image("picture.jpg").quantity(100).soldCount(0).category(catDuLich).build());

                                // Y học
                                bookRepository.save(Book.builder().title("Sống khỏe mỗi ngày với y học cổ truyền")
                                                .author("Lương Đức Thiện")
                                                .price(245000.0).image("picture.jpg").quantity(100).soldCount(0).category(catYHoc).build());
                                bookRepository.save(Book.builder().title("Dinh dưỡng thông minh cho gia đình")
                                                .author("Châu Mỹ Hạnh")
                                                .price(198000.0).image("picture.jpg").quantity(100).soldCount(0).category(catYHoc).build());
                                bookRepository.save(Book.builder().title("Phòng bệnh hơn chữa bệnh")
                                                .author("Tạ Quốc Bảng")
                                                .price(162000.0).image("picture.jpg").quantity(100).soldCount(0).category(catYHoc).build());

                                // Âm nhạc
                                bookRepository.save(Book.builder().title("Guitar từ zero đến hero")
                                                .author("Hồ Nhật Quang")
                                                .price(139000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmNhac).build());
                                bookRepository.save(Book.builder().title("Lịch sử nhạc Trịnh qua từng giai điệu")
                                                .author("Dương Khánh An")
                                                .price(225000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmNhac).build());
                                bookRepository.save(Book.builder().title("Piano cổ điển cho người mới")
                                                .author("Vũ Lan Phương")
                                                .price(185000.0).image("picture.jpg").quantity(100).soldCount(0).category(catAmNhac).build());

                                // Triết học
                                bookRepository.save(Book.builder().title("Socrates và nghệ thuật đặt câu hỏi")
                                                .author("Kiều Trung Hiếu")
                                                .price(172000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrietHoc).build());
                                bookRepository.save(Book.builder().title("Thiền và triết lý phương Đông")
                                                .author("Thích Minh Quang")
                                                .price(195000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrietHoc).build());
                                bookRepository.save(Book.builder().title("Chủ nghĩa hiện sinh là gì?")
                                                .author("Nguyễn Phúc Anh")
                                                .price(158000.0).image("picture.jpg").quantity(100).soldCount(0).category(catTrietHoc).build());
                        }

                        // ===== MÃ GIẢM GIÁ MỚI 100% =====
                        if (couponRepository.count() == 0) {
                                Coupon c1 = new Coupon();
                                c1.setCode("CHAOHE");
                                c1.setDescription("Chào hè rực rỡ - Giảm 15%");
                                c1.setDiscountPercent(15.0);
                                c1.setMaxDiscount(40000.0);
                                c1.setMinOrderAmount(null);
                                c1.setStartDate(java.time.LocalDateTime.now());
                                c1.setEndDate(java.time.LocalDateTime.now().plusMonths(2));
                                c1.setUsageLimit(80);
                                c1.setUsedCount(0);
                                c1.setActive(true);
                                couponRepository.save(c1);

                                Coupon c2 = new Coupon();
                                c2.setCode("MUASAM35");
                                c2.setDescription("Mua sắm thả ga - Giảm 35% đơn từ 300k");
                                c2.setDiscountPercent(35.0);
                                c2.setMaxDiscount(70000.0);
                                c2.setMinOrderAmount(300000.0);
                                c2.setStartDate(java.time.LocalDateTime.now());
                                c2.setEndDate(java.time.LocalDateTime.now().plusMonths(1));
                                c2.setUsageLimit(40);
                                c2.setUsedCount(0);
                                c2.setActive(true);
                                couponRepository.save(c2);

                                Coupon c3 = new Coupon();
                                c3.setCode("DOCGIA60");
                                c3.setDescription("Tri ân độc giả - Giảm 60% đơn từ 600k");
                                c3.setDiscountPercent(60.0);
                                c3.setMaxDiscount(150000.0);
                                c3.setMinOrderAmount(600000.0);
                                c3.setStartDate(java.time.LocalDateTime.now());
                                c3.setEndDate(java.time.LocalDateTime.now().plusMonths(4));
                                c3.setUsageLimit(15);
                                c3.setUsedCount(0);
                                c3.setActive(true);
                                couponRepository.save(c3);
                        }
                };
        }

        private Category findOrCreateCategory(String name) {
                List<Category> allCategories = categoryRepository.findAll();
                Optional<Category> existing = allCategories.stream()
                                .filter(c -> c.getName().equals(name))
                                .findFirst();
                return existing.orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
        }
}
