package com.recruit.candidate_service.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String error, Map<String, String> fields) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, Map.of());
    }
}
