package com.equipment.loan.repository;

import com.equipment.loan.entity.ApprovalStage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStageRepository extends JpaRepository<ApprovalStage, Long> {
}
