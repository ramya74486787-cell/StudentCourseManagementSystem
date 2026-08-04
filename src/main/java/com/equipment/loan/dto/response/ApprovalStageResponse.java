package com.equipment.loan.dto.response;

import com.equipment.loan.entity.ApprovalStage;
import com.equipment.loan.enums.Decision;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApprovalStageResponse {
    private Long id;
    private String stageName;
    private String approverName;
    private Decision decision;
    private LocalDateTime decisionDate;
    private String comments;

    public static ApprovalStageResponse from(ApprovalStage stage) {
        return new ApprovalStageResponse(
                stage.getId(),
                stage.getStageName(),
                stage.getApprover().getName(),
                stage.getDecision(),
                stage.getDecisionDate(),
                stage.getComments()
        );
    }
}
