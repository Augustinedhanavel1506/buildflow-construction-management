package com.buildflow.team.controller;

import com.buildflow.common.dto.ApiResponse;
import com.buildflow.team.dto.EngineerRequest;
import com.buildflow.team.dto.EngineerResponse;
import com.buildflow.team.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/api/team/engineers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<List<EngineerResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(teamService.listEngineers()));
    }

    @PostMapping("/api/team/engineers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EngineerResponse>> create(@Valid @RequestBody EngineerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Engineer account created.", teamService.createEngineer(request)));
    }

    @PutMapping("/api/team/engineers/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EngineerResponse>> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Engineer updated.", teamService.setActive(id, active)));
    }
}
