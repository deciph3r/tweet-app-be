package com.ahamed.abdullah.tweetapp.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ForgetPasswordRequest {
    @NotBlank
    String resetKey;
    @NotBlank
    String password;
}
