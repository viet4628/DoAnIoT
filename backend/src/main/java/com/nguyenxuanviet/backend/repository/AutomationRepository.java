package com.nguyenxuanviet.backend.repository;

import com.nguyenxuanviet.backend.model.Automation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRepository extends JpaRepository<Automation, Long> {
    List<Automation> findByTypeOrderByDisplayOrderAsc(String type);
    List<Automation> findAllByOrderByDisplayOrderAsc();
}
