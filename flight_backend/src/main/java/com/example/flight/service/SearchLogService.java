package com.example.flight.service;

import com.example.flight.dto.SearchLogRequestDTO;
import com.example.flight.dto.SearchLogResponseDTO;
import com.example.flight.entity.SearchLog;
import com.example.flight.entity.User;
import com.example.flight.repository.SearchLogRepository;
import com.example.flight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    // Get all search logs
    public List<SearchLogResponseDTO> getAllSearchLogs() {

        return searchLogRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get search log by ID
    public SearchLogResponseDTO getSearchLogById(
            Long searchId) {

        SearchLog searchLog =
                searchLogRepository.findById(searchId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Search log not found with ID: "
                                                + searchId
                                ));

        return convertToResponse(searchLog);
    }

    // Get search logs by user
    public List<SearchLogResponseDTO> getSearchLogsByUser(
            Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException(
                    "User not found with ID: " + userId
            );
        }

        return searchLogRepository
                .findByUserUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Search by source and destination
    public List<SearchLogResponseDTO> getSearchLogsByRoute(
            String source,
            String destination) {

        return searchLogRepository
                .findBySourceAndDestination(
                        source,
                        destination
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Create search log
    public SearchLogResponseDTO createSearchLog(
            SearchLogRequestDTO dto) {

        SearchLog searchLog =
                modelMapper.map(
                        dto,
                        SearchLog.class
                );

        // User is optional
        if (dto.getUserId() != null) {

            User user =
                    userRepository.findById(
                            dto.getUserId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "User not found with ID: "
                                            + dto.getUserId()
                            ));

            searchLog.setUser(user);
        }

        searchLog.setSearchDate(
                LocalDateTime.now()
        );

        SearchLog savedSearchLog =
                searchLogRepository.save(searchLog);

        return convertToResponse(savedSearchLog);
    }

    // Update search log
    public SearchLogResponseDTO updateSearchLog(
            Long searchId,
            SearchLogRequestDTO dto) {

        SearchLog searchLog =
                searchLogRepository.findById(searchId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Search log not found with ID: "
                                                + searchId
                                ));

        modelMapper.map(dto, searchLog);

        // User is optional
        if (dto.getUserId() != null) {

            User user =
                    userRepository.findById(
                            dto.getUserId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "User not found with ID: "
                                            + dto.getUserId()
                            ));

            searchLog.setUser(user);

        } else {

            searchLog.setUser(null);
        }

        SearchLog updatedSearchLog =
                searchLogRepository.save(searchLog);

        return convertToResponse(updatedSearchLog);
    }

    // Delete search log
    public void deleteSearchLog(Long searchId) {

        SearchLog searchLog =
                searchLogRepository.findById(searchId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Search log not found with ID: "
                                                + searchId
                                ));

        searchLogRepository.delete(searchLog);
    }

    // Entity -> Response DTO
    private SearchLogResponseDTO convertToResponse(
            SearchLog searchLog) {

        SearchLogResponseDTO response =
                modelMapper.map(
                        searchLog,
                        SearchLogResponseDTO.class
                );

        if (searchLog.getUser() != null) {

            response.setUserId(
                    searchLog.getUser().getUserId()
            );
        }

        return response;
    }
}