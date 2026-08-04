package com.equipment.loan.controller;

import com.equipment.loan.dto.request.EquipmentRequest;
import com.equipment.loan.dto.response.EquipmentResponse;
import com.equipment.loan.dto.response.PageResponse;
import com.equipment.loan.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    // Store keepers own the inventory, so only they can add new equipment.
    @PreAuthorize("hasRole('STORE_KEEPER')")
    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<EquipmentResponse>> list(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(equipmentService.list(pageable)));
    }
}
