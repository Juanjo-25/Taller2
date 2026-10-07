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

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
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

    @Transactional
    public void eliminarCliente(Long id) {
        clienteRepository.delete(buscarCliente(id));
    }
}
