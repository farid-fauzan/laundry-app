package com.laundry.dto.request;

import com.laundry.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body untuk mengubah status order")
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(
        description = "Status baru order",
        example = "PROCESSING",
        allowableValues = {"RECEIVED", "PROCESSING", "DONE", "DELIVERED", "CANCELLED"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OrderStatus status;

    @Schema(description = "Catatan perubahan status", example = "Sedang dalam proses pencucian")
    private String notes;
}
