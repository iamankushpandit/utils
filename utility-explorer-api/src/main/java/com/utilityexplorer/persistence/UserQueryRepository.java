package com.utilityexplorer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserQueryRepository extends JpaRepository<UserQuery, Long> {
    List<UserQuery> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
