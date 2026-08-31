package com.winterark.backend.auth.payload;

import lombok.Data;

@Data
public class AuthRequestDTO {
    private String username;
    private String email;
    private String password;
}
