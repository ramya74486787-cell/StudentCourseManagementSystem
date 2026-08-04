package com.equipment.loan.dto.response;

import com.equipment.loan.entity.LoanRequest;
import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class LoanRequestResponse {
    private Long id;
    private Long requestedById;
    private String requestedByName;
    private EquipmentResponse equipment;
    private Integer quantity;
    private String purpose;
    private LocalDateTime requestDate;
    private LocalDateTime dueDate;
    private LoanStatus status;
    private LoanStage currentStage;
    private String rejectionReason;
    private List<ApprovalStageResponse> history;

    public static LoanRequestResponse from(LoanRequest lr) {
        return new LoanRequestResponse(
                lr.getId(),
                lr.getRequestedBy().getId(),
                lr.getRequestedBy().getName(),
                EquipmentResponse.from(lr.getEquipment()),
                lr.getQuantity(),
                lr.getPurpose(),
                lr.getRequestDate(),
                lr.getDueDate(),
                lr.getStatus(),
                lr.getCurrentStage(),
                lr.getRejectionReason(),
                lr.getApprovalStages().stream().map(ApprovalStageResponse::from).collect(Collectors.toList())
        );
    }
}
