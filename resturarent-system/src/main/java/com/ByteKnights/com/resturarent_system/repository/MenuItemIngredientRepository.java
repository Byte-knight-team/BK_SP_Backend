package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItemIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {

    // Get all ingredients for a specific menu item
    List<MenuItemIngredient> findByMenuItemId(Long menuItemId);

    // Delete all ingredients for a specific menu item
    // Used when saving a new ingredient list (replace all approach)
    void deleteByMenuItemId(Long menuItemId);
}
