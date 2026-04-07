package com.nguyenxuanviet.backend.service;

import com.nguyenxuanviet.backend.model.Automation;
import com.nguyenxuanviet.backend.repository.AutomationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AutomationService {

    private final AutomationRepository automationRepository;

    public AutomationService(AutomationRepository automationRepository) {
        this.automationRepository = automationRepository;
    }

    @Cacheable(value = "automations", unless = "#result == null || #result.isEmpty()")
    public List<Automation> findAll() {
        return automationRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Cacheable(value = "automations", key = "'type:' + #type", unless = "#result == null || #result.isEmpty()")
    public List<Automation> findByType(String type) {
        return automationRepository.findByTypeOrderByDisplayOrderAsc(type);
    }

    @Cacheable(value = "automations", key = "#id", unless = "#result == null || #result.isEmpty()")
    public Optional<Automation> findById(Long id) {
        return automationRepository.findById(id);
    }

    @CacheEvict(value = "automations", allEntries = true)
    public Automation create(Automation automation) {
        return automationRepository.save(automation);
    }

    @CacheEvict(value = "automations", allEntries = true)
    public Automation update(Long id, Automation automation) {
        return automationRepository.findById(id)
                .map(existing -> {
                    if (automation.getTitle() != null) existing.setTitle(automation.getTitle());
                    if (automation.getType() != null) existing.setType(automation.getType());
                    if (automation.getTaskCount() > 0) existing.setTaskCount(automation.getTaskCount());
                    existing.setActive(automation.isActive());
                    if (automation.getIconConfig() != null) existing.setIconConfig(automation.getIconConfig());
                    if (automation.getCardColor() != null) existing.setCardColor(automation.getCardColor());
                    if (automation.getCardIcon() != null) existing.setCardIcon(automation.getCardIcon());
                    if (automation.getTriggerConfig() != null) existing.setTriggerConfig(automation.getTriggerConfig());
                    if (automation.getActionConfig() != null) existing.setActionConfig(automation.getActionConfig());
                    if (automation.getDisplayOrder() != null) existing.setDisplayOrder(automation.getDisplayOrder());
                    
                    return automationRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy automation id: " + id));
    }

    @CacheEvict(value = "automations", allEntries = true)
    public void delete(Long id) {
        automationRepository.deleteById(id);
    }
}
