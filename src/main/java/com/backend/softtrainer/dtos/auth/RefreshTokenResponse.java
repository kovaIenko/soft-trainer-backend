package com.backend.softtrainer.dtos.auth;

public record RefreshTokenResponse(String access_jwt_token, String refresh_jwt_token) {};
