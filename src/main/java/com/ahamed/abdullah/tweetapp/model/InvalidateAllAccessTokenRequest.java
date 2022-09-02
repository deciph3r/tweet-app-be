package com.ahamed.abdullah.tweetapp.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class InvalidateAllAccessTokenRequest {
    @NotBlank
    private String password;
}
