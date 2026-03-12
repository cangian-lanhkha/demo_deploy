package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Category;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.ICategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final ICategoryRepository categoryRepository;
    private final IBookRepository bookRepository;

    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void addCategory(Category category) {
        categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void updateCategory(Category category) {
        categoryRepository.save(category);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại: " + id));
        List<Book> books = bookRepository.findByCategoryId(id);
        for (Book book : books) {
            book.setCategory(null);
            bookRepository.save(book);
        }
        categoryRepository.deleteById(id);
    }
}
