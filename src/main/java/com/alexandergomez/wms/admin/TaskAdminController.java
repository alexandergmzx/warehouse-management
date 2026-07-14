package com.alexandergomez.wms.admin;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alexandergomez.wms.identity.AuthenticatedUser;
import com.alexandergomez.wms.picking.BlockTaskRequest;
import com.alexandergomez.wms.picking.BlockTaskResponse;
import com.alexandergomez.wms.picking.PickingService;
import com.alexandergomez.wms.picking.ResumeTaskResponse;

import jakarta.validation.Valid;

/**
 * Administration task endpoints (API.md): dashboard-facing task listing plus
 * the block/resume recovery pair (ADR 0004). Block/resume delegate to
 * {@link PickingService}, which already owns the picking-task state machine.
 */
@RestController
@RequestMapping("/api/v1/admin/tasks")
public class TaskAdminController {

    private final AdminTaskQueryService taskQueryService;
    private final PickingService pickingService;

    public TaskAdminController(AdminTaskQueryService taskQueryService, PickingService pickingService) {
        this.taskQueryService = taskQueryService;
        this.pickingService = pickingService;
    }

    @GetMapping
    public List<AdminTaskSummary> listTasks(
            @RequestParam(required = false) List<String> state,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String assignedUsername,
            @RequestParam(defaultValue = "false") boolean stuckOnly) {
        return taskQueryService.listTasks(state, orderNumber, assignedUsername, stuckOnly);
    }

    @PostMapping("/{taskId}/block")
    public BlockTaskResponse block(@AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long taskId, @Valid @RequestBody BlockTaskRequest request) {
        return pickingService.block(admin.userId(), taskId, request.reason());
    }

    @PostMapping("/{taskId}/resume")
    public ResumeTaskResponse resume(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long taskId) {
        return pickingService.resume(admin.userId(), taskId);
    }
}
