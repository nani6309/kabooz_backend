package com.kabooz.backend.service;

import com.kabooz.backend.dto.request.AdminOrderRequest;
import com.kabooz.backend.dto.request.PlaceOrderRequest;
import com.kabooz.backend.dto.request.UpdateOrderGstRequest;
import com.kabooz.backend.dto.request.UpdateOrderStatusRequest;
import com.kabooz.backend.dto.response.DashboardStatsResponse;
import com.kabooz.backend.dto.response.OrderResponse;
import com.kabooz.backend.dto.response.OrderSummaryResponse;
import com.kabooz.backend.dto.response.PlaceOrderResponse;
import com.kabooz.backend.entity.*;
import com.kabooz.backend.entity.Order.OrderSource;
import com.kabooz.backend.entity.Order.OrderStatus;
import com.kabooz.backend.entity.Order.ReviewStatus;
import com.kabooz.backend.entity.OrderItem.BottleType;
import com.kabooz.backend.exception.OrderNotFoundException;
import com.kabooz.backend.repository.CustomerRepository;
import com.kabooz.backend.repository.InvoiceCounterRepository;
import com.kabooz.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Service handling all order lifecycle operations:
 * creation (public + admin), review (accept/reject), retrieval,
 * status updates, and soft deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository          orderRepository;
    private final CustomerRepository       customerRepository;
    private final InvoiceCounterRepository invoiceCounterRepository;
    private final PricingService           pricingService;

    // ── Public — Place order ───────────────────────────────────────────────

    /**
     * Create a new order from the public homepage.
     * Public orders bypass the review workflow and go straight to ACCEPTED
     * (they are created by the public — no admin review needed since the
     * admin already validates incoming orders via the new-orders queue).
     * Pricing is fully recalculated server-side; frontend totals are ignored.
     * Customers are upserted by mobile number.
     *
     * @param req the validated public order request
     * @return lightweight response with invoice number and grand total
     */
    @Transactional
    public PlaceOrderResponse placePublicOrder(PlaceOrderRequest req) {
        log.info("Public order from mobile={}", req.getCustomer().getMobile());

        // Upsert customer
        Customer customer = upsertCustomer(
                req.getCustomer().getName(),
                req.getCustomer().getMobile(),
                req.getCustomer().getAddress(),
                req.getCustomer().getPlaceOfSupply(),
                null
        );

        // Build order — public orders enter PENDING_REVIEW, invoiceNo assigned on accept
        Order order = Order.builder()
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .customer(customer)
                .customerShopName(req.getCustomer().getCustomerShopName())
                .notes(req.getNotes())
                .withGst(false)
                .gstNumber(null)
                .source(OrderSource.HOMEPAGE)
                .status(OrderStatus.PENDING)
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .build();

        // Build and price items
        for (PlaceOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            BottleType bottleType = pricingService.parseBottleType(itemReq.getBottleType());
            // Strict catalogue validation for public orders
            pricingService.validateCataloguePrice(bottleType, itemReq.getPricePerBottle());

            OrderItem item = OrderItem.builder()
                    .bottleType(bottleType)
                    .flavor(itemReq.getFlavor())
                    .pricePerBottle(itemReq.getPricePerBottle())
                    .quantity(itemReq.getQuantity())
                    .build();
            pricingService.calculateItemPricing(item);
            order.addItem(item);
        }

        aggregateOrderTotals(order);
        Order saved = orderRepository.save(order);
        log.info("Public order saved: id={} grandTotal={}", saved.getId(), saved.getGrandTotal());

        return PlaceOrderResponse.builder()
                .id(saved.getId())
                .invoiceNo(saved.getInvoiceNo()) // null until accepted
                .grandTotal(saved.getGrandTotal())
                .status(saved.getStatus().name())
                .message("Order placed successfully! We will contact you shortly.")
                .build();
    }

    // ── Admin — Create order ───────────────────────────────────────────────

    /**
     * Create a new order from the admin panel in PENDING_REVIEW state.
     * No invoice number is assigned yet — admin must call acceptOrder to finalise.
     * Admin may set any valid price; the {@code receivedAmount} and {@code dueDate} are honoured.
     *
     * @param req the validated admin order request
     * @return full order response (reviewStatus = PENDING_REVIEW, invoiceNo = null)
     */
    @Transactional
    public OrderResponse createAdminOrder(AdminOrderRequest req) {
        log.info("Admin order creation for mobile={}", req.getCustomer().getMobile());

        // Resolve GSTIN: prefer the nested customer.gstNumber (frontend contract);
        // fall back to the legacy top-level gstNumber for backwards compatibility.
        String rawGst = req.getCustomer().getGstNumber();
        if (rawGst == null || rawGst.isBlank()) {
            rawGst = req.getGstNumber();
        }
        boolean withGst = (rawGst != null && !rawGst.isBlank())
                || Boolean.TRUE.equals(req.getWithGst());
        String gstNumber = normalizeGstNumber(rawGst, withGst);
        // If normalize returned null (no GST) honour that as withGst=false
        if (gstNumber == null) {
            withGst = false;
        }

        Customer customer = upsertCustomer(
                req.getCustomer().getName(),
                req.getCustomer().getMobile(),
                req.getCustomer().getAddress(),
                req.getCustomer().getPlaceOfSupply(),
                gstNumber
        );

        LocalDate dueDate = req.getDueDate() != null
                ? req.getDueDate()
                : LocalDate.now().plusDays(30);

        // No invoiceNo assigned — that happens on accept
        Order order = Order.builder()
                .invoiceDate(LocalDate.now())
                .dueDate(dueDate)
                .customer(customer)
                .customerShopName(req.getCustomer().getCustomerShopName())
                .notes(req.getNotes())
                .receivedAmount(req.getReceivedAmount() != null
                        ? req.getReceivedAmount()
                        : BigDecimal.ZERO)
                .withGst(withGst)
                .gstNumber(gstNumber)
                .source(OrderSource.ADMIN)
                .status(OrderStatus.PENDING)
                .reviewStatus(ReviewStatus.PENDING_REVIEW)
                .build();

        for (AdminOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            BottleType bottleType = pricingService.parseBottleType(itemReq.getBottleType());
            OrderItem item = OrderItem.builder()
                    .bottleType(bottleType)
                    .flavor(itemReq.getFlavor())
                    .pricePerBottle(itemReq.getPricePerBottle())
                    .quantity(itemReq.getQuantity())
                    .build();
            pricingService.calculateItemPricing(item);
            order.addItem(item);
        }

        aggregateOrderTotals(order);

        Order saved = orderRepository.save(order);
        log.info("Admin order created (pending review): id={}", saved.getId());
        return toOrderResponse(saved);
    }

    // ── Admin — Accept order ───────────────────────────────────────────────

    /**
     * Accept a pending-review order.
     * Assigns an invoice number and transitions reviewStatus → ACCEPTED.
     * Auto-sets payment status based on received amount.
     *
     * @param id the order ID
     * @return updated full order response (reviewStatus = ACCEPTED, invoiceNo set)
     * @throws OrderNotFoundException   if order not found
     * @throws IllegalStateException    if order is not in PENDING_REVIEW state
     */
    @Transactional
    public OrderResponse acceptOrder(Long id) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getReviewStatus() != ReviewStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Order " + id + " is already " + order.getReviewStatus().name().toLowerCase()
                            + " and cannot be accepted again.");
        }

        // Assign invoice number and mark as accepted
        order.setInvoiceNo(nextInvoiceNumber());
        order.setReviewStatus(ReviewStatus.ACCEPTED);

        // Auto-set payment status based on received amount
        autoSetStatus(order);

        Order saved = orderRepository.save(order);
        log.info("Order {} accepted → invoiceNo={}", id, saved.getInvoiceNo());
        return toOrderResponse(saved);
    }

    // ── Admin — Reject order ───────────────────────────────────────────────

    /**
     * Reject a pending-review order.
     * Sets reviewStatus → REJECTED and stores the optional reason.
     * No invoice number is assigned.
     *
     * @param id     the order ID
     * @param reason optional rejection reason (may be null or blank)
     * @return updated full order response (reviewStatus = REJECTED, invoiceNo = null)
     * @throws OrderNotFoundException if order not found
     * @throws IllegalStateException  if order is not in PENDING_REVIEW state
     */
    @Transactional
    public OrderResponse rejectOrder(Long id, String reason) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getReviewStatus() != ReviewStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Order " + id + " is already " + order.getReviewStatus().name().toLowerCase()
                            + " and cannot be rejected again.");
        }

        order.setReviewStatus(ReviewStatus.REJECTED);
        order.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : null);

        Order saved = orderRepository.save(order);
        log.info("Order {} rejected. Reason: {}", id, saved.getRejectionReason());
        return toOrderResponse(saved);
    }

    // ── Admin — List orders ────────────────────────────────────────────────

    /**
     * List all active (non-deleted) orders with optional filtering and pagination.
     *
     * @param page         0-based page number
     * @param size         page size (default 20)
     * @param status       optional payment status filter: "all" | "PENDING" | "PAID" | "OVERDUE"
     * @param reviewStatus optional review status filter: "all" | "PENDING_REVIEW" | "ACCEPTED" | "REJECTED"
     * @param search       optional free-text search on customer name, mobile, or invoice number
     * @return paginated order summaries
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrders(int page, int size, String status, String reviewStatus, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        OrderStatus statusFilter = null;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                statusFilter = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        ReviewStatus reviewStatusFilter = null;
        if (reviewStatus != null && !reviewStatus.isBlank() && !"all".equalsIgnoreCase(reviewStatus)) {
            try {
                reviewStatusFilter = ReviewStatus.valueOf(reviewStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid reviewStatus: " + reviewStatus);
            }
        }

        String searchTerm = (search != null && !search.isBlank()) ? search : null;
        Page<Order> orders = orderRepository.findAllActive(statusFilter, reviewStatusFilter, searchTerm, pageable);

        return orders.map(this::toOrderSummary);
    }

    // ── Admin — Get single order ───────────────────────────────────────────

    /**
     * Retrieve a single non-deleted order by ID with all details.
     *
     * @param id the order ID
     * @return full order response
     * @throws OrderNotFoundException if the order does not exist or is deleted
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toOrderResponse(order);
    }

    // ── Admin — Update status ──────────────────────────────────────────────

    /**
     * Update an order's payment status and/or received amount.
     * Only valid for ACCEPTED orders.
     *
     * @param id  the order ID
     * @param req status + received amount update request
     * @return updated full order response
     * @throws OrderNotFoundException if order not found
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + req.getStatus());
        }

        order.setStatus(newStatus);
        if (req.getReceivedAmount() != null) {
            order.setReceivedAmount(req.getReceivedAmount());
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} status updated to {}", id, newStatus);
        return toOrderResponse(saved);
    }

    // ── Admin — Update GST number ──────────────────────────────────────────

    /**
     * Attach, update, or clear an order's GSTIN.
     * <p>
     * Passing a non-blank GSTIN sets the order to "With GST" and also persists
     * the GSTIN onto the linked customer so future orders default to it.
     * Passing {@code null} or a blank string clears the GSTIN and marks the
     * order as "Without GST" (the customer record is left untouched).
     *
     * @param id        the order ID
     * @param gstNumber 15-char GSTIN, or {@code null}/blank to clear
     * @return updated full order response
     * @throws OrderNotFoundException if the order does not exist or is deleted
     */
    @Transactional
    public OrderResponse updateOrderGst(Long id, String gstNumber) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (gstNumber == null || gstNumber.isBlank()) {
            order.setGstNumber(null);
            order.setWithGst(false);
            log.info("Order {} GSTIN cleared (now Without GST)", id);
        } else {
            String normalized = normalizeGstNumber(gstNumber, true);
            order.setGstNumber(normalized);
            order.setWithGst(true);
            // Mirror onto the customer so subsequent orders default to it
            Customer c = order.getCustomer();
            if (c != null) {
                c.setGstNumber(normalized);
                customerRepository.save(c);
            }
            log.info("Order {} GSTIN set to {} (now With GST)", id, normalized);
        }

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    // ── Admin — Soft delete ────────────────────────────────────────────────

    /**
     * Soft-delete an order by setting its {@code deletedAt} timestamp.
     *
     * @param id the order ID
     * @throws OrderNotFoundException if order not found
     */
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findActiveById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setDeletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order {} soft-deleted", id);
    }

    // ── Admin — Dashboard ──────────────────────────────────────────────────

    /**
     * Aggregate dashboard statistics across all non-deleted accepted orders.
     *
     * @return DashboardStatsResponse with totals and this-month metrics
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime monthStart = YearMonth.now()
                .atDay(1)
                .atStartOfDay();

        return DashboardStatsResponse.builder()
                .totalOrders(orderRepository.countActive())
                .totalRevenue(orZero(orderRepository.sumGrandTotal()))
                .totalReceived(orZero(orderRepository.sumReceivedAmount()))
                .pendingCount(orderRepository.countByStatus(OrderStatus.PENDING))
                .overdueCount(orderRepository.countByStatus(OrderStatus.OVERDUE))
                .thisMonthOrders(orderRepository.countActiveCreatedAfter(monthStart))
                .thisMonthRevenue(orZero(orderRepository.sumGrandTotalCreatedAfter(monthStart)))
                .pendingReviewCount(orderRepository.countPendingReview())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Upsert a customer by mobile number.
     * If a customer with the given mobile already exists, update their name/address.
     *
     * @param name          customer name
     * @param mobile        unique mobile number
     * @param address       delivery address (nullable)
     * @param placeOfSupply supply state (defaults to Karnataka)
     * @return the persisted customer entity
     */
    private Customer upsertCustomer(String name, String mobile, String address, String placeOfSupply, String gstNumber) {
        return customerRepository.findByMobile(mobile)
                .map(existing -> {
                    existing.setName(name);
                    if (address != null) existing.setAddress(address);
                    if (placeOfSupply != null) existing.setPlaceOfSupply(placeOfSupply);
                    if (gstNumber != null) existing.setGstNumber(gstNumber);
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .name(name)
                        .mobile(mobile)
                        .address(address)
                        .placeOfSupply(placeOfSupply != null ? placeOfSupply : "Karnataka")
                        .gstNumber(gstNumber)
                        .build()));
    }

    /**
     * Generate the next sequential invoice number using a pessimistic DB lock.
     *
     * @return invoice number as a string (e.g. "183")
     */
    private String nextInvoiceNumber() {
        InvoiceCounter counter = invoiceCounterRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("Invoice counter not initialised"));
        counter.setLastValue(counter.getLastValue() + 1);
        invoiceCounterRepository.save(counter);
        log.debug("Next invoice number: {}", counter.getLastValue());
        return String.valueOf(counter.getLastValue());
    }

    /**
     * Sum taxable amounts, cgst, sgst, and grandTotal across all items on the order.
     *
     * @param order the order whose items have already been priced
     */
    private void aggregateOrderTotals(Order order) {
        BigDecimal taxableAmount = order.getItems().stream()
                .map(OrderItem::getTaxableSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cgst = pricingService.calculateCgst(taxableAmount);
        BigDecimal sgst = pricingService.calculateSgst(taxableAmount);
        BigDecimal grandTotal = taxableAmount.add(cgst).add(sgst)
                .setScale(2, RoundingMode.HALF_UP);

        order.setTaxableAmount(taxableAmount);
        order.setCgst(cgst);
        order.setSgst(sgst);
        order.setGrandTotal(grandTotal);
    }

    /**
     * Automatically transition order payment status based on received amount.
     *
     * @param order the order (must have grandTotal and receivedAmount set)
     */
    private void autoSetStatus(Order order) {
        if (order.getReceivedAmount().compareTo(order.getGrandTotal()) >= 0) {
            order.setStatus(OrderStatus.PAID);
        } else if (order.getDueDate() != null
                && LocalDate.now().isAfter(order.getDueDate())
                && order.getReceivedAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.OVERDUE);
        }
    }

    /**
     * Map an Order entity to a full OrderResponse DTO.
     *
     * @param order the order entity
     * @return full OrderResponse
     */
    private OrderResponse toOrderResponse(Order order) {
        BigDecimal balanceDue = order.getGrandTotal()
                .subtract(order.getReceivedAmount())
                .max(BigDecimal.ZERO);

        List<OrderResponse.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemDto.builder()
                        .id(item.getId())
                        .bottleType(item.getBottleType().name())
                        .flavor(item.getFlavor())
                        .pricePerBottle(item.getPricePerBottle())
                        .quantity(item.getQuantity())
                        .bottlesPerUnit(item.getBottlesPerUnit())
                        .ratePerUnit(item.getRatePerUnit())
                        .taxPerUnit(item.getTaxPerUnit())
                        .totalPerUnit(item.getTotalPerUnit())
                        .taxableSubtotal(item.getTaxableSubtotal())
                        .taxSubtotal(item.getTaxSubtotal())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        Customer c = order.getCustomer();
        return OrderResponse.builder()
                .id(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .invoiceDate(order.getInvoiceDate())
                .dueDate(order.getDueDate())
                .status(order.getStatus().name())
                .source(order.getSource().name())
                .reviewStatus(order.getReviewStatus().name())
                .rejectionReason(order.getRejectionReason())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .customer(OrderResponse.CustomerDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .mobile(c.getMobile())
                        .address(c.getAddress())
                        .placeOfSupply(c.getPlaceOfSupply())
                        .gstNumber(c.getGstNumber())
                        .customerShopName(order.getCustomerShopName())
                        .build())
                .items(itemDtos)
                .taxableAmount(order.getTaxableAmount())
                .cgst(order.getCgst())
                .sgst(order.getSgst())
                .grandTotal(order.getGrandTotal())
                .receivedAmount(order.getReceivedAmount())
                .balanceDue(balanceDue)
                .withGst(Boolean.TRUE.equals(order.getWithGst()))
                .gstNumber(order.getGstNumber())
                .build();
    }

    /**
     * Map an Order entity to a compact summary DTO for list views.
     *
     * @param order the order entity
     * @return compact OrderSummaryResponse
     */
    private OrderSummaryResponse toOrderSummary(Order order) {
        BigDecimal balanceDue = order.getGrandTotal()
                .subtract(order.getReceivedAmount())
                .max(BigDecimal.ZERO);

        // Safely get item count — avoids LazyInitializationException when items are not fetched
        int itemCount = 0;
        try {
            itemCount = order.getItems().size();
        } catch (Exception ignored) {
            // items not loaded in this query context — count stays 0
        }

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .invoiceNo(order.getInvoiceNo())
                .customerName(order.getCustomer().getName())
                .customerShopName(order.getCustomerShopName())
                .mobile(order.getCustomer().getMobile())
                .invoiceDate(order.getInvoiceDate())
                .grandTotal(order.getGrandTotal())
                .receivedAmount(order.getReceivedAmount())
                .balanceDue(balanceDue)
                .status(order.getStatus().name())
                .withGst(Boolean.TRUE.equals(order.getWithGst()))
                .gstNumber(order.getGstNumber())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt())
                .reviewStatus(order.getReviewStatus().name())
                .rejectionReason(order.getRejectionReason())
                .build();
    }

    private String normalizeGstNumber(String gstNumber, boolean withGst) {
        if (!withGst) {
            return null;
        }

        if (gstNumber == null || gstNumber.isBlank()) {
            throw new IllegalArgumentException("GST number is required when GST is enabled");
        }

        String normalized = gstNumber.trim().toUpperCase();
        if (!normalized.matches("^[0-9A-Z]{15}$")) {
            throw new IllegalArgumentException("GST number must be a valid 15-character GSTIN");
        }
        return normalized;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
