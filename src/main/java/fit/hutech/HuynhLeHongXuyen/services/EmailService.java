package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@bookstore.vn}")
    private String fromEmail;

    @Async
    public void sendOrderConfirmation(Invoice invoice) {
        if (invoice.getEmail() == null || invoice.getEmail().isBlank()) {
            log.warn("Cannot send order confirmation - no email for order: {}", invoice.getOrderCode());
            return;
        }

        Context context = new Context();
        context.setVariable("invoice", invoice);
        String html = templateEngine.process("email/order-confirmation", context);

        sendHtmlEmail(invoice.getEmail(),
                "BookStore - Xác nhận đơn hàng #" + invoice.getOrderCode(),
                html);
    }

    @Async
    public void sendOrderStatusUpdate(Invoice invoice) {
        if (invoice.getEmail() == null || invoice.getEmail().isBlank()) return;

        Context context = new Context();
        context.setVariable("invoice", invoice);
        String html = templateEngine.process("email/order-status-update", context);

        sendHtmlEmail(invoice.getEmail(),
                "BookStore - Cập nhật đơn hàng #" + invoice.getOrderCode(),
                html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
