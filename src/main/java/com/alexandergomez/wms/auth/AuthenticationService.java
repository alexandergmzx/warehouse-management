package com.alexandergomez.wms.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.identity.AppUser;
import com.alexandergomez.wms.identity.AppUserRepository;
import com.alexandergomez.wms.identity.Device;
import com.alexandergomez.wms.identity.DeviceRepository;
import com.alexandergomez.wms.identity.IssuedToken;
import com.alexandergomez.wms.identity.TokenService;
import com.alexandergomez.wms.picking.PickingTaskRepository;
import com.alexandergomez.wms.picking.TaskStatus;

/**
 * Authenticates an operator on a known device and issues a token. Enforces the
 * login contract error order (API.md): credentials, then user state, then
 * device registration/state, then the device-assignment conflict. Passwords,
 * hashes, and tokens are never logged.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final List<TaskStatus> ACTIVE_TASK_STATES =
            List.of(TaskStatus.ASSIGNED, TaskStatus.LOCATION_CONFIRMED, TaskStatus.ARTICLE_CONFIRMED);

    private final AppUserRepository users;
    private final DeviceRepository devices;
    private final PickingTaskRepository pickingTasks;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthenticationService(AppUserRepository users, DeviceRepository devices,
            PickingTaskRepository pickingTasks, PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.users = users;
        this.devices = devices;
        this.pickingTasks = pickingTasks;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByUsername(request.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw reject(request, ProblemCode.INVALID_CREDENTIALS, "Invalid username or password.");
        }
        if (!user.isActive()) {
            throw reject(request, ProblemCode.USER_INACTIVE, "The user account is inactive.");
        }
        Device device = devices.findByDeviceCode(request.deviceCode()).orElse(null);
        if (device == null) {
            throw reject(request, ProblemCode.DEVICE_NOT_REGISTERED, "The device is not registered.");
        }
        if (!device.isActive()) {
            throw reject(request, ProblemCode.DEVICE_INACTIVE, "The device is inactive.");
        }
        if (pickingTasks.existsByAssignedDeviceIdAndStatusInAndAssignedUserIdNot(
                device.getId(), ACTIVE_TASK_STATES, user.getId())) {
            throw reject(request, ProblemCode.DEVICE_ASSIGNMENT_CONFLICT,
                    "The device currently holds an active task for another user.");
        }
        IssuedToken issued = tokenService.issue(user, device);
        log.info("login succeeded username={} deviceCode={}", user.getUsername(), device.getDeviceCode());
        return LoginResponse.of(issued, user, device);
    }

    @Transactional
    public void logout(String rawToken) {
        tokenService.revoke(rawToken);
    }

    private ProblemException reject(LoginRequest request, ProblemCode code, String detail) {
        log.warn("login rejected username={} deviceCode={} code={}",
                request.username(), request.deviceCode(), code.code());
        return new ProblemException(code, detail);
    }
}
