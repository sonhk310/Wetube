package com.wetube.wetube_service.service.live;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Ghi lại transaction nào đang mở (và có phải REQUIRES_NEW không) để test biết mã chạy trong transaction nào. */
class TrackingTransactionManager implements PlatformTransactionManager {

    int started;
    int committed;
    int rolledBack;
    private final Deque<Boolean> open = new ArrayDeque<>();

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        started++;
        open.push(definition != null
                && definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new SimpleTransactionStatus(true);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        committed++;
        open.pop();
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        rolledBack++;
        open.pop();
    }

    int active() {
        return open.size();
    }

    /** Đang ở trong một transaction REQUIRES_NEW (tự commit khi lambda của TransactionTemplate trả về). */
    boolean insideRequiresNew() {
        return !open.isEmpty() && open.peek();
    }
}
