package com.ajing.medicalmultimodal.repo;

import com.ajing.medicalmultimodal.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByName(String name);

    boolean existsByName(String name);
}
