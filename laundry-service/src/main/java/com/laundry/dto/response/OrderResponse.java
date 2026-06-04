package com.laundry.dto.response;

import com.laundry.domain.entity.LaundryOrder;
import com.laundry.domain.enums.OrderStatus;
import com.laundry.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Data order laundry")
public class OrderResponse {

    @Schema(description = "ID unik order", example = "1")
    private Long id;

    @Schema(description = "Kode order", example = "LDR-1703123456789")
    private String orderCode;

    @Schema(description = "Nama pelanggan", example = "Budi Santoso")
    private String customerName;

    @Schema(description = "Nomor telepon pelanggan", example = "08123456789")
    private String customerPhone;

    @Schema(description = "Jenis layanan", example = "EXPRESS")
    private ServiceType serviceType;

    @Schema(description = "Nama layanan (human-readable)", example = "Express Wash")
    private String serviceDisplayName;

    @Schema(description = "Berat cucian (kg)", example = "3.5")
    private BigDecimal weightKg;

    @Schema(description = "Harga per kg (Rp)", example = "10000")
    private BigDecimal pricePerKg;

    @Schema(description = "Total harga (Rp)", example = "35000")
    private BigDecimal totalPrice;

    @Schema(description = "Status order", example = "PROCESSING")
    private OrderStatus status;

    @Schema(description = "Catatan", example = "Pisahkan baju putih")
    private String notes;

    @Schema(description = "Estimasi selesai")
    private LocalDateTime estimatedDoneAt;

    @Schema(description = "Waktu selesai actual")
    private LocalDateTime completedAt;

    @Schema(description = "Waktu order dibuat")
    private LocalDateTime createdAt;

    @Schema(description = "Waktu terakhir diupdate")
    private LocalDateTime updatedAt;

    public static OrderResponse from(LaundryOrder order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .serviceType(order.getServiceType())
                .serviceDisplayName(order.getServiceType().getDisplayName())
                .weightKg(order.getWeightKg())
                .pricePerKg(order.getPricePerKg())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .notes(order.getNotes())
                .estimatedDoneAt(order.getEstimatedDoneAt())
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
