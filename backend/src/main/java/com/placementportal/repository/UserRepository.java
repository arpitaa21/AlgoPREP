package com.placementportal.repository;

import com.placementportal.entity.User;
import com.placementportal.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select count(u) from User u join u.roles r where r.name = :name")
    long countByRoleName(@Param("name") RoleName name);
}
