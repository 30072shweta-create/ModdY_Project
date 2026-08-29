package com.example.flight.repository;

import com.example.flight.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchLogRepository
        extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByUserUserId(Long userId);

    List<SearchLog> findBySourceAndDestination(
            String source,
            String destination
    );
}