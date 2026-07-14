package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateKitchenMenuItemRequest;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemIngredientRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.request.kitchen.MenuItemUpdateRequestDto;
import com.ByteKnights.com.resturarent_system.service.KitchenMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Kitchen-owned menu management — everything under {@code /api/v1/kitchen/menu}.
 *
 * This is a separate controller from the admin/shared MenuController on
 * purpose: it exists so kitchen-side menu work (create item, view own items,
 * toggle availability, manage recipe ingredients, raise an edit request) lives
 * in its own file, the same way KitchenInventoryController is separate from
 * whatever the manager/admin side uses for inventory. Nothing here modifies
 * or depends on MenuController/MenuServiceImpl — they can keep evolving
 * independently of each other.
 *
 * Every endpoint is CHEF-only and branch-scoped: the branch/chef identity is
 * always resolved from the caller's own JWT (Principal), never trusted from
 * a request parameter.
 */
@RestController
@RequestMapping("/api/v1/kitchen/menu")
@CrossOrigin
@RequiredArgsConstructor
@PreAuthorize("hasRole('CHEF')")
public class KitchenMenuController {

    private final KitchenMenuService kitchenMenuService;

    // Create a new item — always lands in PENDING, awaiting admin review.
    @PostMapping
    public ResponseEntity<StandardResponse> createMenuItem(
            @Valid @RequestBody CreateKitchenMenuItemRequest request,
            Principal principal) {
        var created = kitchenMenuService.createMenuItem(request, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(201, "Menu item submitted for approval", created),
                HttpStatus.CREATED);
    }

    // All of the caller's own branch's menu items, any status.
    @GetMapping
    public ResponseEntity<StandardResponse> getMyMenuItems(Principal principal) {
        var items = kitchenMenuService.getMyMenuItems(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", items), HttpStatus.OK);
    }

    // A single item — 400s if it belongs to a different branch.
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<StandardResponse> getMenuItemById(@PathVariable Long id, Principal principal) {
        var item = kitchenMenuService.getMenuItemById(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", item), HttpStatus.OK);
    }

    // Flip "in stock now". Only works while the item is ACTIVE.
    @PatchMapping("/{id:\\d+}/availability")
    public ResponseEntity<StandardResponse> toggleAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload,
            Principal principal) {
        Boolean isAvailable = payload != null ? payload.get("isAvailable") : null;
        if (isAvailable == null) {
            return new ResponseEntity<>(
                    new StandardResponse(400, "isAvailable is required", null), HttpStatus.BAD_REQUEST);
        }
        var updated = kitchenMenuService.toggleAvailability(id, isAvailable, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Availability updated", updated), HttpStatus.OK);
    }

    // Recipe (ingredient list) for one of the caller's own items.
    @GetMapping("/{id:\\d+}/ingredients")
    public ResponseEntity<StandardResponse> getIngredients(@PathVariable Long id, Principal principal) {
        var ingredients = kitchenMenuService.getIngredients(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", ingredients), HttpStatus.OK);
    }

    // Replace the full ingredient list for one of the caller's own items.
    @PostMapping("/{id:\\d+}/ingredients")
    public ResponseEntity<StandardResponse> saveIngredients(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemIngredientRequestDTO request,
            Principal principal) {
        var saved = kitchenMenuService.saveIngredients(id, request, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Ingredients saved", saved), HttpStatus.OK);
    }

    // Active categories only — used to populate the create-item category dropdown.
    @GetMapping("/categories")
    public ResponseEntity<StandardResponse> getActiveCategories() {
        var categories = kitchenMenuService.getActiveCategories();
        return new ResponseEntity<>(new StandardResponse(200, "Success", categories), HttpStatus.OK);
    }

    // Distinct sub-category names already used in the caller's branch (optionally
    // narrowed to one category) — powers the "pick or type a new one" field.
    @GetMapping("/subcategories")
    public ResponseEntity<StandardResponse> getSubCategories(
            @RequestParam(required = false) Long categoryId,
            Principal principal) {
        var subCategories = kitchenMenuService.getMySubCategories(principal.getName(), categoryId);
        return new ResponseEntity<>(new StandardResponse(200, "Success", subCategories), HttpStatus.OK);
    }

    // Ask the admin to change something on an ACTIVE item. Rejected if the
    // item isn't ACTIVE, or doesn't belong to the caller's branch.
    @PostMapping("/edit-requests")
    public ResponseEntity<StandardResponse> createEditRequest(
            @RequestBody MenuItemUpdateRequestDto request,
            Principal principal) {
        kitchenMenuService.createEditRequest(request, principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(201, "Edit request submitted", null), HttpStatus.CREATED);
    }

    // The caller's own edit requests, newest first — for the My Requests page.
    @GetMapping("/edit-requests")
    public ResponseEntity<StandardResponse> getMyEditRequests(Principal principal) {
        var requests = kitchenMenuService.getMyEditRequests(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", requests), HttpStatus.OK);
    }
}
