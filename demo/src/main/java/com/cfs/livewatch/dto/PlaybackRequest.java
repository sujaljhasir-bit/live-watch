package com.cfs.livewatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackRequest {

    @NotBlank(message = "participantId is required")
    private String participantId;

    @PositiveOrZero(message = "time cannot be negative")
    private Double time;   // optional for play/pause; the service requires it for seek
}