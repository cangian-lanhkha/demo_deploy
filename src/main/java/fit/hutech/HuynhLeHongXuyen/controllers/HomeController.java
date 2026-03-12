package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.services.BookService;
import fit.hutech.HuynhLeHongXuyen.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String index(Model model) {
        List<Book> allBooks = bookService.getAllBooks();
        model.addAttribute("featuredBooks", allBooks.stream().limit(8).collect(Collectors.toList()));
        model.addAttribute("bestSellers", allBooks.stream()
                .sorted(Comparator.comparingDouble(Book::getPrice).reversed()).limit(4).collect(Collectors.toList()));
        model.addAttribute("newestBooks", allBooks.stream().sorted(Comparator.comparingLong(Book::getId).reversed())
                .limit(4).collect(Collectors.toList()));
        model.addAttribute("cheapBooks", allBooks.stream().sorted(Comparator.comparingDouble(Book::getPrice)).limit(4)
                .collect(Collectors.toList()));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("totalBooks", allBooks.size());
        return "home/index";
    }
}
