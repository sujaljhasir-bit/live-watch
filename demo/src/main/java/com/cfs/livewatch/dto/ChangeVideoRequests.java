package com.cfs.livewatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeVideoRequests {

    @NotBlank(message = "participantId is required")
    private String participantId;

    @NotBlank(message = "videoUrl is required")
    private String videoUrl;
}
