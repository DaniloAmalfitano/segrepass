package com.myuni.segrepass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String sessionId;
    private String username;
    private String message;
}
