package com.laundry.domain.enums;

import java.math.BigDecimal;

public enum ServiceType {

    REGULAR("Regular Wash", new BigDecimal("5000")),
    EXPRESS("Express Wash", new BigDecimal("10000")),
    DRY_CLEAN("Dry Clean", new BigDecimal("15000"));

    private final String displayName;
    private final BigDecimal pricePerKg;

    ServiceType(String displayName, BigDecimal pricePerKg) {
        this.displayName = displayName;
        this.pricePerKg = pricePerKg;
    }

    public String getDisplayName() { return displayName; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
}
