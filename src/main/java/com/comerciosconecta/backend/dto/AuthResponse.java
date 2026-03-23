package com.comerciosconecta.backend.dto;

public class AuthResponse {

    private String accessToken;
    private long expiresIn;
    private String refreshToken;
    private Integer comercioId;
    private String nombre;


    public AuthResponse() {}


    public AuthResponse(String accessToken, long expiresIn, String refreshToken) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    public AuthResponse(String accessToken, long expiresIn, String refreshToken, Integer comercioId) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.comercioId = comercioId;
    }

    public AuthResponse(String accessToken, long expiresIn, String refreshToken, Integer comercioId, String nombre) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.comercioId = comercioId;
        this.nombre = nombre;
    }

    // ===== Getters y Setters =====
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Integer getComercioId() {
        return comercioId;
    }

    public void setComercioId(Integer comercioId) {
        this.comercioId = comercioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
