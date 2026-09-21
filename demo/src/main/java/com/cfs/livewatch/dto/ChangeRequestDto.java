package com.cfs.livewatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequestDto {
    private String id;
    private String requesterId;
    private String requesterName;
    private String kind;
    private String videoId;
    private Double time;
    private long createdAt;
}

