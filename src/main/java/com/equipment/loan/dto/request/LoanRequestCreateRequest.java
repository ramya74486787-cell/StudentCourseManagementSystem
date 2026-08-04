package com.equipment.loan.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoanRequestCreateRequest {

    @NotNull(message = "equipmentId is required")
    private Long equipmentId;

    @Positive(message = "quantity must be greater than 0")
    private Integer quantity;

    private String purpose;

    @NotNull(message = "dueDate is required")
    @Future(message = "dueDate must be after the request date")
    private LocalDateTime dueDate;
}
