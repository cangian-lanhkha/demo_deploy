package fit.hutech.HuynhLeHongXuyen.api;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Category;
import fit.hutech.HuynhLeHongXuyen.services.BookService;
import fit.hutech.HuynhLeHongXuyen.services.CategoryService;
import fit.hutech.HuynhLeHongXuyen.viewmodels.BookGetVm;
import fit.hutech.HuynhLeHongXuyen.viewmodels.BookPostVm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookApiController {
    private final BookService bookService;
    private final CategoryService categoryService;

    private BookGetVm toGetVm(Book book) {
        return BookGetVm.builder().id(book.getId()).title(book.getTitle()).author(book.getAuthor())
                .price(book.getPrice()).image(book.getImage())
                .categoryName(book.getCategory() != null ? book.getCategory().getName() : null).build();
    }

    private Book toEntity(BookPostVm vm) {
        Book book = new Book();
        book.setTitle(vm.getTitle());
        book.setAuthor(vm.getAuthor());
        book.setPrice(vm.getPrice());
        if (vm.getCategoryId() != null)
            book.setCategory(categoryService.getCategoryById(vm.getCategoryId()).orElse(null));
        return book;
    }

    @GetMapping
    public ResponseEntity<List<BookGetVm>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks().stream().map(this::toGetVm).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookGetVm> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id).map(book -> ResponseEntity.ok(toGetVm(book)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BookGetVm> createBook(@Valid @RequestBody BookPostVm bookVm) {
        Book book = toEntity(bookVm);
        bookService.addBook(book);
        return ResponseEntity.ok(toGetVm(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookGetVm> updateBook(@PathVariable Long id, @Valid @RequestBody BookPostVm bookVm) {
        Book book = toEntity(bookVm);
        book.setId(id);
        bookService.updateBook(book);
        return ResponseEntity.ok(toGetVm(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBookById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookGetVm>> searchBooks(@RequestParam String keyword) {
        return ResponseEntity
                .ok(bookService.searchBooks(keyword).stream().map(this::toGetVm).collect(Collectors.toList()));
    }
}
