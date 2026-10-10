package com.smartpay.platform.repository;

import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);

    @Query("""
            select u from User u
            where lower(u.name) like :search
               or lower(u.email) like :search
            """)
    Page<User> search(@Param("search") String search, Pageable pageable);
}
