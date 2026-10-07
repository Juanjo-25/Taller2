package com.example.yate.Modelos.Entity;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Cliente {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        private String nombre;
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        private String apellido;
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingrese un correo válido")
        @Size(max = 254, message = "Máximo 254 caracteres")
        private String email;
        private Date createAt;

        public Cliente() {
        }

        public Cliente(String nombre, String apellido, String email, Long id, Date createAt) {
            this.nombre = nombre;
            this.apellido = apellido;
            this.email = email;
            this.id = id;
            this.createAt = createAt;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setCreateAt(Date createAt) {
            this.createAt = createAt;
        }

        public String getNombre() {
            return nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public String getEmail() {
            return email;
        }

        public Long getId() {
            return id;
        }

        public Date getCreateAt() {
            return createAt;
        }

        @Override
        public String toString() {
            return "Cliente nombre:" + nombre + ", apellido" + apellido + ", email" + email + ", id" + id
                    + ", createAt" + createAt + "]";
        }
}
