package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequest;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemUpdateRequestRepository extends JpaRepository<MenuItemUpdateRequest, Long> {
    List<MenuItemUpdateRequest> findByStatus(MenuItemUpdateRequestStatus status);
    List<MenuItemUpdateRequest> findByChefId(Long chefId);
}
