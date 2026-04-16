package com.byteknights.com.resturarent_system.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {
    private String reason;
}
