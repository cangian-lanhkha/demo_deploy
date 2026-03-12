package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Notification;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface INotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrUserIsNullOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
}
