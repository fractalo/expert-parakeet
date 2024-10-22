package com.github.fractalo.streaming_settlement.security;


import lombok.Getter;

@Getter
public enum OAuth2Provider {
    GOOGLE("google");

    private final String id;

    OAuth2Provider(String id) {
        this.id = id;
    }

}
