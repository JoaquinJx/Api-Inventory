package com.joaquin.inventory.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String code;
    private String message;
    private String traceId;
    private String path;
    private Map<String, String> fieldErrors;
}