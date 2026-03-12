package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICategoryRepository extends JpaRepository<Category, Long> {
}
