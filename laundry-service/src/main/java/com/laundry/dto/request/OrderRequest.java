package com.laundry.dto.request;

import com.laundry.domain.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Request body untuk membuat atau mengupdate order laundry")
public class OrderRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    @Schema(description = "Nama pelanggan", example = "Budi Santoso", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "Invalid phone format")
    @Schema(description = "Nomor telepon pelanggan", example = "08123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerPhone;

    @NotNull(message = "Service type is required")
    @Schema(
        description = "Jenis layanan laundry",
        example = "EXPRESS",
        allowableValues = {"REGULAR", "EXPRESS", "DRY_CLEAN"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ServiceType serviceType;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg")
    @DecimalMax(value = "100.0", message = "Weight cannot exceed 100 kg")
    @Schema(description = "Berat cucian dalam kg", example = "3.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal weightKg;

    @Size(max = 500)
    @Schema(description = "Catatan tambahan", example = "Pisahkan baju putih dan berwarna")
    private String notes;

    @Schema(description = "Estimasi selesai (ISO 8601)", example = "2024-12-25T14:00:00")
    private LocalDateTime estimatedDoneAt;
}
