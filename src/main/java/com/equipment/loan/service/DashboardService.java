package com.equipment.loan.service;

import com.equipment.loan.dto.response.DashboardResponse;
import com.equipment.loan.entity.Equipment;
import com.equipment.loan.entity.LoanRequest;
import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import com.equipment.loan.repository.EquipmentRepository;
import com.equipment.loan.repository.LoanRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final LoanRequestRepository loanRequestRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {

        List<Equipment> allEquipment = equipmentRepository.findAll();

        // Equipment Counts
        long totalEquipment = allEquipment.stream()
                .mapToLong(Equipment::getTotalUnits)
                .sum();

        long availableEquipment = allEquipment.stream()
                .mapToLong(Equipment::getAvailableUnits)
                .sum();

        // Loan Request Counts
        long totalLoanRequests = loanRequestRepository.count();

        long pendingRequests = loanRequestRepository.countByStatus(LoanStatus.PENDING);

        long approvedRequests = loanRequestRepository.countByStatus(LoanStatus.APPROVED);

        long rejectedRequests = loanRequestRepository.countByStatus(LoanStatus.REJECTED);

        long returnedEquipment = loanRequestRepository.countByStatus(LoanStatus.RETURNED);

        // Most Requested Equipment
        List<LoanRequest> allRequests = loanRequestRepository.findAll();

        Map<Equipment, Long> countByEquipment = allRequests.stream()
                .collect(Collectors.groupingBy(
                        LoanRequest::getEquipment,
                        Collectors.counting()));

        List<DashboardResponse.MostRequestedItem> mostRequested = countByEquipment.entrySet().stream()
                .sorted(Map.Entry.<Equipment, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new DashboardResponse.MostRequestedItem(
                        e.getKey().getId(),
                        e.getKey().getName(),
                        e.getValue()))
                .collect(Collectors.toList());

        return new DashboardResponse(
                totalEquipment,
                availableEquipment,
                totalLoanRequests,
                pendingRequests,
                approvedRequests,
                rejectedRequests,
                returnedEquipment,
                mostRequested
        );
    }
}
