package com.laundry.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class OrderStatsResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByServiceType;
    private Map<String, BigDecimal> revenueByServiceType;
}
