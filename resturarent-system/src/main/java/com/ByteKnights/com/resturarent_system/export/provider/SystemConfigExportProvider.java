package com.ByteKnights.com.resturarent_system.export.provider;

import com.ByteKnights.com.resturarent_system.entity.SystemConfig;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SystemConfigExportProvider implements ExportDataProvider {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public ExportTarget getTarget() {
        return ExportTarget.SYSTEM_CONFIG;
    }

    @Override
    public String getBaseFileName() {
        return "system-config";
    }

    @Override
    public Set<ExportFormat> getSupportedFormats() {
        return EnumSet.of(ExportFormat.JSON);
    }

    @Override
    public List<LinkedHashMap<String, Object>> getCsvRows() {
        return Collections.emptyList();
    }

    @Override
    public Object getJsonData() {
        SystemConfig config = systemConfigRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        if (config == null) {
            return Collections.emptyMap();
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("id", config.getId());
        data.put("taxEnabled", config.isTaxEnabled());
        data.put("taxPercentage", config.getTaxPercentage());
        data.put("serviceChargeEnabled", config.isServiceChargeEnabled());
        data.put("serviceChargePercentage", config.getServiceChargePercentage());
        data.put("loyaltyEnabled", config.isLoyaltyEnabled());
        data.put("pointsPerAmount", config.getPointsPerAmount());
        data.put("amountPerPoint", config.getAmountPerPoint());
        data.put("minPointsToRedeem", config.getMinPointsToRedeem());
        data.put("valuePerPoint", config.getValuePerPoint());
        data.put("orderCancelWindowMinutes", config.getOrderCancelWindowMinutes());
        data.put("updatedAt", config.getUpdatedAt());

        return data;
    }
}