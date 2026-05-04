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

public class KitchenServiceImplTest {

    // MOCK (FAKE) THE REPOSITORIES
    // We don't want to use the real database, so we create fake versions.
    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private OrderRepository orderRepository;

    // INJECT THE MOCKS INTO THE REAL SERVICE
    // This tells Mockito to put our fake repositories inside the real KitchenServiceImpl
    @InjectMocks
    private KitchenServiceImpl kitchenService;

    // SET UP MOCKITO BEFORE EACH TEST RUNS
    @BeforeMethod
    public void setUp() {
        // This command turns on the @Mock annotations
        MockitoAnnotations.openMocks(this);
    }

    // WRITE THE ACTUAL TEST CASE
    @Test
    public void testGetKitchenDashboardStats_Success() {

        // ARRANGE: Prepare the fake data
        User fakeUser = new User();
        fakeUser.setEmail("chef@test.com");

        Branch fakeBranch = new Branch();
        fakeBranch.setId(1L);

        Staff fakeStaff = new Staff();
        fakeStaff.setBranch(fakeBranch);

        // Tell Mockito how to behave when the service calls the database
        when(userRepository.findByEmail("chef@test.com")).thenReturn(Optional.of(fakeUser));
        when(staffRepository.findByUser(fakeUser)).thenReturn(Optional.of(fakeStaff));

        // Fake the order counts for Pending, Preparing, and Completed
        when(orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(eq(1L), eq(OrderStatus.PENDING), any()))
                .thenReturn(15L);
        when(orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(eq(1L), eq(OrderStatus.PREPARING), any()))
                .thenReturn(5L);
        when(orderRepository.countByBranchIdAndStatusAndCreatedAtAfter(eq(1L), eq(OrderStatus.COMPLETED), any()))
                .thenReturn(20L);

        // Fake the average preparation time
        when(orderRepository.getAveragePreparationTimeTodayByBranch(eq(1L), any()))
                .thenReturn(12.5);

        // ACT: Run the real method
        KitchenDashboardStatsDTO result = kitchenService.getKitchenDashboardStats("chef@test.com");

        // ASSERT: Check if the results match what we faked
        Assert.assertEquals(result.getPendingOrders(), 15L);
        Assert.assertEquals(result.getPreparingOrders(), 5L);
        Assert.assertEquals(result.getCompletedOrders(), 20L);
        Assert.assertEquals(result.getAveragePrepTimeInMinutes(), 12.5);
    }
}
