package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.TableActionRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist/tables")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistTableController {

    private final ReceptionistTableService receptionistTableService;

    // get all tables details for the receptionist's branch
    @GetMapping
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getBranchTables(Principal principal) {
        List<ReceptionistTableResponse> tables = receptionistTableService.getBranchTables(principal.getName());
        return new ResponseEntity<>(
                new StandardResponse(200, "Success", tables),
                HttpStatus.OK
        );
    }

    // mark a table as occupied when guests are seated
    @PutMapping("/{id}/occupy")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_UPDATE')")
    public ResponseEntity<StandardResponse> occupyTable(
            @PathVariable Long id,
            @Valid @RequestBody TableActionRequest request,
            Principal principal) {

        receptionistTableService.occupyTable(id, request.getGuestCount(), principal.getName());

        return new ResponseEntity<>(
                new StandardResponse(200, "Table #" + id + " is now occupied", null),
                HttpStatus.OK
        );
    }

    // clear a table when guests leave
    @PutMapping("/{id}/clear")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_UPDATE')")
    public ResponseEntity<StandardResponse> clearTable(
            @PathVariable Long id,
            Principal principal) {

        receptionistTableService.clearTable(id, principal.getName());

        return new ResponseEntity<>(
                new StandardResponse(200, "Table #" + id + " is now available", null),
                HttpStatus.OK
        );
    }


}
