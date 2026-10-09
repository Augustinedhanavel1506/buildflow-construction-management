package com.buildflow.labour.service;

import com.buildflow.common.exception.ResourceNotFoundException;
import com.buildflow.common.security.CurrentUserProvider;
import com.buildflow.labour.dto.AttendanceEntryResponse;
import com.buildflow.labour.dto.AttendanceSubmitRequest;
import com.buildflow.labour.dto.LabourCostResponse;
import com.buildflow.labour.entity.LabourAttendance;
import com.buildflow.labour.entity.Worker;
import com.buildflow.labour.repository.LabourAttendanceRepository;
import com.buildflow.labour.repository.WorkerRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabourAttendanceService {

    private final LabourAttendanceRepository labourAttendanceRepository;
    private final WorkerRepository workerRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserProvider currentUserProvider;

    public LabourAttendanceService(LabourAttendanceRepository labourAttendanceRepository,
                                    WorkerRepository workerRepository,
                                    ProjectRepository projectRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.labourAttendanceRepository = labourAttendanceRepository;
        this.workerRepository = workerRepository;
        this.projectRepository = projectRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<AttendanceEntryResponse> getForDate(Long projectId, LocalDate date) {
        Project project = findOwnedProject(projectId);

        List<Worker> workers = workerRepository.findByProjectIdOrderByNameAsc(project.getId());
        Map<Long, Boolean> presentByWorkerId = new LinkedHashMap<>();
        labourAttendanceRepository.findByWorkerProjectIdAndDate(project.getId(), date)
                .forEach(record -> presentByWorkerId.put(record.getWorker().getId(), record.isPresent()));

        return workers.stream()
                .filter(Worker::isActive)
                .map(worker -> AttendanceEntryResponse.of(worker, date, presentByWorkerId.getOrDefault(worker.getId(), false)))
                .toList();
    }

    @Transactional
    public List<AttendanceEntryResponse> submit(Long projectId, AttendanceSubmitRequest request) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        findOwnedProject(projectId);

        for (var entry : request.entries()) {
            Worker worker = workerRepository.findByIdAndProjectBusinessId(entry.workerId(), businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

            LabourAttendance attendance = labourAttendanceRepository
                    .findByWorkerIdAndDate(worker.getId(), request.date())
                    .orElseGet(LabourAttendance::new);
            attendance.setWorker(worker);
            attendance.setDate(request.date());
            attendance.setPresent(entry.present());
            labourAttendanceRepository.save(attendance);
        }

        return getForDate(projectId, request.date());
    }

    public LabourCostResponse getCostSummary(Long projectId) {
        Project project = findOwnedProject(projectId);

        BigDecimal totalCost = labourAttendanceRepository.sumLabourCostByProjectId(project.getId());

        List<LabourCostResponse.RoleBreakdown> byRole = labourAttendanceRepository
                .findRoleBreakdownByProjectId(project.getId()).stream()
                .map(row -> new LabourCostResponse.RoleBreakdown(
                        (String) row[0],
                        (Long) row[1],
                        (BigDecimal) row[2]))
                .toList();

        return new LabourCostResponse(totalCost, byRole);
    }

    private Project findOwnedProject(Long projectId) {
        Long businessId = currentUserProvider.getCurrentBusinessId();
        return projectRepository.findByIdAndBusinessId(projectId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
    }
}
