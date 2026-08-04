package com.equipment.loan.dto.response;

import com.equipment.loan.entity.Equipment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentResponse {
    private Long id;
    private String name;
    private String category;
    private Integer totalUnits;
    private Integer availableUnits;
    private String condition;
    private String location;

    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getTotalUnits(),
                equipment.getAvailableUnits(),
                equipment.getCondition(),
                equipment.getLocation()
        );
    }
}
