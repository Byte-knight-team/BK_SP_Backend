package com.ByteKnights.com.resturarent_system.export.provider;

import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BranchExportProvider implements ExportDataProvider {

    private final BranchRepository branchRepository;

    @Override
    public ExportTarget getTarget() {
        return ExportTarget.BRANCHES;
    }

    @Override
    public String getBaseFileName() {
        return "branches";
    }

    @Override
    public Set<ExportFormat> getSupportedFormats() {
        return EnumSet.of(ExportFormat.CSV, ExportFormat.JSON);
    }

    @Override
    public List<LinkedHashMap<String, Object>> getCsvRows() {
        return branchRepository.findAll(Sort.by(Sort.Order.asc("id"))).stream()
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    @Override
    public Object getJsonData() {
        return branchRepository.findAll(Sort.by(Sort.Order.asc("id")));
    }

    private LinkedHashMap<String, Object> toRow(Branch branch) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();

        row.put("id", branch.getId());
        row.put("name", branch.getName());
        row.put("address", branch.getAddress());
        row.put("contactNumber", branch.getContactNumber());
        row.put("email", branch.getEmail());
        row.put("status", branch.getStatus() != null ? branch.getStatus().name() : null);
        row.put("createdAt", branch.getCreatedAt());

        return row;
    }
}