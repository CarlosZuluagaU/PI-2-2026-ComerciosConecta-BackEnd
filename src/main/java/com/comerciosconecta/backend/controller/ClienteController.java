package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.entity.Cliente;
import com.comerciosconecta.backend.entity.Order;
import com.comerciosconecta.backend.repository.ClienteRepository;
import com.comerciosconecta.backend.repository.OrderRepository;
import com.comerciosconecta.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:3000")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrderRepository orderRepository;

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

    @GetMapping("/{id}/pedidos")
    public ResponseEntity<List<Map<String, Object>>> getPedidos(@PathVariable Long id) {
        Cliente c = clienteService.obtenerClientePorId(id);

        List<Order> orders = (c.getCorreo() != null && !c.getCorreo().isBlank())
            ? orderRepository.findByCustomerEmailIgnoreCaseAndComercioIdOrderByCreatedAtDesc(
                c.getCorreo(), c.getComercioId())
            : List.of();

        // Also include orders matched by document when email returns nothing
        if (orders.isEmpty() && c.getNumeroDocumento() != null && !c.getNumeroDocumento().isBlank()) {
            orders = orderRepository.findByCustomerDocumentAndComercioIdOrderByCreatedAtDesc(
                c.getNumeroDocumento(), c.getComercioId());
        }

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",          o.getId());
            m.put("uuid",        o.getUuid());
            m.put("orderNumber", o.getComercioOrderNumber());
            m.put("total",       o.getTotalInCents() / 100.0);
            m.put("status",      o.getStatus());
            m.put("createdAt",   o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            m.put("itemCount",   o.getItems() != null ? o.getItems().size() : 0);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
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
