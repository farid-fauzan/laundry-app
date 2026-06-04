package com.laundry.domain.entity;

import com.laundry.domain.enums.OrderStatus;
import com.laundry.domain.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "laundry_orders", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_customer_phone", columnList = "customer_phone"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaundryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "order_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false, length = 20)
    private String orderCode;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.RECEIVED;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "estimated_done_at")
    private LocalDateTime estimatedDoneAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (orderCode == null) {
            orderCode = "LDR-" + System.currentTimeMillis();
        }
        if (pricePerKg == null && serviceType != null) {
            pricePerKg = serviceType.getPricePerKg();
        }
        if (totalPrice == null && pricePerKg != null && weightKg != null) {
            totalPrice = pricePerKg.multiply(weightKg);
        }
    }
}
