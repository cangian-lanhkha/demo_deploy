package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final IBookRepository bookRepository;

    @GetMapping
    public String listCategories(Model model) {
        var categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        Map<Long, Long> bookCountMap = new HashMap<>();
        for (var cat : categories) {
            bookCountMap.put(cat.getId(), bookRepository.countByCategoryId(cat.getId()));
        }
        model.addAttribute("bookCountMap", bookCountMap);
        return "category/list";
    }
}
