package com.equipment.loan.repository;

import com.equipment.loan.entity.LoanRequest;
import com.equipment.loan.entity.User;
import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long>, JpaSpecificationExecutor<LoanRequest> {

    Page<LoanRequest> findByRequestedBy(User requestedBy, Pageable pageable);

    Page<LoanRequest> findByCurrentStage(LoanStage currentStage, Pageable pageable);

    @Query("select count(l) from LoanRequest l where l.status = :status and l.currentStage = :stage")
    long countByStatusAndStage(@Param("status") LoanStatus status, @Param("stage") LoanStage stage);
    long countByStatus(LoanStatus status);
}
