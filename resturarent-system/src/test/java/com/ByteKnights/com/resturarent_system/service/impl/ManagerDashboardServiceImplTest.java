package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDashboardSummaryDTO;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.EmploymentStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ManagerDashboardServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private ManagerDashboardServiceImpl managerDashboardService;

    private Staff mockStaff;
    private Branch mockBranch;

    @BeforeEach
    void setUp() {
        mockBranch = new Branch();
        mockBranch.setId(1L);
        mockBranch.setName("Main Branch");

        mockStaff = new Staff();
        mockStaff.setId(10L);
        mockStaff.setBranch(mockBranch);
    }

    @Test
    void getDashboardSummary_Success() {
        // GIVEN: Resolve branch setup
        Long userId = 100L;
        when(staffRepository.findByUserId(userId)).thenReturn(Optional.of(mockStaff));

        // Mock revenue
        when(orderRepository.sumFinalAmountByBranchIdAndPaidStatusAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("1500.50"));

        // Mock active orders count
        when(orderRepository.countByBranchIdAndStatusIn(eq(1L), any()))
                .thenReturn(5L);

        // Mock pending/out deliveries
        when(orderRepository.findByStatusAndStatusUpdatedAtAfter(any(), any(), any()))
                .thenReturn(new ArrayList<>());

        // Mock low stock
        when(inventoryItemRepository.countLowStockByBranchId(1L))
                .thenReturn(2L);

        // Mock order distribution
        when(orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(eq(1L), eq(OrderType.QR), any(), any()))
                .thenReturn(10L);
        when(orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(eq(1L), eq(OrderType.ONLINE_DELIVERY), any(), any()))
                .thenReturn(4L);
        when(orderRepository.countByBranchIdAndOrderTypeAndCreatedAtBetween(eq(1L), eq(OrderType.ONLINE_PICKUP), any(), any()))
                .thenReturn(3L);

        // Mock recent orders
        when(orderRepository.findTop50ByBranchIdOrderByCreatedAtDesc(1L))
                .thenReturn(new ArrayList<>());

        // Mock staff availability
        when(staffRepository.countByBranchIdAndUserRoleName(1L, "CHEF"))
                .thenReturn(4L);
        when(staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(1L, "CHEF", EmploymentStatus.ACTIVE))
                .thenReturn(3L);
        when(staffRepository.countByBranchIdAndUserRoleName(1L, "DELIVERY"))
                .thenReturn(5L);
        when(staffRepository.countByBranchIdAndUserRoleNameAndEmploymentStatus(1L, "DELIVERY", EmploymentStatus.ACTIVE))
                .thenReturn(4L);

        // WHEN
        ManagerDashboardSummaryDTO result = managerDashboardService.getDashboardSummary(null, userId);

        // THEN
        assertNotNull(result);
        assertEquals(new BigDecimal("1500.50"), result.getRevenue());
        assertEquals(5, result.getActiveOrders());
        assertEquals(2, result.getLowStockAlerts());
        
        // Order Distribution
        assertNotNull(result.getOrderDistribution());
        assertEquals(10, result.getOrderDistribution().getDineIn());
        assertEquals(7, result.getOrderDistribution().getOnline()); // 4 delivery + 3 pickup
        assertEquals(17, result.getOrderDistribution().getTotal()); // 10 + 7
        
        // Staff Availability
        assertNotNull(result.getStaff());
        assertEquals(4, result.getStaff().getKitchen().getTotal());
        assertEquals(3, result.getStaff().getKitchen().getActive());
        assertEquals(5, result.getStaff().getFleet().getTotal());
        assertEquals(4, result.getStaff().getFleet().getActive());
    }

    @Test
    void getDashboardSummary_ThrowsExceptionWhenUserHasNoBranch() {
        // GIVEN
        Long userId = 100L;
        Staff staffWithoutBranch = new Staff();
        staffWithoutBranch.setId(11L);
        // branch is intentionally null
        
        when(staffRepository.findByUserId(userId)).thenReturn(Optional.of(staffWithoutBranch));

        // WHEN & THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            managerDashboardService.getDashboardSummary(null, userId);
        });
        
        assertEquals("Staff member is not assigned to a branch", exception.getMessage());
    }
}
