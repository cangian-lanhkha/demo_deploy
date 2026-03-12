package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.PaymentTransaction;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentStatus;
import fit.hutech.HuynhLeHongXuyen.repositories.IPaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTransactionService {
    private final IPaymentTransactionRepository transactionRepository;

    @Transactional
    public PaymentTransaction createTransaction(Invoice order, PaymentMethod method,
                                                 Double amount, String transactionCode) {
        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(method)
                .amount(amount)
                .transactionCode(transactionCode)
                .status(PaymentStatus.PENDING)
                .build();
        return transactionRepository.save(tx);
    }

    @Transactional
    public void updateTransactionStatus(String transactionCode, PaymentStatus status, String gatewayResponse) {
        transactionRepository.findByTransactionCode(transactionCode).ifPresent(tx -> {
            tx.setStatus(status);
            tx.setGatewayResponse(gatewayResponse);
            transactionRepository.save(tx);
            log.info("Transaction {} updated to {}", transactionCode, status);
        });
    }

    public List<PaymentTransaction> getTransactionsByOrder(Long orderId) {
        return transactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }
}
