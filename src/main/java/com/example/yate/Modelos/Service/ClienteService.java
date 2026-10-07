package com.example.yate.Modelos.Service;

import java.util.Date;
import java.util.List;
import com.example.yate.Modelos.Entity.Cliente;
import com.example.yate.Modelos.Repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional(readOnly = true)
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final LoginService loginService;

    public ClienteService(ClienteRepository clienteRepository, LoginService loginService) {
        this.clienteRepository = clienteRepository;
        this.loginService = loginService;
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @Transactional
    public Cliente guardarCliente(Cliente datos) {
        Cliente cliente = datos.getId() == null ? new Cliente() : buscarCliente(datos.getId());
        cliente.setNombre(datos.getNombre().trim());
        cliente.setApellido(datos.getApellido().trim());
        cliente.setEmail(datos.getEmail().trim());
        if (cliente.getId() == null) {
            cliente.setCreateAt(new Date());
        }
        return clienteRepository.save(cliente);
    }

    public boolean tienePerfil(String email) {
        return clienteRepository.existsByLoginEmail(email);
    }

    public Cliente buscarPerfil(String email) {
        return clienteRepository.findByLoginEmail(email).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setEmail(email);
            return cliente;
        });
    }

    @Transactional
    public void guardarPerfil(String email, Cliente datos) {
        Cliente cliente = buscarPerfil(email);
        cliente.setNombre(datos.getNombre().trim());
        cliente.setApellido(datos.getApellido().trim());
        cliente.setEmail(datos.getEmail().trim());
        if (cliente.getId() == null) {
            cliente.setCreateAt(new Date());
            cliente.setLogin(loginService.buscarLogin(email));
        }
        clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        clienteRepository.delete(buscarCliente(id));
    }
}
