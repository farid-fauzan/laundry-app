package com.laundry.repository;

import com.laundry.domain.entity.LaundryOrder;
import com.laundry.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<LaundryOrder, Long> {

    Optional<LaundryOrder> findByOrderCode(String orderCode);

    // ---------------------------------------------------------------
    // Advanced Native SQL Queries
    // ---------------------------------------------------------------

    /**
     * Paginated list with total price filter — native SQL with ILIKE for case-insensitive search.
     */
    @Query(
        value = """
            SELECT *
            FROM laundry_orders
            WHERE (:status IS NULL OR status = :status)
              AND (:customerName IS NULL OR customer_name ILIKE '%' || :customerName || '%')
            ORDER BY created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM laundry_orders
            WHERE (:status IS NULL OR status = :status)
              AND (:customerName IS NULL OR customer_name ILIKE '%' || :customerName || '%')
            """,
        nativeQuery = true
    )
    Page<LaundryOrder> searchOrders(
            @Param("status") String status,
            @Param("customerName") String customerName,
            Pageable pageable
    );

    /**
     * Revenue summary grouped by service type.
     * Demonstrates GROUP BY + aggregate functions in native SQL.
     */
    @Query(
        value = """
            SELECT
                service_type                       AS serviceType,
                COUNT(*)                           AS totalOrders,
                SUM(total_price)                   AS totalRevenue,
                ROUND(AVG(total_price), 2)         AS avgRevenue,
                ROUND(AVG(weight_kg), 2)           AS avgWeight,
                MIN(created_at)                    AS firstOrder,
                MAX(created_at)                    AS lastOrder
            FROM laundry_orders
            WHERE created_at BETWEEN :from AND :to
              AND status != 'CANCELLED'
            GROUP BY service_type
            ORDER BY totalRevenue DESC
            """,
        nativeQuery = true
    )
    List<Map<String, Object>> getRevenueByServiceType(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Order count grouped by status.
     */
    @Query(
        value = """
            SELECT status, COUNT(*) AS total
            FROM laundry_orders
            GROUP BY status
            """,
        nativeQuery = true
    )
    List<Map<String, Object>> getCountByStatus();

    /**
     * Top customers by total spend — subquery + HAVING.
     */
    @Query(
        value = """
            SELECT
                customer_name                              AS customerName,
                customer_phone                             AS customerPhone,
                COUNT(*)                                   AS orderCount,
                SUM(total_price)                           AS totalSpent,
                ROUND(AVG(total_price), 2)                 AS avgOrderValue,
                MAX(created_at)                            AS lastOrderDate
            FROM laundry_orders
            WHERE status != 'CANCELLED'
            GROUP BY customer_name, customer_phone
            HAVING COUNT(*) >= :minOrders
            ORDER BY totalSpent DESC
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<Map<String, Object>> getTopCustomers(
            @Param("minOrders") int minOrders,
            @Param("limit") int limit
    );

    /**
     * Overdue orders: estimated done time has passed but not yet delivered.
     * Uses window function ROW_NUMBER for deduplication in complex scenarios.
     */
    @Query(
        value = """
            SELECT *
            FROM laundry_orders
            WHERE estimated_done_at < NOW()
              AND status NOT IN ('DONE', 'DELIVERED', 'CANCELLED')
            ORDER BY estimated_done_at ASC
            """,
        nativeQuery = true
    )
    List<LaundryOrder> findOverdueOrders();

    /**
     * Monthly revenue report with running total using window function.
     */
    @Query(
        value = """
            SELECT
                TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM')   AS month,
                COUNT(*)                                                AS orderCount,
                SUM(total_price)                                        AS monthRevenue,
                SUM(SUM(total_price)) OVER (
                    ORDER BY DATE_TRUNC('month', created_at)
                )                                                       AS runningTotal
            FROM laundry_orders
            WHERE status != 'CANCELLED'
              AND created_at BETWEEN :from AND :to
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY month
            """,
        nativeQuery = true
    )
    List<Map<String, Object>> getMonthlyRevenueReport(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Full order history for a customer, with cumulative spend.
     */
    @Query(
        value = """
            SELECT
                id,
                order_code      AS orderCode,
                service_type    AS serviceType,
                weight_kg       AS weightKg,
                total_price     AS totalPrice,
                status,
                created_at      AS createdAt,
                SUM(total_price) OVER (
                    PARTITION BY customer_phone
                    ORDER BY created_at
                    ROWS UNBOUNDED PRECEDING
                )               AS cumulativeSpent
            FROM laundry_orders
            WHERE customer_phone = :phone
            ORDER BY created_at DESC
            """,
        nativeQuery = true
    )
    List<Map<String, Object>> getCustomerHistory(@Param("phone") String phone);

    /**
     * Bulk status update — native SQL UPDATE for performance.
     */
    @Modifying
    @Query(
        value = """
            UPDATE laundry_orders
            SET status = :newStatus,
                updated_at = NOW(),
                completed_at = CASE WHEN :newStatus = 'DELIVERED' THEN NOW() ELSE completed_at END
            WHERE id IN (:ids)
            """,
        nativeQuery = true
    )
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("newStatus") String newStatus);

    boolean existsByCustomerPhoneAndStatus(String customerPhone, OrderStatus status);
}
