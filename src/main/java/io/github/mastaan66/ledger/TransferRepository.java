package io.github.mastaan66.ledger;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<TransferRecord, String> {

    Page<TransferRecord> findByFromAccountOrToAccount(
            String fromAccount,
            String toAccount,
            Pageable pageable);

    Page<TransferRecord> findByFromAccount(String fromAccount, Pageable pageable);

    Page<TransferRecord> findByToAccount(String toAccount, Pageable pageable);

    @Query("""
            select count(transfer) as transferCount, sum(transfer.amount) as totalAmount
            from TransferRecord transfer
            where transfer.fromAccount = :accountNumber
            """)
    TransferTotals summarizeOutgoing(@Param("accountNumber") String accountNumber);

    @Query("""
            select count(transfer) as transferCount, sum(transfer.amount) as totalAmount
            from TransferRecord transfer
            where transfer.toAccount = :accountNumber
            """)
    TransferTotals summarizeIncoming(@Param("accountNumber") String accountNumber);

    interface TransferTotals {
        long getTransferCount();

        BigDecimal getTotalAmount();
    }
}
