package com.equipment.loan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "category is required")
    private String category;

    @Positive(message = "totalUnits must be greater than 0")
    private Integer totalUnits;

    private String condition;
    @NotBlank(message="location is required")
    private String location;
}
