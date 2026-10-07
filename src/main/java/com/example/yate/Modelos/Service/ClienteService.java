package com.example.yate.Modelos.Service;

import java.util.Date;
import java.util.List;
import com.example.yate.Modelos.Entity.Cliente;
import com.example.yate.Modelos.DAO.ClienteDAO;
import com.example.yate.Modelos.DAO.EncabezadoDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional(readOnly = true)
public class ClienteService {
    private final ClienteDAO clienteDAO;
    private final EncabezadoDAO referencias;
    private final LoginService loginService;

    public ClienteService(ClienteDAO clienteDAO, LoginService loginService, EncabezadoDAO referencias) {
        this.clienteDAO = clienteDAO;
        this.referencias = referencias;
        this.loginService = loginService;
    }

    public List<Cliente> listarClientes() {
        return clienteDAO.findAll();
    }

    public Cliente buscarCliente(Long id) {
        return clienteDAO.findById(id)
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
        return clienteDAO.save(cliente);
    }

    public boolean tienePerfil(String email) {
        return clienteDAO.existsByLoginEmail(email);
    }

    public Cliente buscarPerfil(String email) {
        return clienteDAO.findByLoginEmail(email).orElseGet(() -> {
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
        clienteDAO.save(cliente);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        if (referencias.existsByClienteId(id)) {
            throw new IllegalArgumentException("No puede eliminar un cliente con compras registradas");
        }
        clienteDAO.delete(buscarCliente(id));
    }
}
