package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.GrnLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GrnLineItemRepository extends JpaRepository<GrnLineItem, Long> {

    // Find all line items for a specific GRN
    List<GrnLineItem> findByGoodsReceiptNoteId(Long goodsReceiptNoteId);

    // Sum total received quantity for a specific PO line item across all GRNs
    // Used to determine if a PO line item has been fully received
    @Query("SELECT COALESCE(SUM(g.receivedQuantity), 0) FROM GrnLineItem g " +
           "WHERE g.purchaseOrderItem.id = :poItemId " +
           "AND g.condition = com.ByteKnights.com.resturarent_system.entity.GrnItemCondition.GOOD")
    BigDecimal sumReceivedQuantityByPoItemId(@Param("poItemId") Long poItemId);
}
