package com.example.inventory.stock.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class InboundRequest {

    @NotBlank
    private String sku;
    private String name;
    @Positive
    private int quantity;
}
