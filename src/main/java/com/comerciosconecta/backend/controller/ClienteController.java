package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.entity.Cliente;
import com.comerciosconecta.backend.repository.ClienteRepository;
import com.comerciosconecta.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:3000")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    @PostMapping
    public ResponseEntity<?> registrarCliente(@RequestBody Cliente cliente,
                                              @RequestParam(required = false) Integer comercioId) {
        if (comercioId != null) cliente.setComercioId(comercioId);
        try {
            Cliente nuevo = clienteService.registrarCliente(cliente);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", e.getMessage())
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes(@RequestParam(required = false) Integer comercioId) {
        if (comercioId != null) return ResponseEntity.ok(clienteRepository.findByComercioId(comercioId));
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(
            @PathVariable Long id,
            @RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, cliente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
