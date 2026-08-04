package com.equipment.loan.entity;

import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private Integer quantity;

    private String purpose;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    // Pointer: "where is this request right now / who should act on it next".
    // This gets overwritten as the request moves through the workflow.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStage currentStage;

    private String rejectionReason;

    // Log: append-only history of every decision made on this request.
    // Rows here are never edited once written.
    @Builder.Default
    @OneToMany(mappedBy = "loanRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decisionDate ASC")
    private List<ApprovalStage> approvalStages = new ArrayList<>();
}
