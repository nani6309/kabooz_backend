package com.kabooz.backend.controller;

import com.kabooz.backend.dto.request.AdminOrderRequest;
import com.kabooz.backend.dto.request.RejectOrderRequest;
import com.kabooz.backend.dto.request.UpdateOrderGstRequest;
import com.kabooz.backend.dto.request.UpdateOrderStatusRequest;
import com.kabooz.backend.dto.response.DashboardStatsResponse;
import com.kabooz.backend.dto.response.OrderResponse;
import com.kabooz.backend.dto.response.OrderSummaryResponse;
import com.kabooz.backend.entity.Order;
import com.kabooz.backend.service.InvoiceService;
import com.kabooz.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Admin REST controller providing full order management operations.
 * All endpoints require a valid JWT with ROLE_ADMIN (enforced by SecurityConfig).
 *
 * <ul>
 *   <li>GET    /api/admin/dashboard                     — dashboard stats</li>
 *   <li>GET    /api/admin/orders                        — paginated order list (supports reviewStatus filter)</li>
 *   <li>GET    /api/admin/orders/{id}                   — full order detail</li>
 *   <li>POST   /api/admin/orders                        — create order (PENDING_REVIEW)</li>
 *   <li>POST   /api/admin/orders/{id}/accept            — accept order → assign invoice</li>
 *   <li>POST   /api/admin/orders/{id}/reject            — reject order</li>
 *   <li>PUT    /api/admin/orders/{id}/status            — update payment status</li>
 *   <li>PATCH  /api/admin/orders/{id}/gst              — update GSTIN</li>
 *   <li>DELETE /api/admin/orders/{id}                   — soft delete</li>
 *   <li>GET    /api/admin/orders/{id}/invoice/pdf       — download PDF</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService   orderService;
    private final InvoiceService invoiceService;

    // ── Dashboard ──────────────────────────────────────────────────────────

    /**
     * Retrieve aggregated dashboard statistics.
     *
     * @return 200 with totals for orders, revenue, monthly figures, and pending-review count
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboard() {
        log.debug("Admin dashboard stats requested");
        return ResponseEntity.ok(orderService.getDashboardStats());
    }

    // ── Order listing ──────────────────────────────────────────────────────

    /**
     * List all non-deleted orders with optional filtering and pagination.
     *
     * @param page         0-based page number (default 0)
     * @param size         page size (default 20)
     * @param status       payment status filter: all | PENDING | PAID | OVERDUE (default "all")
     * @param reviewStatus review status filter: all | PENDING_REVIEW | ACCEPTED | REJECTED (default "all")
     * @param search       free-text search on customer name, mobile, or invoice number
     * @return paginated page of order summaries
     */
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String reviewStatus,
            @RequestParam(required = false) String search) {

        log.debug("Admin list orders: page={} size={} status={} reviewStatus={} search={}",
                page, size, status, reviewStatus, search);
        return ResponseEntity.ok(orderService.listOrders(page, size, status, reviewStatus, search));
    }

    // ── Single order ───────────────────────────────────────────────────────

    /**
     * Retrieve full order details including all items and tax breakdown.
     *
     * @param id the order ID
     * @return 200 with full OrderResponse, or 404 if not found
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.debug("Admin get order id={}", id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // ── Create order ───────────────────────────────────────────────────────

    /**
     * Create a new order from the admin panel.
     * The order is created with reviewStatus=PENDING_REVIEW and no invoice number.
     * Admin must call /accept to assign an invoice and finalise.
     *
     * @param req validated admin order request
     * @return 201 Created with OrderResponse (reviewStatus=PENDING_REVIEW, invoiceNo=null)
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody AdminOrderRequest req) {
        log.info("Admin creating order for mobile={}", req.getCustomer().getMobile());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createAdminOrder(req));
    }

    // ── Accept order ───────────────────────────────────────────────────────

    /**
     * Accept a pending-review order.
     * Assigns an invoice number and transitions reviewStatus to ACCEPTED.
     *
     * @param id the order ID
     * @return 200 with updated OrderResponse (reviewStatus=ACCEPTED, invoiceNo set)
     */
    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable Long id) {
        log.info("Admin accepting order id={}", id);
        return ResponseEntity.ok(orderService.acceptOrder(id));
    }

    // ── Reject order ───────────────────────────────────────────────────────

    /**
     * Reject a pending-review order with an optional reason.
     *
     * @param id  the order ID
     * @param req rejection request (reason is optional)
     * @return 200 with updated OrderResponse (reviewStatus=REJECTED, invoiceNo=null)
     */
    @PostMapping("/orders/{id}/reject")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectOrderRequest req) {
        log.info("Admin rejecting order id={}", id);
        String reason = (req != null) ? req.getReason() : null;
        return ResponseEntity.ok(orderService.rejectOrder(id, reason));
    }

    // ── Update status ──────────────────────────────────────────────────────

    /**
     * Update an order's payment status and/or received amount.
     *
     * @param id  the order ID
     * @param req status update request
     * @return 200 with updated full OrderResponse
     */
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest req) {
        log.info("Admin updating order {} status to {}", id, req.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, req));
    }

    // ── Update GST number ──────────────────────────────────────────────────

    /**
     * Attach, update, or clear an order's GSTIN.
     * <p>
     * Body: {@code {"gstNumber": "29ABCDE1234F1Z5"}} to set, or
     * {@code {"gstNumber": null}} to clear (mark Without GST).
     *
     * @param id  the order ID
     * @param req GST update request
     * @return 200 with updated full OrderResponse
     */
    @PatchMapping("/orders/{id}/gst")
    public ResponseEntity<OrderResponse> updateGst(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderGstRequest req) {
        log.info("Admin updating GSTIN for order {} (gstNumber={})", id,
                req.getGstNumber() == null ? "null" : "<set>");
        return ResponseEntity.ok(orderService.updateOrderGst(id, req.getGstNumber()));
    }

    // ── Soft delete ────────────────────────────────────────────────────────

    /**
     * Soft-delete an order (sets {@code deleted_at} — data is retained in DB).
     *
     * @param id the order ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Admin soft-deleting order id={}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ── PDF Invoice ────────────────────────────────────────────────────────

    /**
     * Generate and stream a PDF invoice for the given order.
     * Only valid for ACCEPTED orders (those with an invoiceNo).
     * The response is streamed as an attachment for immediate download.
     *
     * @param id the order ID
     * @return 200 with {@code application/pdf} content and filename header
     * @throws IOException if PDF generation fails
     */
    @GetMapping("/orders/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) throws IOException {
        log.info("PDF invoice requested for order id={}", id);

        OrderResponse orderDto = orderService.getOrderById(id);

        if (orderDto.getInvoiceNo() == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }

        // Load the full entity for PDF generation (we need the entity for InvoiceService)
        // Re-fetch the Order entity through the service
        Order order = buildOrderFromDto(orderDto, id);
        byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "Invoice-" + orderDto.getInvoiceNo() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // ── Private helper ─────────────────────────────────────────────────────

    /**
     * Reconstruct an Order entity from its DTO for PDF generation.
     * This avoids a second DB call while keeping InvoiceService entity-based.
     *
     * @param dto the full order DTO
     * @param id  the order ID
     * @return reconstructed Order entity (not managed by JPA)
     */
    private Order buildOrderFromDto(OrderResponse dto, Long id) {
        com.kabooz.backend.entity.Customer customer = com.kabooz.backend.entity.Customer.builder()
                .id(dto.getCustomer().getId())
                .name(dto.getCustomer().getName())
                .mobile(dto.getCustomer().getMobile())
                .address(dto.getCustomer().getAddress())
                .placeOfSupply(dto.getCustomer().getPlaceOfSupply())
                .gstNumber(dto.getCustomer().getGstNumber())
                .build();

        Order order = Order.builder()
                .id(id)
                .invoiceNo(dto.getInvoiceNo())
                .invoiceDate(dto.getInvoiceDate())
                .dueDate(dto.getDueDate())
                .customer(customer)
                .taxableAmount(dto.getTaxableAmount())
                .cgst(dto.getCgst())
                .sgst(dto.getSgst())
                .grandTotal(dto.getGrandTotal())
                .receivedAmount(dto.getReceivedAmount())
                .withGst(Boolean.TRUE.equals(dto.getWithGst()))
                .gstNumber(dto.getGstNumber())
                .status(Order.OrderStatus.valueOf(dto.getStatus()))
                .source(Order.OrderSource.valueOf(dto.getSource()))
                .reviewStatus(Order.ReviewStatus.valueOf(dto.getReviewStatus()))
                .notes(dto.getNotes())
                .createdAt(dto.getCreatedAt())
                .build();

        if (dto.getItems() != null) {
            for (OrderResponse.OrderItemDto itemDto : dto.getItems()) {
                com.kabooz.backend.entity.OrderItem item = com.kabooz.backend.entity.OrderItem.builder()
                        .id(itemDto.getId())
                        .bottleType(com.kabooz.backend.entity.OrderItem.BottleType.valueOf(itemDto.getBottleType()))
                        .flavor(itemDto.getFlavor())
                        .pricePerBottle(itemDto.getPricePerBottle())
                        .quantity(itemDto.getQuantity())
                        .bottlesPerUnit(itemDto.getBottlesPerUnit())
                        .ratePerUnit(itemDto.getRatePerUnit())
                        .taxPerUnit(itemDto.getTaxPerUnit())
                        .totalPerUnit(itemDto.getTotalPerUnit())
                        .taxableSubtotal(itemDto.getTaxableSubtotal())
                        .taxSubtotal(itemDto.getTaxSubtotal())
                        .lineTotal(itemDto.getLineTotal())
                        .build();
                order.addItem(item);
            }
        }

        return order;
    }
}
