package com.backend.softtrainer.dtos.auth;

public record LoginResponse(String message, String access_jwt_token, String refresh_jwt_token) {};
