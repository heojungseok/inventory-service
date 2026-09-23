package com.example.inventory.stock.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OutboundRequest {
    @NotBlank
    private String sku;
    @Positive
    private int quantity;
}
