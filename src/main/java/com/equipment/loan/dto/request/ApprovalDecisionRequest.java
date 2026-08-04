package com.equipment.loan.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalDecisionRequest {

    // Required when rejecting; optional free-text comment when approving.
    private String comments;
}
