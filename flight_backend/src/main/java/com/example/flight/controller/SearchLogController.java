package com.example.flight.controller;

import com.example.flight.dto.SearchLogRequestDTO;
import com.example.flight.dto.SearchLogResponseDTO;
import com.example.flight.service.SearchLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search-logs")
@RequiredArgsConstructor
public class SearchLogController {

    private final SearchLogService searchLogService;

    // Get all search logs
    @GetMapping
    public ResponseEntity<List<SearchLogResponseDTO>>
    getAllSearchLogs() {

        return ResponseEntity.ok(
                searchLogService.getAllSearchLogs()
        );
    }

    // Get search log by ID
    @GetMapping("/{searchId}")
    public ResponseEntity<SearchLogResponseDTO>
    getSearchLogById(
            @PathVariable Long searchId) {

        return ResponseEntity.ok(
                searchLogService.getSearchLogById(
                        searchId
                )
        );
    }

    // Get search logs by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SearchLogResponseDTO>>
    getSearchLogsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                searchLogService.getSearchLogsByUser(
                        userId
                )
        );
    }

    // Get search logs by route
    @GetMapping("/route")
    public ResponseEntity<List<SearchLogResponseDTO>>
    getSearchLogsByRoute(
            @RequestParam String source,
            @RequestParam String destination) {

        return ResponseEntity.ok(
                searchLogService.getSearchLogsByRoute(
                        source,
                        destination
                )
        );
    }

    // Create search log
    @PostMapping
    public ResponseEntity<SearchLogResponseDTO>
    createSearchLog(
            @Valid @RequestBody SearchLogRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        searchLogService.createSearchLog(dto)
                );
    }

    // Update search log
    @PutMapping("/{searchId}")
    public ResponseEntity<SearchLogResponseDTO>
    updateSearchLog(
            @PathVariable Long searchId,
            @Valid @RequestBody SearchLogRequestDTO dto) {

        return ResponseEntity.ok(
                searchLogService.updateSearchLog(
                        searchId,
                        dto
                )
        );
    }

    // Delete search log
    @DeleteMapping("/{searchId}")
    public ResponseEntity<String>
    deleteSearchLog(
            @PathVariable Long searchId) {

        searchLogService.deleteSearchLog(searchId);

        return ResponseEntity.ok(
                "Search log deleted successfully"
        );
    }
}