package com.ajing.medicalmultimodal.repo;

import com.ajing.medicalmultimodal.domain.AppAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppAdminRepository extends JpaRepository<AppAdmin, Long> {

    Optional<AppAdmin> findByUsername(String username);
}
