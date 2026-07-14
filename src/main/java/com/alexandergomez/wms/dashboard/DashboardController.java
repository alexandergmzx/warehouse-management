package com.alexandergomez.wms.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alexandergomez.wms.admin.AdminTaskQueryService;
import com.alexandergomez.wms.admin.AdminTaskSummary;
import com.alexandergomez.wms.configuration.WmsProperties;

import org.springframework.stereotype.Controller;

/**
 * Live task-state dashboard (ADR 0007, admin-only via
 * {@link com.alexandergomez.wms.security.DashboardSecurityConfiguration}). The
 * page renders the current state server-side, then a small script polls
 * {@link #tasks()} to refresh the table without a full reload. Both reuse
 * {@link AdminTaskQueryService}, the same read the {@code /api/v1/admin/tasks}
 * REST endpoint uses.
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final AdminTaskQueryService taskQueryService;
    private final WmsProperties properties;

    public DashboardController(AdminTaskQueryService taskQueryService, WmsProperties properties) {
        this.taskQueryService = taskQueryService;
        this.properties = properties;
    }

    @GetMapping
    public String view(Model model) {
        model.addAttribute("tasks", taskQueryService.listTasks(null, null, null, false));
        model.addAttribute("pollIntervalMs", properties.dashboard().pollInterval().toMillis());
        return "dashboard";
    }

    @GetMapping(value = "/api/tasks")
    @ResponseBody
    public List<AdminTaskSummary> tasks() {
        return taskQueryService.listTasks(null, null, null, false);
    }
}
