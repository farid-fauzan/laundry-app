package com.laundry.service.impl;

import com.laundry.domain.entity.LaundryOrder;
import com.laundry.domain.enums.OrderStatus;
import com.laundry.dto.request.OrderRequest;
import com.laundry.dto.request.UpdateStatusRequest;
import com.laundry.dto.response.OrderResponse;
import com.laundry.dto.response.OrderStatsResponse;
import com.laundry.event.OrderEvent;
import com.laundry.event.OrderEventType;
import com.laundry.exception.OrderNotFoundException;
import com.laundry.kafka.OrderEventPublisher;
import com.laundry.repository.OrderRepository;
import com.laundry.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service                   // Spring IoC: stereotype annotation → registered as bean
@RequiredArgsConstructor   // Spring IoC: constructor injection via Lombok
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    // Spring IoC: injected by constructor (no @Autowired needed with @RequiredArgsConstructor)
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orderList", allEntries = true),
        @CacheEvict(value = "orderStats", allEntries = true)
    })
    public OrderResponse createOrder(OrderRequest request) {
        LaundryOrder order = LaundryOrder.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .serviceType(request.getServiceType())
                .weightKg(request.getWeightKg())
                .pricePerKg(request.getServiceType().getPricePerKg())
                .totalPrice(request.getServiceType().getPricePerKg()
                        .multiply(request.getWeightKg()))
                .notes(request.getNotes())
                .estimatedDoneAt(request.getEstimatedDoneAt())
                .status(OrderStatus.RECEIVED)
                .build();

        LaundryOrder saved = orderRepository.save(order);
        log.info("Order created: {}", saved.getOrderCode());

        // Kafka: publish event asynchronously
        eventPublisher.publish(buildEvent(saved, OrderEventType.ORDER_CREATED, null));

        return OrderResponse.from(saved);
    }

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    @Override
    @Cacheable(value = "orders", key = "#id")  // Redis: cache by id
    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Cacheable(value = "orders", key = "'code:' + #orderCode")
    public OrderResponse getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderCode));
    }

    @Override
    @Cacheable(
        value = "orderList",
        key = "#status + ':' + #customerName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<OrderResponse> searchOrders(OrderStatus status, String customerName, Pageable pageable) {
        String statusStr = status != null ? status.name() : null;
        return orderRepository
                .searchOrders(statusStr, customerName, pageable)
                .map(OrderResponse::from);
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    @Caching(
        put  = { @CachePut(value = "orders", key = "#id") },
        evict = {
            @CacheEvict(value = "orderList",  allEntries = true),
            @CacheEvict(value = "orderStats", allEntries = true)
        }
    )
    public OrderResponse updateStatus(Long id, UpdateStatusRequest request) {
        LaundryOrder order = findEntityById(id);
        OrderStatus previous = order.getStatus();

        validateStatusTransition(previous, request.getStatus());

        order.setStatus(request.getStatus());
        if (request.getNotes() != null) order.setNotes(request.getNotes());
        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        LaundryOrder updated = orderRepository.save(order);

        OrderEventType eventType = request.getStatus() == OrderStatus.DELIVERED
                ? OrderEventType.ORDER_COMPLETED
                : request.getStatus() == OrderStatus.CANCELLED
                    ? OrderEventType.ORDER_CANCELLED
                    : OrderEventType.ORDER_STATUS_UPDATED;

        eventPublisher.publish(buildEvent(updated, eventType, previous));

        return OrderResponse.from(updated);
    }

    @Override
    @Transactional
    @Caching(
        put  = { @CachePut(value = "orders", key = "#id") },
        evict = {
            @CacheEvict(value = "orderList",  allEntries = true),
            @CacheEvict(value = "orderStats", allEntries = true)
        }
    )
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        LaundryOrder order = findEntityById(id);

        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot edit order in status: " + order.getStatus());
        }

        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setServiceType(request.getServiceType());
        order.setWeightKg(request.getWeightKg());
        order.setPricePerKg(request.getServiceType().getPricePerKg());
        order.setTotalPrice(request.getServiceType().getPricePerKg().multiply(request.getWeightKg()));
        order.setNotes(request.getNotes());
        order.setEstimatedDoneAt(request.getEstimatedDoneAt());

        return OrderResponse.from(orderRepository.save(order));
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orders",     key = "#id"),
        @CacheEvict(value = "orderList",  allEntries = true),
        @CacheEvict(value = "orderStats", allEntries = true)
    })
    public void deleteOrder(Long id) {
        LaundryOrder order = findEntityById(id);
        if (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.DONE) {
            throw new IllegalStateException("Cannot delete order in status: " + order.getStatus());
        }
        orderRepository.delete(order);
        log.info("Order deleted: {}", order.getOrderCode());
    }

    // ---------------------------------------------------------------
    // STATS — Java Stream API for in-memory aggregation
    // ---------------------------------------------------------------

    @Override
    @Cacheable(value = "orderStats", key = "'global'")
    public OrderStatsResponse getStatistics() {
        List<LaundryOrder> all = orderRepository.findAll();

        // Java Stream: group by status, count each
        Map<String, Long> countByStatus = all.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));

        // Java Stream: group by service type, count and sum revenue
        Map<String, Long> countByServiceType = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        o -> o.getServiceType().name(),
                        Collectors.counting()
                ));

        Map<String, BigDecimal> revenueByServiceType = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        o -> o.getServiceType().name(),
                        Collectors.mapping(
                                LaundryOrder::getTotalPrice,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Java Stream: compute totals from non-cancelled orders
        BigDecimal totalRevenue = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(LaundryOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long nonCancelledCount = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .count();

        BigDecimal avgValue = nonCancelledCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(nonCancelledCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return OrderStatsResponse.builder()
                .totalOrders(all.size())
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgValue)
                .countByStatus(countByStatus)
                .countByServiceType(countByServiceType)
                .revenueByServiceType(revenueByServiceType)
                .build();
    }

    // ---------------------------------------------------------------
    // REPORTS — delegate to native SQL queries
    // ---------------------------------------------------------------

    @Override
    public List<Map<String, Object>> getRevenueReport(LocalDateTime from, LocalDateTime to) {
        return orderRepository.getRevenueByServiceType(from, to);
    }

    @Override
    public List<Map<String, Object>> getMonthlyRevenue(LocalDateTime from, LocalDateTime to) {
        return orderRepository.getMonthlyRevenueReport(from, to);
    }

    @Override
    public List<Map<String, Object>> getTopCustomers(int minOrders, int limit) {
        return orderRepository.getTopCustomers(minOrders, limit);
    }

    @Override
    public List<Map<String, Object>> getCustomerHistory(String phone) {
        return orderRepository.getCustomerHistory(phone);
    }

    @Override
    public List<OrderResponse> getOverdueOrders() {
        // Java Stream: map entity list to DTO list
        return orderRepository.findOverdueOrders().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // BULK
    // ---------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "orders",     allEntries = true),
        @CacheEvict(value = "orderList",  allEntries = true),
        @CacheEvict(value = "orderStats", allEntries = true)
    })
    public int bulkUpdateStatus(List<Long> ids, OrderStatus newStatus) {
        int updated = orderRepository.bulkUpdateStatus(ids, newStatus.name());
        log.info("Bulk status update: {} orders → {}", updated, newStatus);
        return updated;
    }

    // ---------------------------------------------------------------
    // PRIVATE HELPERS
    // ---------------------------------------------------------------

    private LaundryOrder findEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Java Stream: define allowed transitions as a map, then validate
        Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
                OrderStatus.RECEIVED,   List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
                OrderStatus.PROCESSING, List.of(OrderStatus.DONE, OrderStatus.CANCELLED),
                OrderStatus.DONE,       List.of(OrderStatus.DELIVERED),
                OrderStatus.DELIVERED,  List.of(),
                OrderStatus.CANCELLED,  List.of()
        );

        boolean isValid = allowed.getOrDefault(current, List.of())
                .stream()
                .anyMatch(s -> s == next);

        if (!isValid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: %s → %s", current, next));
        }
    }

    private OrderEvent buildEvent(LaundryOrder order, OrderEventType type, OrderStatus previous) {
        return OrderEvent.builder()
                .eventType(type)
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .serviceType(order.getServiceType().name())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .previousStatus(previous)
                .notes(order.getNotes())
                .build();
    }
}
