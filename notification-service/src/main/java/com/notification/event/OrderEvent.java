package com.notification.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OrderEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String serviceType;
    private BigDecimal totalPrice;
    private String status;
    private String previousStatus;
    private String notes;
    private LocalDateTime occurredAt;
}
