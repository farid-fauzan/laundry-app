package com.laundry.event;

import com.laundry.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String eventId;
    private OrderEventType eventType;
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String serviceType;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private String notes;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
