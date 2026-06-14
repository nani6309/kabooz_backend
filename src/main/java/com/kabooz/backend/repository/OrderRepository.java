package com.kabooz.backend.repository;

import com.kabooz.backend.entity.Order;
import com.kabooz.backend.entity.Order.OrderStatus;
import com.kabooz.backend.entity.Order.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repository for {@link Order} entities.
 * All queries automatically filter out soft-deleted records via {@code deleted_at IS NULL}.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ── Paginated listing with optional filters ────────────────────────────

    /**
     * List active orders with optional status, reviewStatus, and search filters.
     * When reviewStatus is non-null, only orders with that review status are returned.
     *
     * @param status       payment status filter; null means all
     * @param reviewStatus review workflow filter; null means all
     * @param search       free-text search; null means no filter
     * @param pageable     pagination/sorting parameters
     * @return page of matching orders
     */
    @Query(value = """
            SELECT o FROM Order o
            JOIN FETCH o.customer c
            WHERE o.deletedAt IS NULL
              AND (:status IS NULL OR o.status = :status)
              AND (:reviewStatus IS NULL OR o.reviewStatus = :reviewStatus)
              AND (:search IS NULL OR :search = ''
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR c.mobile LIKE CONCAT('%', :search, '%')
                    OR LOWER(o.invoiceNo) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            JOIN o.customer c
            WHERE o.deletedAt IS NULL
              AND (:status IS NULL OR o.status = :status)
              AND (:reviewStatus IS NULL OR o.reviewStatus = :reviewStatus)
              AND (:search IS NULL OR :search = ''
                    OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR c.mobile LIKE CONCAT('%', :search, '%')
                    OR LOWER(o.invoiceNo) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Order> findAllActive(
            @Param("status") OrderStatus status,
            @Param("reviewStatus") ReviewStatus reviewStatus,
            @Param("search") String search,
            Pageable pageable);

    // ── Dashboard stats ────────────────────────────────────────────────────

    /** Count all non-deleted orders that have been accepted (have an invoice). */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED'")
    long countActive();

    /** Sum grand totals of all non-deleted accepted orders. */
    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED'")
    BigDecimal sumGrandTotal();

    /** Sum received amounts of all non-deleted accepted orders. */
    @Query("SELECT COALESCE(SUM(o.receivedAmount), 0) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED'")
    BigDecimal sumReceivedAmount();

    /** Count non-deleted accepted orders by payment status. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED' AND o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    /** Count non-deleted accepted orders created after the given timestamp. */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED' AND o.createdAt >= :from")
    long countActiveCreatedAfter(@Param("from") LocalDateTime from);

    /** Sum grand totals for accepted orders created after the given timestamp. */
    @Query("""
            SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o
            WHERE o.deletedAt IS NULL AND o.reviewStatus = 'ACCEPTED' AND o.createdAt >= :from
            """)
    BigDecimal sumGrandTotalCreatedAfter(@Param("from") LocalDateTime from);

    /** Count pending-review orders (new incoming). */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deletedAt IS NULL AND o.reviewStatus = 'PENDING_REVIEW'")
    long countPendingReview();

    // ── Single order lookup (non-deleted) ─────────────────────────────────

    /**
     * Find a non-deleted order by id, eagerly loading customer and items.
     *
     * @param id the order id
     * @return optional order
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.customer
            LEFT JOIN FETCH o.items
            WHERE o.id = :id AND o.deletedAt IS NULL
            """)
    java.util.Optional<Order> findActiveById(@Param("id") Long id);
}
