package com.equipment.loan.controller;

import com.equipment.loan.dto.request.ApprovalDecisionRequest;
import com.equipment.loan.dto.request.LoanRequestCreateRequest;
import com.equipment.loan.dto.response.LoanRequestResponse;
import com.equipment.loan.dto.response.PageResponse;
import com.equipment.loan.entity.User;
import com.equipment.loan.enums.LoanStatus;
import com.equipment.loan.security.CurrentUserProvider;
import com.equipment.loan.service.LoanRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController 
{

    private final LoanRequestService loanRequestService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<LoanRequestResponse> create(@Valid @RequestBody LoanRequestCreateRequest request) 
    {
        User requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(loanRequestService.create(request, requester));
    }
    @GetMapping("/my")
    public ResponseEntity<PageResponse<LoanRequestResponse>> myRequests(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) 
    {
        User requester = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(PageResponse.from(loanRequestService.getMyRequests(requester, pageable)));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR', 'STORE_KEEPER')")
    @GetMapping("/pending-my-action")
    public ResponseEntity<PageResponse<LoanRequestResponse>> pendingForMe
    (
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(PageResponse.from(loanRequestService.getPendingForRole(user.getRole(), pageable)));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR', 'STORE_KEEPER')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanRequestResponse> approve(@PathVariable Long id,
                                                         @RequestBody(required = false) ApprovalDecisionRequest body) 
    {
        User approver = currentUserProvider.getCurrentUser();
        ApprovalDecisionRequest decision = body != null ? body : new ApprovalDecisionRequest();
        return ResponseEntity.ok(loanRequestService.approve(id, approver, decision));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR', 'STORE_KEEPER')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanRequestResponse> reject(@PathVariable Long id,
                                                        @Valid @RequestBody ApprovalDecisionRequest body) 
    {
        User approver = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(loanRequestService.reject(id, approver, body));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<LoanRequestResponse> markReturned(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(loanRequestService.markReturned(id, user));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<LoanRequestResponse>> search(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) 
    {
        return ResponseEntity.ok(PageResponse.from(loanRequestService.search(status, category, from, to, pageable)));
    }
    @GetMapping
    public Page<LoanRequestResponse> search(
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            Pageable pageable) {

        return loanRequestService.search(
                employeeName,
                equipmentName,
                status,
                category,
                from,
                to,
                pageable);
    }
}
