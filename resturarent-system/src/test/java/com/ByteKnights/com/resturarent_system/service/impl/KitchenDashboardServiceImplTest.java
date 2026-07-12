package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.KitchenDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class KitchenDashboardServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private StaffRepository staffRepository;

        @Mock
        private OrderRepository orderRepository;

        @InjectMocks
        private KitchenDashboardServiceImpl kitchenDashboardService;

        @BeforeMethod
        public void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        @Test
        public void testGetKitchenDashboardStats_Success() {

                User fakeUser = new User();
                fakeUser.setEmail("chef@test.com");

                Branch fakeBranch = new Branch();
                fakeBranch.setId(1L);

                Staff fakeStaff = new Staff();
                fakeStaff.setBranch(fakeBranch);

                when(userRepository.findByEmail("chef@test.com")).thenReturn(Optional.of(fakeUser));
                when(staffRepository.findByUser(fakeUser)).thenReturn(Optional.of(fakeStaff));

                when(orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(eq(1L), eq(OrderStatus.PENDING), any()))
                                .thenReturn(15L);
                when(orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(eq(1L), eq(OrderStatus.PREPARING),
                                any()))
                                .thenReturn(5L);
                // Completed count now covers COMPLETED + SERVED via the status-in query
                when(orderRepository.countByBranchIdAndStatusInAndCreatedAtAfter(eq(1L), any(), any()))
                                .thenReturn(20L);

                when(orderRepository.getAveragePreparationTimeTodayByBranch(eq(1L), any()))
                                .thenReturn(12.5);

                KitchenDashboardStatsDTO result = kitchenDashboardService.getKitchenDashboardStats("chef@test.com");

                Assert.assertEquals(result.getPendingOrders(), 15L);
                Assert.assertEquals(result.getPreparingOrders(), 5L);
                Assert.assertEquals(result.getCompletedOrders(), 20L);
                Assert.assertEquals(result.getAveragePrepTimeInMinutes(), 12.5);
        }
}
