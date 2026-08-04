package com.equipment.loan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardResponse {

    private long totalEquipment;
    private long availableEquipment;
    private long totalLoanRequests;
    private long pendingRequests;
    private long approvedRequests;
    private long rejectedRequests;
    private long returnedEquipment;

    private List<MostRequestedItem> mostRequestedItems;

    @Getter
    @AllArgsConstructor
    public static class MostRequestedItem {
        private Long equipmentId;
        private String equipmentName;
        private long requestCount;
    }
}