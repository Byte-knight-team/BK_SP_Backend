package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CheckoutCalculateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CheckoutCalculateResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.BranchDetailResponse;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.CheckoutService;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import com.ByteKnights.com.resturarent_system.dto.cache.BranchConfigCacheDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final MenuItemRepository menuItemRepository;
    private final SystemConfigService systemConfigService;
    private final SystemConfigRepository systemConfigRepository;
    private final CouponRepository couponRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public CheckoutServiceImpl(MenuItemRepository menuItemRepository,
            SystemConfigService systemConfigService,
            SystemConfigRepository systemConfigRepository,
            CouponRepository couponRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository) {
        this.menuItemRepository = menuItemRepository;
        this.systemConfigService = systemConfigService;
        this.systemConfigRepository = systemConfigRepository;
        this.couponRepository = couponRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    public CheckoutCalculateResponse calculateOrderTotals(String userIdentifier, CheckoutCalculateRequest request) {
        return buildReceiptMath(userIdentifier, request);
    }
    // THE CORE MATH ENGINE (THE HELPER)
    private CheckoutCalculateResponse buildReceiptMath(String userIdentifier, CheckoutCalculateRequest request) {

        // 1. Fetch Configs (500 Internal Server Error if these are missing, as the
        // system is broken)
        SystemConfig sysConfig = systemConfigRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "System configuration is missing"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "Branch not found"));
        
        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "This branch is currently closed and not accepting orders.");
        }

        BranchConfigCacheDto branchConfig = systemConfigService.getCachedBranchConfig(request.getBranchId());

        double orderDistanceKm = 0.0;
        if ("ONLINE_DELIVERY".equals(request.getOrderType())) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Delivery location coordinates are required.");
            }
            if (branch.getLatitude() == null || branch.getLongitude() == null) {
                throw new CustomerAuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Branch location is not configured properly.");
            }
            orderDistanceKm = com.ByteKnights.com.resturarent_system.util.DistanceUtil.calculateDistance(
                    branch.getLatitude(), branch.getLongitude(),
                    request.getLatitude(), request.getLongitude()
            );
            if (orderDistanceKm > branchConfig.getMaxDeliveryRadiusKm()) {
                throw new CustomerAuthException(HttpStatus.BAD_REQUEST, 
                        String.format("Delivery location is outside our service area. Max range is %.1f km, but you are %.1f km away.", 
                                branchConfig.getMaxDeliveryRadiusKm(), orderDistanceKm));
            }
        }

        // 2. Calculate Base Subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Cart cannot be empty");
        }

        // Batch-load menu items to prevent N+1 queries
        List<Long> requestedItemIds = request.getItems().stream()
                .map(CheckoutCalculateRequest.CartItemRequest::getMenuItemId)
                .collect(Collectors.toList());
        List<MenuItem> dbMenuItems = menuItemRepository.findAllById(requestedItemIds);
        Map<Long, MenuItem> menuItemMap = dbMenuItems.stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));

        for (CheckoutCalculateRequest.CartItemRequest item : request.getItems()) {
            MenuItem dbItem = menuItemMap.get(item.getMenuItemId());
            if (dbItem == null) {
                throw new CustomerAuthException(HttpStatus.NOT_FOUND,
                        "Menu item not found: " + item.getMenuItemId());
            }

            if (Boolean.FALSE.equals(dbItem.getIsAvailable()) || dbItem.getStatus() != MenuItemStatus.ACTIVE) {
                throw new CustomerAuthException(HttpStatus.BAD_REQUEST, dbItem.getName() + " is currently unavailable or inactive.");
            }
            if (!"ACTIVE".equals(dbItem.getCategory().getStatus())) {
                throw new CustomerAuthException(HttpStatus.BAD_REQUEST, dbItem.getName() + " belongs to an inactive category.");
            }

            BigDecimal lineTotal = dbItem.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        // 3. Process Coupon
        BigDecimal couponDiscount = BigDecimal.ZERO;
        String appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            Coupon coupon = validateAndGetCoupon(request.getCouponCode(), subtotal);
            appliedCoupon = coupon.getCode();

            if (coupon.getDiscountType() == DiscountType.FIXED) {
                couponDiscount = coupon.getDiscountValue();
            } else { // PERCENT
                couponDiscount = subtotal.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100)));
                if (coupon.getMaxDiscount() != null && couponDiscount.compareTo(coupon.getMaxDiscount()) > 0) {
                    couponDiscount = coupon.getMaxDiscount();
                }
            }
        }

        // 4. Process Loyalty Points (The 50% Rule)
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        Integer pointsUsed = 0;
        Integer availablePoints = 0;

        if (userIdentifier != null && sysConfig.isLoyaltyEnabled()) {
            Customer customer = getCustomer(userIdentifier);
            availablePoints = customer.getLoyaltyPoints();

            if (request.getRedeemLoyaltyPoints() != null && request.getRedeemLoyaltyPoints() > 0) {
                pointsUsed = request.getRedeemLoyaltyPoints();

                // Validation 1: Min points required to redeem
                if (pointsUsed < sysConfig.getMinPointsToRedeem()) {
                    throw new CustomerAuthException(HttpStatus.BAD_REQUEST,
                            "Minimum points required to redeem is " + sysConfig.getMinPointsToRedeem());
                }
                // Validation 2: Does user have enough?
                if (pointsUsed > availablePoints) {
                    throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "Insufficient loyalty points balance");
                }

                // Convert points to cash
                loyaltyDiscount = BigDecimal.valueOf(pointsUsed).multiply(sysConfig.getValuePerPoint());

                // Validation 3: 50% Max Rule
                BigDecimal subtotalAfterCoupon = subtotal.subtract(couponDiscount);
                BigDecimal maxAllowedLoyaltyDiscount = subtotalAfterCoupon.multiply(BigDecimal.valueOf(0.5));

                if (loyaltyDiscount.compareTo(maxAllowedLoyaltyDiscount) > 0) {
                    throw new CustomerAuthException(HttpStatus.BAD_REQUEST,
                            "Loyalty discount cannot exceed 50% of the order value after coupons");
                }
            }
        }

        BigDecimal totalDiscount = couponDiscount.add(loyaltyDiscount);

        // Ensure discount doesn't exceed subtotal
        if (totalDiscount.compareTo(subtotal) > 0) {
            totalDiscount = subtotal;
        }

        BigDecimal discountedSubtotal = subtotal.subtract(totalDiscount);

        // 5. Delivery Fee
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if ("ONLINE_DELIVERY".equalsIgnoreCase(request.getOrderType())) {
            BigDecimal baseFee = branchConfig.getDeliveryFee();
            BigDecimal perKmFee = branchConfig.getDeliveryFeePerKm();
            
            // Delivery Fee = Base + (Distance * PerKmRate), rounded to whole number
            BigDecimal distanceCost = perKmFee.multiply(BigDecimal.valueOf(orderDistanceKm));
            deliveryFee = baseFee.add(distanceCost).setScale(0, java.math.RoundingMode.HALF_UP);
        }

        // 6. Tax & Service Charge (Calculated on discounted subtotal)
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal serviceCharge = BigDecimal.ZERO;

        if (sysConfig.isTaxEnabled()) {
            taxAmount = discountedSubtotal.multiply(sysConfig.getTaxPercentage().divide(BigDecimal.valueOf(100)));
        }
        if (sysConfig.isServiceChargeEnabled()) {
            serviceCharge = discountedSubtotal
                    .multiply(sysConfig.getServiceChargePercentage().divide(BigDecimal.valueOf(100)));
        }

        // 7. Final Total, rounded to 2 decimal places for clean UI
        BigDecimal finalTotal = discountedSubtotal.add(deliveryFee).add(taxAmount).add(serviceCharge)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // 8. Calculate Earned Points
        Integer pointsEarned = 0;
        if (sysConfig.isLoyaltyEnabled() && sysConfig.getPointsPerAmount().compareTo(BigDecimal.ZERO) > 0) {
            // e.g., For every $x (amountPerPoint), earn y point (pointsPerAmount)
            BigDecimal multiplier = finalTotal.divide(sysConfig.getAmountPerPoint(), 0, RoundingMode.DOWN);
            pointsEarned = multiplier.multiply(sysConfig.getPointsPerAmount()).intValue();
        }

        // The max loyalty discount allowed is 50% of (subtotal − couponDiscount).
        // Convert that back to points using valuePerPoint.
        Integer maxRedeemable = 0;
        if (sysConfig.isLoyaltyEnabled()
                && availablePoints >= sysConfig.getMinPointsToRedeem()
                && sysConfig.getValuePerPoint().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal subtotalAfterCoupon = subtotal.subtract(couponDiscount);
            BigDecimal maxLoyaltyDiscount = subtotalAfterCoupon.multiply(BigDecimal.valueOf(0.5));
            int maxPointsByValue = maxLoyaltyDiscount.divide(sysConfig.getValuePerPoint(), 0, RoundingMode.DOWN)
                    .intValue();
            maxRedeemable = Math.max(0, Math.min(availablePoints, maxPointsByValue));
        }

        // 9. Fetch Branch Details for Pickup
        BranchDetailResponse branchDetails = null;
        if ("ONLINE_PICKUP".equalsIgnoreCase(request.getOrderType())) {
            branchDetails = BranchDetailResponse.builder()
                    .name(branch.getName())
                    .address(branch.getAddress())
                    .contactNumber(branch.getContactNumber())
                    .email(branch.getEmail())
                    .build();
        }

        // 10. Return the Receipt DTO
        return CheckoutCalculateResponse.builder()
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(deliveryFee)
                .serviceCharge(serviceCharge)
                .couponDiscountAmount(couponDiscount)
                .loyaltyDiscountAmount(loyaltyDiscount)
                .totalDiscountAmount(totalDiscount)
                .finalTotal(finalTotal)
                .appliedCouponCode(appliedCoupon)
                .loyaltyPointsRedeemed(pointsUsed)
                .availableLoyaltyPoints(availablePoints)
                .loyaltyPointsEarnedThisOrder(pointsEarned)
                .minPointsToRedeem(sysConfig.getMinPointsToRedeem())
                .maxRedeemablePoints(maxRedeemable)
                .branchDetails(branchDetails)
                .build();
    }

    // --- Private Validators ---
    //validate coupon
    private Coupon validateAndGetCoupon(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "Invalid coupon code"));

        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "This coupon is currently inactive");
        }
        if (coupon.getEndDate() != null && LocalDateTime.now().isAfter(coupon.getEndDate())) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "This coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST, "This coupon has reached its maximum usage limit");
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CustomerAuthException(HttpStatus.BAD_REQUEST,
                    "Your subtotal must be at least LKR " + coupon.getMinOrderAmount() + " to use this coupon");
        }

        return coupon;
    }

    private Customer getCustomer(String identifier) {
        // Dual-lookup by email or phone
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "User profile not found")));

        return customerRepository.findByUser(user)
                .orElseThrow(() -> new CustomerAuthException(HttpStatus.NOT_FOUND, "Customer details not found"));
    }
}