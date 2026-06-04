package com.laundry.controller;

import com.laundry.domain.enums.OrderStatus;
import com.laundry.dto.request.OrderRequest;
import com.laundry.dto.request.UpdateStatusRequest;
import com.laundry.dto.response.ApiResponse;
import com.laundry.dto.response.OrderResponse;
import com.laundry.dto.response.OrderStatsResponse;
import com.laundry.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "CRUD & manajemen status order laundry")
public class OrderController {

    private final OrderService orderService;

    // ---------------------------------------------------------------
    // CRUD
    // ---------------------------------------------------------------

    @PostMapping
    @Operation(
        summary = "Buat order baru",
        description = "Membuat order laundry baru. Harga dihitung otomatis dari serviceType × weightKg. " +
                      "Event ORDER_CREATED dikirim ke Kafka → notification-service."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Order berhasil dibuat"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validasi gagal",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(examples = @ExampleObject(value = """
            {
              "customerName": "Budi Santoso",
              "customerPhone": "08123456789",
              "serviceType": "EXPRESS",
              "weightKg": 3.5,
              "notes": "Pisahkan baju putih",
              "estimatedDoneAt": "2024-12-25T14:00:00"
            }
            """))
    )
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order created successfully", created));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get order by ID",
        description = "Mengambil detail order berdasarkan ID. **Data di-cache di Redis** (TTL 10 menit)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data order"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order tidak ditemukan"),
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @Parameter(description = "ID order", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Order retrieved", orderService.getOrderById(id)));
    }

    @GetMapping("/code/{orderCode}")
    @Operation(summary = "Get order by kode order", description = "Ambil order berdasarkan kode unik (misal: LDR-1703123456789)")
    public ResponseEntity<ApiResponse<OrderResponse>> getByCode(
            @Parameter(description = "Kode order", example = "LDR-1703123456789") @PathVariable String orderCode) {
        return ResponseEntity.ok(
                ApiResponse.ok("Order retrieved", orderService.getOrderByCode(orderCode)));
    }

