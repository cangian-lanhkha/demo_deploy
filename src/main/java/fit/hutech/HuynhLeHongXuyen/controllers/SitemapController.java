package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Category;
import fit.hutech.HuynhLeHongXuyen.services.BookService;
import fit.hutech.HuynhLeHongXuyen.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SitemapController {
    private final BookService bookService;
    private final CategoryService categoryService;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // Static pages
        addUrl(sb, "/", today, "daily", "1.0");
        addUrl(sb, "/books", today, "daily", "0.9");

        // Category pages
        List<Category> categories = categoryService.getAllCategories();
        for (Category cat : categories) {
            addUrl(sb, "/books?categoryId=" + cat.getId(), today, "weekly", "0.7");
        }

        // Book detail pages
        List<Book> books = bookService.getAllBooks();
        for (Book book : books) {
            addUrl(sb, "/books/" + book.getId(), today, "weekly", "0.8");
        }

        sb.append("</urlset>");
        return sb.toString();
    }

    private void addUrl(StringBuilder sb, String path, String lastmod, String changefreq, String priority) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escapeXml(baseUrl + path)).append("</loc>\n");
        sb.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
