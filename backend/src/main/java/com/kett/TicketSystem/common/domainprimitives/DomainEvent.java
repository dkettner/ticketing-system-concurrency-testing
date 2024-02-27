package com.kett.TicketSystem.common.domainprimitives;

import lombok.Getter;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    protected final UUID id;
    protected final LocalDateTime timeStamp;
    protected final UUID transactionId;
    protected final UUID parentTransactionId;

    public UUID getTransactionInformation() {
        return transactionId != null ? transactionId : parentTransactionId;
    }

    protected DomainEvent() {
        this.id = UUID.randomUUID();
        this.timeStamp = LocalDateTime.now();
        this.transactionId = MDC.get("transactionId") != null ? UUID.fromString(MDC.get("transactionId")) : null;
        this.parentTransactionId = MDC.get("parentTransactionId") != null ? UUID.fromString(MDC.get("parentTransactionId")) : null;
    }
}
