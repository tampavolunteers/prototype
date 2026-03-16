package org.tampavolunteers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.AdminAuditLog;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.AdminAuditLogRepository;
import org.tampavolunteers.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logAction(Long adminId, String action, String targetType,
                          Long targetId, String details, String ipAddress) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUser(admin);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);

        auditLogRepository.save(log);
    }
}
