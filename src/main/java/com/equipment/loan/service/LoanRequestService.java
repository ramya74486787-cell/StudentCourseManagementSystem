package com.equipment.loan.service;

import com.equipment.loan.dto.request.ApprovalDecisionRequest;
import com.equipment.loan.dto.request.LoanRequestCreateRequest;
import com.equipment.loan.dto.response.LoanRequestResponse;
import com.equipment.loan.entity.ApprovalStage;
import com.equipment.loan.entity.Equipment;
import com.equipment.loan.entity.LoanRequest;
import com.equipment.loan.entity.User;
import com.equipment.loan.enums.Decision;
import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import com.equipment.loan.enums.Role;
import com.equipment.loan.exception.InsufficientStockException;
import com.equipment.loan.exception.InvalidStageException;
import com.equipment.loan.exception.ResourceNotFoundException;
import com.equipment.loan.exception.StockConflictException;
import com.equipment.loan.exception.ValidationException;
import com.equipment.loan.repository.ApprovalStageRepository;
import com.equipment.loan.repository.EquipmentRepository;
import com.equipment.loan.repository.LoanRequestRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final EquipmentRepository equipmentRepository;
    private final ApprovalStageRepository approvalStageRepository;

    @Transactional
    public LoanRequestResponse create(LoanRequestCreateRequest request, User requester) 
    {
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id " + request.getEquipmentId()));

        LocalDateTime now = LocalDateTime.now();
        if (!request.getDueDate().isAfter(now)) 
        {
            throw new ValidationException("dueDate must be after the request date");
        }
        if (equipment.getAvailableUnits() < request.getQuantity()) 
        {
            throw new InsufficientStockException(
                    "Not enough stock available for " + equipment.getName() +
                            " (requested " + request.getQuantity() + ", available " + equipment.getAvailableUnits() + ")");
        }

        LoanRequest loanRequest = LoanRequest.builder()
                .requestedBy(requester)
                .equipment(equipment)
                .quantity(request.getQuantity())
                .purpose(request.getPurpose())
                .requestDate(now)
                .dueDate(request.getDueDate())
                .status(LoanStatus.PENDING)
                .currentStage(LoanStage.SUPERVISOR_REVIEW)
                .build();

        loanRequest = loanRequestRepository.save(loanRequest);
        return LoanRequestResponse.from(loanRequest);
    }

    @Transactional(readOnly = true)
    public Page<LoanRequestResponse> getMyRequests(User requester, Pageable pageable) 
    {
        return loanRequestRepository.findByRequestedBy(requester, pageable).map(LoanRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LoanRequestResponse> getPendingForRole(Role role, Pageable pageable) 
    {
        LoanStage stage = stageForRole(role);
        return loanRequestRepository.findByCurrentStage(stage, pageable).map(LoanRequestResponse::from);
    }

    @Transactional
    public LoanRequestResponse approve(Long requestId, User approver, ApprovalDecisionRequest decision) 
    {
        LoanRequest loanRequest = getRequestForUpdate(requestId);

        LoanStage stage = loanRequest.getCurrentStage();
        assertCanAct(loanRequest, approver);

        if (stage == LoanStage.SUPERVISOR_REVIEW) {
            recordStage(loanRequest, approver, stage.name(), Decision.APPROVED, decision.getComments());
            loanRequest.setCurrentStage(LoanStage.STORE_KEEPER_REVIEW);

        } else if (stage == LoanStage.STORE_KEEPER_REVIEW) 
        {
            reduceStockOrFail(loanRequest);

            recordStage(loanRequest, approver, stage.name(), Decision.APPROVED, decision.getComments());
            loanRequest.setStatus(LoanStatus.APPROVED);
            loanRequest.setCurrentStage(LoanStage.COMPLETED);

        } 
        else 
        {
            throw new InvalidStageException("This request is no longer awaiting approval (current stage: " + stage + ")");
        }

        loanRequest = loanRequestRepository.save(loanRequest);
        return LoanRequestResponse.from(loanRequest);
    }

    @Transactional
    public LoanRequestResponse reject(Long requestId, User approver, ApprovalDecisionRequest decision) 
    {
        if (decision.getComments() == null || decision.getComments().isBlank()) 
        {
            throw new ValidationException("A reason is required when rejecting a request");
        }

        LoanRequest loanRequest = getRequestForUpdate(requestId);
        LoanStage stage = loanRequest.getCurrentStage();
        assertCanAct(loanRequest, approver);

        recordStage(loanRequest, approver, stage.name(), Decision.REJECTED, decision.getComments());
        loanRequest.setStatus(LoanStatus.REJECTED);
        loanRequest.setCurrentStage(LoanStage.REJECTED);
        loanRequest.setRejectionReason(decision.getComments());

        loanRequest = loanRequestRepository.save(loanRequest);
        return LoanRequestResponse.from(loanRequest);
    }

    @Transactional
    public LoanRequestResponse markReturned(Long requestId, User user) 
    {
        LoanRequest loanRequest = getRequestForUpdate(requestId);

        if (!loanRequest.getRequestedBy().getId().equals(user.getId())) 
        {
            throw new InvalidStageException("Only the person who requested this equipment can mark it as returned");
        }
        if (loanRequest.getStatus() != LoanStatus.APPROVED) 
        {
            throw new InvalidStageException("Only an approved, currently-out request can be marked as returned (current status: " + loanRequest.getStatus() + ")");
        }

        Equipment equipment = equipmentRepository.findById(loanRequest.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));
        equipment.setAvailableUnits(Math.min(equipment.getTotalUnits(), equipment.getAvailableUnits() + loanRequest.getQuantity()));

        try {
            equipmentRepository.saveAndFlush(equipment);
        } 
        catch (OptimisticLockingFailureException e) 
        {
            throw new StockConflictException("Equipment stock was updated concurrently, please retry the return.");
        }

        loanRequest.setStatus(LoanStatus.RETURNED);
        loanRequest = loanRequestRepository.save(loanRequest);
        return LoanRequestResponse.from(loanRequest);
    }

    @Transactional(readOnly = true)
    public Page<LoanRequestResponse> search(LoanStatus status, String category, LocalDateTime from, LocalDateTime to, Pageable pageable) 
    {
        Specification<LoanRequest> spec = (root, query, cb) -> 
        {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) 
            {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null && !category.isBlank()) 
            {
                predicates.add(cb.equal(cb.lower(root.get("equipment").get("category")), category.toLowerCase()));
            }
            if (from != null) 
            {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), from));
            }
            if (to != null) 
            {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return loanRequestRepository.findAll(spec, pageable).map(LoanRequestResponse::from);
    }

    // ----
    private LoanRequest getRequestForUpdate(Long id) 
    {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan request not found with id " + id));
    }

    private void assertCanAct(LoanRequest loanRequest, User actor) 
    {
        LoanStage stage = loanRequest.getCurrentStage();

        if (stage == LoanStage.COMPLETED || stage == LoanStage.REJECTED) {
            throw new InvalidStageException("This request has already been finalized (current stage: " + stage + ")");
        }

        Role requiredRole = stage == LoanStage.SUPERVISOR_REVIEW ? Role.SUPERVISOR : Role.STORE_KEEPER;

        if (actor.getRole() != requiredRole) {
            throw new InvalidStageException(
                    "This request is waiting on a " + requiredRole + " - your role (" + actor.getRole() + ") cannot act on it");
        }
    }

    private void recordStage(LoanRequest loanRequest, User approver, String stageName, Decision decision, String comments) 
    {
        ApprovalStage stage = ApprovalStage.builder()
                .loanRequest(loanRequest)
                .stageName(stageName)
                .approver(approver)
                .decision(decision)
                .decisionDate(LocalDateTime.now())
                .comments(comments)
                .build();
        approvalStageRepository.save(stage);
        loanRequest.getApprovalStages().add(stage);
    }
    private void reduceStockOrFail(LoanRequest loanRequest) 
    {
        Equipment equipment = equipmentRepository.findById(loanRequest.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));

        if (equipment.getAvailableUnits() < loanRequest.getQuantity()) {
            throw new InsufficientStockException(
                    "Not enough stock left for " + equipment.getName() +
                            " (requested " + loanRequest.getQuantity() + ", available " + equipment.getAvailableUnits() + ")");
        }

        equipment.setAvailableUnits(equipment.getAvailableUnits() - loanRequest.getQuantity());

        try 
        {
            equipmentRepository.saveAndFlush(equipment);
        } 
        catch (OptimisticLockingFailureException e) 
        {
            throw new StockConflictException(
                    "Someone else just took the remaining stock of " + equipment.getName() + ". Please try again.");
        }
    }

    private LoanStage stageForRole(Role role) 
    {
        return switch (role) {
            case SUPERVISOR -> LoanStage.SUPERVISOR_REVIEW;
            case STORE_KEEPER -> LoanStage.STORE_KEEPER_REVIEW;
            case EMPLOYEE -> throw new InvalidStageException("Employees don't have requests waiting on them");
        };
    }
    @Transactional(readOnly = true)
    public Page<LoanRequestResponse> search(
            String employeeName,
            String equipmentName,
            LoanStatus status,
            String category,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<LoanRequest> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) 
            {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (employeeName != null && !employeeName.isBlank()) 
            {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("requestedBy").get("fullName")),
                        "%" + employeeName.toLowerCase() + "%"
                    )
                );
            }

            if (equipmentName != null && !equipmentName.isBlank()) 
            {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("equipment").get("name")),
                        "%" + equipmentName.toLowerCase() + "%"
                    )
                );
            }

            if (category != null && !category.isBlank()) 
            {
                predicates.add(
                    cb.equal(
                        cb.lower(root.get("equipment").get("category")),
                        category.toLowerCase()
                    )
                );
            }

            if (from != null) 
            {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestDate"), from));
            }

            if (to != null) 
            {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestDate"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return loanRequestRepository.findAll(spec, pageable)
                .map(LoanRequestResponse::from);
    }
}
