package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.CompraDTO;
import com.comerciosconecta.backend.dto.CompraResumenDTO;
import com.comerciosconecta.backend.service.CompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<CompraDTO> registrarCompra(@RequestBody CompraDTO dto) {
        return ResponseEntity.ok(compraService.registrarCompra(dto));
    }

    @GetMapping
    public ResponseEntity<List<CompraResumenDTO>> listarCompras(
            @RequestParam(required = false) Integer comercioId) {
        return ResponseEntity.ok(compraService.listarCompras(comercioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDTO> obtenerCompra(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerCompra(id));
    }

    @PatchMapping("/{id}/recibir")
    public ResponseEntity<?> recibirCompra(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("ok", true, "compra", compraService.recibirCompra(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
