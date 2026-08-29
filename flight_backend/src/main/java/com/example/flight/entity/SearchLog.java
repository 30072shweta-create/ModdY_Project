package com.example.flight.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "search_logs",
        schema = "flight_booking_system"
)
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long searchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(
            name = "source",
            length = 100,
            nullable = true
    )
    private String source;

    @Column(
            name = "destination",
            length = 100,
            nullable = true
    )
    private String destination;

    @Column(name = "search_date")
    private LocalDateTime searchDate;

    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getSearchDate() { return searchDate; }
    public void setSearchDate(LocalDateTime searchDate) { this.searchDate = searchDate; }
}