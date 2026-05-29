package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.entity.Comercio;
import com.comerciosconecta.backend.repository.ComercioRepository;
import com.comerciosconecta.backend.repository.UsuarioRepository;
import com.comerciosconecta.backend.security.JwtUtil;
import com.comerciosconecta.backend.service.ComercioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comercios")
@CrossOrigin(origins = "http://localhost:3000")
public class ComercioController {

    private final ComercioService comercioService;
    private final ComercioRepository comercioRepository;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public ComercioController(ComercioService comercioService,
                              ComercioRepository comercioRepository,
                              JwtUtil jwtUtil,
                              UsuarioRepository usuarioRepository) {
        this.comercioService = comercioService;
        this.comercioRepository = comercioRepository;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<?> registrarComercio(@RequestBody Comercio comercio) {
        try {
            Comercio nuevo = comercioService.registrarComercio(comercio);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("message", e.getMessage())
            );
        }
    }

    // Devuelve solo el comercio del usuario autenticado (no todos los tenants)
    @GetMapping
    public ResponseEntity<List<Comercio>> listarComercios(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwtUtil.extractUsername(header.substring(7));
                return usuarioRepository.findByEmail(email)
                        .filter(u -> u.getComercio() != null)
                        .flatMap(u -> comercioRepository.findById(Long.valueOf(u.getComercio().getId())))
                        .map(c -> ResponseEntity.ok(List.of(c)))
                        .orElse(ResponseEntity.ok(List.of()));
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}/apariencia")
    public ResponseEntity<?> obtenerApariencia(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(comercioService.obtenerApariencia(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/perfil")
    public ResponseEntity<?> actualizarPerfil(@PathVariable Long id, @RequestBody java.util.Map<String, String> datos) {
        try {
            return ResponseEntity.ok(comercioService.actualizarPerfil(id, datos));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/apariencia")
    public ResponseEntity<?> actualizarApariencia(@PathVariable Long id, @RequestBody Comercio apariencia) {
        try {
            return ResponseEntity.ok(comercioService.actualizarApariencia(id, apariencia));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
