package com.example.inventory.stock.in;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "SKU-A001")
    @NotBlank
    @Size(max = 50)
    private String sku;
    @Schema(description = "처음 보는 sku일 때만 필수. 기존 상품이면 무시한다", example = "무선 마우스")
    @Size(max = 100)
    private String name;
    @Schema(example = "10")
    @Positive
    private int quantity;
}
