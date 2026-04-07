package com.comerciosconecta.backend.dto;

public class RegisterRequest {

    // Datos del usuario
    private String nombre;
    private String apellido;
    private String email;
    private String password;

    // Datos del comercio
    private String comercioNombre;
    private String nit;
    private String tipoDocumento;
    private String direccion;
    private String ciudad;
    private String telefono;
    private String categoria;

    public RegisterRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getComercioNombre() { return comercioNombre; }
    public void setComercioNombre(String comercioNombre) { this.comercioNombre = comercioNombre; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