    @GetMapping
    @Operation(
        summary = "Cari & list order",
        description = "List semua order dengan filter status & nama pelanggan. " +
                      "Mendukung pagination & sorting. **Native SQL ILIKE** untuk pencarian case-insensitive. " +
                      "**Hasil di-cache** di Redis (TTL 2 menit)."
    )
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> search(
            @Parameter(description = "Filter status", example = "PROCESSING") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter nama pelanggan (partial match)", example = "Budi") @RequestParam(required = false) String customerName,
            @Parameter(description = "Nomor halaman (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Jumlah item per halaman", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field untuk sorting", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Arah sorting") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> result = orderService.searchOrders(status, customerName, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved", result));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update data order",
        description = "Update data order. Hanya bisa dilakukan jika status masih **RECEIVED**. Cache otomatis diperbarui."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order berhasil diupdate"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Order sudah tidak bisa diedit (status bukan RECEIVED)"),
    })
    public ResponseEntity<ApiResponse<OrderResponse>> update(
            @Parameter(description = "ID order", example = "1") @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Order updated", orderService.updateOrder(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update status order",
        description = """
            Ubah status order sesuai alur berikut:

            ```
            RECEIVED → PROCESSING → DONE → DELIVERED
                     ↘           ↘
                      CANCELLED   CANCELLED
            ```

            Status tidak bisa dibalik. Setiap perubahan dikirim sebagai event ke Kafka.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status berhasil diubah"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Transisi status tidak valid"),
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = @Content(examples = @ExampleObject(value = """
            {
              "status": "PROCESSING",
              "notes": "Mulai proses pencucian"
            }
            """))
    )
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @Parameter(description = "ID order", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Status updated", orderService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Hapus order",
        description = "Hapus order. Tidak bisa dihapus jika status **PROCESSING** atau **DONE**."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order dihapus"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Order tidak bisa dihapus pada status ini"),
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID order", example = "1") @PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.ok("Order deleted", null));
    }

    // ---------------------------------------------------------------
    // BULK
    // ---------------------------------------------------------------

    @PatchMapping("/bulk/status")
    @Operation(
        summary = "Bulk update status",
        description = "Update status banyak order sekaligus menggunakan **native SQL UPDATE** (lebih efisien dari loop). Cache semua order di-invalidate."
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkUpdateStatus(
            @Parameter(description = "List ID order, pisahkan dengan koma", example = "1,2,3") @RequestParam List<Long> ids,
            @Parameter(description = "Status baru", example = "PROCESSING") @RequestParam OrderStatus status) {
        int updated = orderService.bulkUpdateStatus(ids, status);
        return ResponseEntity.ok(
                ApiResponse.ok("Bulk update completed", Map.of("updatedCount", updated)));
    }

    // ---------------------------------------------------------------
    // STATS & REPORTS
    // ---------------------------------------------------------------

    @GetMapping("/stats")
    @Operation(
        summary = "Statistik global",
        description = "Ringkasan statistik: total order, revenue, rata-rata nilai order, breakdown per status & service type. " +
                      "Dihitung menggunakan **Java Stream API** (groupingBy, reduce). **Di-cache** 5 menit."
    )
    @Tag(name = "Reports")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.ok("Statistics retrieved", orderService.getStatistics()));
    }

    @GetMapping("/reports/revenue")
    @Operation(
        summary = "Revenue per service type",
        description = "Laporan revenue dikelompokkan per jenis layanan dalam rentang tanggal. " +
                      "Menggunakan **Native SQL GROUP BY + SUM/AVG/MIN/MAX**."
    )
    @Tag(name = "Reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueReport(
            @Parameter(description = "Dari tanggal (ISO 8601)", example = "2024-01-01T00:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Sampai tanggal (ISO 8601)", example = "2024-12-31T23:59:59") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                ApiResponse.ok("Revenue report", orderService.getRevenueReport(from, to)));
    }

    @GetMapping("/reports/monthly")
    @Operation(
        summary = "Revenue bulanan + running total",
        description = "Laporan revenue per bulan lengkap dengan **running total** menggunakan **PostgreSQL Window Function** `SUM() OVER()`."
    )
    @Tag(name = "Reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyRevenue(
            @Parameter(description = "Dari tanggal", example = "2024-01-01T00:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Sampai tanggal", example = "2024-12-31T23:59:59") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                ApiResponse.ok("Monthly revenue", orderService.getMonthlyRevenue(from, to)));
    }

    @GetMapping("/reports/top-customers")
    @Operation(
        summary = "Top pelanggan",
        description = "Daftar pelanggan terbaik berdasarkan total belanja. Menggunakan **Native SQL GROUP BY + HAVING + ORDER BY**."
    )
    @Tag(name = "Reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopCustomers(
            @Parameter(description = "Minimum jumlah order", example = "2") @RequestParam(defaultValue = "2") int minOrders,
            @Parameter(description = "Maksimal hasil", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                ApiResponse.ok("Top customers", orderService.getTopCustomers(minOrders, limit)));
    }

    @GetMapping("/customers/{phone}/history")
    @Operation(
        summary = "Riwayat order pelanggan",
        description = "Semua order dari satu nomor HP beserta **cumulative spend** menggunakan **PostgreSQL Window Function** `SUM() OVER(PARTITION BY)`."
    )
    @Tag(name = "Customers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerHistory(
            @Parameter(description = "Nomor HP pelanggan", example = "08123456789") @PathVariable String phone) {
        return ResponseEntity.ok(
                ApiResponse.ok("Customer history", orderService.getCustomerHistory(phone)));
    }

    @GetMapping("/overdue")
    @Operation(
        summary = "Order terlambat",
        description = "Order yang melewati estimatedDoneAt tetapi belum berstatus DONE atau DELIVERED. Query langsung ke DB via native SQL."
    )
    @Tag(name = "Reports")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOverdueOrders() {
        return ResponseEntity.ok(
                ApiResponse.ok("Overdue orders", orderService.getOverdueOrders()));
    }
}
