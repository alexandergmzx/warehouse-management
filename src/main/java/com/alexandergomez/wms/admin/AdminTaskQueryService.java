package com.alexandergomez.wms.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.configuration.WmsProperties;

@Service
public class AdminTaskQueryService {

    private final AdminTaskJdbcRepository adminTasks;
    private final WmsProperties properties;

    public AdminTaskQueryService(AdminTaskJdbcRepository adminTasks, WmsProperties properties) {
        this.adminTasks = adminTasks;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<AdminTaskSummary> listTasks(
            List<String> states, String orderNumber, String assignedUsername, boolean stuckOnly) {
        return adminTasks.listTasks(states, orderNumber, assignedUsername, stuckOnly, properties.task().stuckThreshold());
    }
}
