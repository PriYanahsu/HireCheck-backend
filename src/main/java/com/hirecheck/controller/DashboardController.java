package com.hirecheck.controller;

import com.hirecheck.exception.ApiException;
import com.hirecheck.security.UserPrincipal;
import com.hirecheck.service.StatsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final StatsService statsService;

    public DashboardController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        return statsService.getDashboardStats(principal.getId());
    }

    @GetMapping("/recent-activity")
    public List<Map<String, Object>> recentActivity(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        return statsService.getRecentActivity(principal.getId(), 10);
    }

    @GetMapping("/performance")
    public List<Map<String, Object>> performance(@AuthenticationPrincipal UserPrincipal principal) {
        requireAuth(principal);
        return statsService.getTestPerformance(principal.getId(), 5);
    }

    private void requireAuth(UserPrincipal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
