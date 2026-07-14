package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerPasswordUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerProfileUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerProfileResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerStatisticsResponse;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ProfilePicturePresignRequest;
import com.ByteKnights.com.resturarent_system.dto.request.customer.ProfilePictureUpdateRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ProfilePicturePresignResponse;

public interface CustomerProfileService {
    CustomerProfileResponse getCustomerProfile(String email);
    CustomerProfileResponse updateCustomerProfile(String currentEmail, CustomerProfileUpdateRequest request);
    void updatePassword(String email, CustomerPasswordUpdateRequest request);

    ProfilePicturePresignResponse createProfilePictureUploadUrl(String email, ProfilePicturePresignRequest request);
    void updateProfilePicture(String email, ProfilePictureUpdateRequest request);
    void removeProfilePicture(String email);

    CustomerStatisticsResponse getCustomerStatistics(String email);

    void requestEmailVerification(String email);
    void verifyEmail(String token);
}