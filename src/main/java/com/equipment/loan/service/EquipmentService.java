package com.equipment.loan.service;

import com.equipment.loan.dto.request.EquipmentRequest;
import com.equipment.loan.dto.response.EquipmentResponse;
import com.equipment.loan.entity.Equipment;
import com.equipment.loan.exception.ResourceNotFoundException;
import com.equipment.loan.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        Equipment equipment = Equipment.builder()
                .name(request.getName())
                .category(request.getCategory())
                .totalUnits(request.getTotalUnits())
                // brand new equipment starts fully available
                .availableUnits(request.getTotalUnits())
                .condition(request.getCondition())
                .build();

        equipment = equipmentRepository.save(equipment);
        return EquipmentResponse.from(equipment);
    }

    @Transactional(readOnly = true)
    public Page<EquipmentResponse> list(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(EquipmentResponse::from);
    }

    @Transactional(readOnly = true)
    public Equipment getEntityById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id " + id));
    }
}
