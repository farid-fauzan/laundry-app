package com.laundry.service;

import com.laundry.domain.enums.OrderStatus;
import com.laundry.dto.request.OrderRequest;
import com.laundry.dto.request.UpdateStatusRequest;
import com.laundry.dto.response.OrderResponse;
import com.laundry.dto.response.OrderStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getOrderByCode(String orderCode);

    Page<OrderResponse> searchOrders(OrderStatus status, String customerName, Pageable pageable);

    OrderResponse updateStatus(Long id, UpdateStatusRequest request);

    OrderResponse updateOrder(Long id, OrderRequest request);

    void deleteOrder(Long id);

    OrderStatsResponse getStatistics();

    List<Map<String, Object>> getRevenueReport(LocalDateTime from, LocalDateTime to);

    List<Map<String, Object>> getMonthlyRevenue(LocalDateTime from, LocalDateTime to);

    List<Map<String, Object>> getTopCustomers(int minOrders, int limit);

    List<Map<String, Object>> getCustomerHistory(String phone);

    List<OrderResponse> getOverdueOrders();

    int bulkUpdateStatus(List<Long> ids, OrderStatus newStatus);
}
