package com.equipment.loan;

import com.equipment.loan.dto.request.ApprovalDecisionRequest;
import com.equipment.loan.entity.Equipment;
import com.equipment.loan.entity.LoanRequest;
import com.equipment.loan.entity.User;
import com.equipment.loan.enums.LoanStage;
import com.equipment.loan.enums.LoanStatus;
import com.equipment.loan.enums.Role;
import com.equipment.loan.repository.EquipmentRepository;
import com.equipment.loan.repository.LoanRequestRepository;
import com.equipment.loan.repository.UserRepository;
import com.equipment.loan.service.LoanRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the "two people request the last unit at the same time" scenario
 * from the assignment actually behaves correctly: exactly one of two
 * concurrent store-keeper approvals succeeds, the other is rejected with a
 * meaningful error (not a 500), and stock never goes negative.
 *
 * See the README for the full explanation of why @Version / optimistic
 * locking was chosen over a pessimistic lock here.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentStockUpdateTest {

    @Autowired
    private LoanRequestService loanRequestService;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private LoanRequestRepository loanRequestRepository;
    @Autowired
    private UserRepository userRepository;

    private Equipment equipment;
    private User supervisor;
    private User storeKeeper;
    private LoanRequest requestA;
    private LoanRequest requestB;

    @BeforeEach
    void setUp() {
        // Only 1 unit available - both requests want it, only one can win.
        equipment = equipmentRepository.save(Equipment.builder()
                .name("DSLR Camera")
                .category("Photography")
                .totalUnits(1)
                .availableUnits(1)
                .condition("GOOD")
                .build());

        User employee1 = userRepository.save(User.builder()
                .name("Employee One").email("emp1@test.com").password("hashed").role(Role.EMPLOYEE).build());
        User employee2 = userRepository.save(User.builder()
                .name("Employee Two").email("emp2@test.com").password("hashed").role(Role.EMPLOYEE).build());
        supervisor = userRepository.save(User.builder()
                .name("Supervisor").email("sup@test.com").password("hashed").role(Role.SUPERVISOR).build());
        storeKeeper = userRepository.save(User.builder()
                .name("Store Keeper").email("sk@test.com").password("hashed").role(Role.STORE_KEEPER).build());

        // Both requests are already sitting at STORE_KEEPER_REVIEW, ready to be
        // approved concurrently - this is the exact moment the race can happen.
        requestA = loanRequestRepository.save(LoanRequest.builder()
                .requestedBy(employee1).equipment(equipment).quantity(1).purpose("Shoot A")
                .requestDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(3))
                .status(LoanStatus.PENDING).currentStage(LoanStage.STORE_KEEPER_REVIEW)
                .build());

        requestB = loanRequestRepository.save(LoanRequest.builder()
                .requestedBy(employee2).equipment(equipment).quantity(1).purpose("Shoot B")
                .requestDate(LocalDateTime.now()).dueDate(LocalDateTime.now().plusDays(3))
                .status(LoanStatus.PENDING).currentStage(LoanStage.STORE_KEEPER_REVIEW)
                .build());
    }

    @Test
    void onlyOneOfTwoConcurrentApprovalsWinsTheLastUnit() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Exception> unexpectedErrors = new java.util.concurrent.CopyOnWriteArrayList<>();

        Runnable approveA = () -> attemptApproval(requestA.getId(), startLine, finishLine, successCount, conflictCount, unexpectedErrors);
        Runnable approveB = () -> attemptApproval(requestB.getId(), startLine, finishLine, successCount, conflictCount, unexpectedErrors);

        pool.submit(approveA);
        pool.submit(approveB);

        // Release both threads at (as close to) the same instant as possible.
        startLine.countDown();

        boolean finished = finishLine.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(finished, "Both approval attempts should complete within the timeout");
        assertTrue(unexpectedErrors.isEmpty(), "No unexpected exceptions should occur: " + unexpectedErrors);

        // Exactly one request should have won the last unit.
        assertEquals(1, successCount.get(), "Exactly one of the two concurrent approvals should succeed");
        assertEquals(1, conflictCount.get(), "Exactly one of the two concurrent approvals should be rejected as a conflict");

        // Stock must never go negative, and must reflect exactly one unit taken.
        Equipment finalState = equipmentRepository.findById(equipment.getId()).orElseThrow();
        assertEquals(0, finalState.getAvailableUnits(), "The single available unit should have been claimed exactly once");
        assertTrue(finalState.getAvailableUnits() >= 0, "Stock must never go negative");
    }

    private void attemptApproval(Long requestId, CountDownLatch startLine, CountDownLatch finishLine,
                                  AtomicInteger successCount, AtomicInteger conflictCount, List<Exception> unexpectedErrors) {
        try {
            startLine.await();
            loanRequestService.approve(requestId, storeKeeper, new ApprovalDecisionRequest());
            successCount.incrementAndGet();
        } catch (com.equipment.loan.exception.StockConflictException | com.equipment.loan.exception.InsufficientStockException e) {
            // This is the expected, meaningful outcome for the losing thread.
            conflictCount.incrementAndGet();
        } catch (Exception e) {
            unexpectedErrors.add(e);
        } finally {
            finishLine.countDown();
        }
    }
}
