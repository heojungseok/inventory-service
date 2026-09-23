package com.example.inventory.stock.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class InboundRequest {

    @NotBlank
    @Size(max = 50)
    private String sku;
    @Size(max = 100)
    private String name;
    @Positive
    private int quantity;
}
