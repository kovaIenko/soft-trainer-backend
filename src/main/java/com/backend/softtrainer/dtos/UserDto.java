package com.backend.softtrainer.dtos;

public record UserDto(long id,
                      String department,
                      String name,
                      String avatar,
                      double exp) {
}
