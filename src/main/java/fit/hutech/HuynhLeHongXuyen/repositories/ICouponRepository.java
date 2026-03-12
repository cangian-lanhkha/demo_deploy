package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ICouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    List<Coupon> findByActiveTrueAndStartDateBeforeAndEndDateAfter(
            LocalDateTime now1, LocalDateTime now2);

    List<Coupon> findByActiveTrue();

    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id AND c.usedCount < :usageLimit")
    int atomicIncrementUsedCount(@Param("id") Long id, @Param("usageLimit") int usageLimit);

    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.id = :id")
    int atomicIncrementUsedCountNoLimit(@Param("id") Long id);
}
