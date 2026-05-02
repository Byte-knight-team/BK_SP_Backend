package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import com.ByteKnights.com.resturarent_system.service.ReceptionistTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
