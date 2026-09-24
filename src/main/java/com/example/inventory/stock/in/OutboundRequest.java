package com.example.inventory.stock.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OutboundRequest {
    @Schema(example = "SKU-A001")
    @NotBlank
    private String sku;
    @Schema(example = "3")
    @Positive
    private int quantity;
}
