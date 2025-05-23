package com.backend.softtrainer.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenResponse(@JsonProperty("access_jwt_token") String accessJwtToken,
                                   @JsonProperty("refresh_jwt_token") String refreshJwtToken,
                                   boolean success,
                                   @JsonProperty("error_message") String errorMessage) {
};
