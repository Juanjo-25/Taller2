package com.example.yate.Modelos.Form;

import jakarta.validation.constraints.*;

public class RegistroForm {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo válido")
    @Size(max = 254, message = "Máximo 254 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "Use entre 8 y 64 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{L}\\p{N}\\s])\\S+$",
             message = "Incluya mayúscula, minúscula, número y símbolo, sin espacios")
    private String contrasena;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
