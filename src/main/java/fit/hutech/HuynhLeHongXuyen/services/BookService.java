package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.ItemInvoice;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IItemInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {
    private final IBookRepository bookRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;

    @Cacheable(value = "books", key = "'all'")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Cacheable(value = "bookDetail", key = "#id")
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Caching(evict = {
        @CacheEvict(value = "books", allEntries = true),
        @CacheEvict(value = "bookDetail", allEntries = true)
    })
    public void addBook(Book book) {
        bookRepository.save(book);
    }

    @Caching(evict = {
        @CacheEvict(value = "books", allEntries = true),
        @CacheEvict(value = "bookDetail", key = "#book.id")
    })
    public void updateBook(Book book) {
        Book existingBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại"));
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setPrice(book.getPrice());
        existingBook.setCategory(book.getCategory());
        existingBook.setImage(book.getImage());
        bookRepository.save(existingBook);
    }

    /**
     * Xóa sách: lịch sử đơn hàng chứa sách này sẽ KHÔNG bị xóa.
     * Set book = null trong item_invoice, giữ lại snapshot tên/giá.
     */
    @Caching(evict = {
        @CacheEvict(value = "books", allEntries = true),
        @CacheEvict(value = "bookDetail", key = "#id")
    })
    public void deleteBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại: " + id));
        List<ItemInvoice> relatedItems = itemInvoiceRepository.findByBook(book);
        for (ItemInvoice item : relatedItems) {
            if (item.getBookTitle() == null)
                item.setBookTitle(book.getTitle());
            if (item.getBookAuthor() == null)
                item.setBookAuthor(book.getAuthor());
            if (item.getPrice() == null)
                item.setPrice(book.getPrice());
            item.setBook(null);
        }

        // QUAN TRỌNG: Flush tất cả thay đổi TRƯỚC khi xoá sách
        if (!relatedItems.isEmpty()) {
            itemInvoiceRepository.saveAllAndFlush(relatedItems);
        }

        bookRepository.deleteById(id);
    }

    public List<Book> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    public Page<Book> advancedSearch(String keyword, Long categoryId, Double minPrice,
                                      Double maxPrice, Double minRating, Boolean inStock,
                                      Pageable pageable) {
        return bookRepository.advancedSearch(keyword, categoryId, minPrice, maxPrice, minRating, inStock, pageable);
    }

    public List<Book> getRelatedBooks(Long categoryId, Long excludeBookId) {
        return bookRepository.findTop8ByCategoryIdAndIdNot(categoryId, excludeBookId);
    }

    /**
     * Check if book has enough stock.
     */
    public boolean hasStock(Long bookId, int quantity) {
        return bookRepository.findById(bookId)
                .map(book -> book.getQuantity() != null && book.getQuantity() >= quantity)
                .orElse(false);
    }

    /**
     * Increase stock (for admin restocking or order cancellation).
     */
    public void increaseStock(Long bookId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        int updated = bookRepository.increaseStock(bookId, quantity);
        if (updated == 0) throw new IllegalArgumentException("Sách không tồn tại: " + bookId);
    }
}
