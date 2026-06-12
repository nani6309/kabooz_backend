package com.kabooz.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard statistics response for the admin panel home screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalReceived;
    private long pendingCount;
    private long overdueCount;
    private long thisMonthOrders;
    private BigDecimal thisMonthRevenue;
}
