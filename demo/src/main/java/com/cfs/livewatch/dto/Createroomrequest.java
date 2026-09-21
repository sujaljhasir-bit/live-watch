package com.cfs.livewatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Createroomrequest {
    @NotBlank(message = "username is required")
    @Size(max = 24, message = "username must be at most 24 characters")
    private String username;


}
