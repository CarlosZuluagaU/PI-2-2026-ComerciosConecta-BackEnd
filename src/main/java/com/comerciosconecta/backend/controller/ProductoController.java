package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.ProductoDTO;
import com.comerciosconecta.backend.repository.UsuarioRepository;
import com.comerciosconecta.backend.security.JwtUtil;
import com.comerciosconecta.backend.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public ProductoController(ProductoService productoService, JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.productoService = productoService;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    private Integer extractComercioId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String email = jwtUtil.extractUsername(token);
                return usuarioRepository.findByEmail(email)
                        .map(u -> u.getComercio() != null ? u.getComercio().getId() : null)
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // Crear un nuevo producto
    @PostMapping
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoDTO productoDTO, HttpServletRequest request) {
        try {
            Integer comercioId = extractComercioId(request);
            ProductoDTO creado = productoService.crearProducto(productoDTO, comercioId);
            return ResponseEntity.ok(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Error interno al guardar el producto";
            if (msg.contains("referencia") || msg.contains("unique") || msg.contains("Unique") || msg.contains("ConstraintViolation")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Ya existe un producto con esa referencia"));
            }
            return ResponseEntity.internalServerError().body(Map.of("message", msg));
        }
    }

    // Listar todos los productos
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarProductos(
            @RequestParam(required = false) Integer comercioId,
            HttpServletRequest request) {
        Integer cid = comercioId != null ? comercioId : extractComercioId(request);
        return ResponseEntity.ok(productoService.listarProductos(cid));
    }

    // Obtener un producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerProducto(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Actualizar producto existente
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoDTO productoDTO) {
        try {
            return ResponseEntity.ok(productoService.actualizarProducto(id, productoDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Error al actualizar el producto";
            if (msg.contains("referencia") || msg.contains("unique") || msg.contains("ConstraintViolation")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Ya existe un producto con esa referencia"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    // Productos con stock bajo (stock <= stockMinimo)
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductoDTO>> getLowStock() {
        return ResponseEntity.ok(productoService.getLowStockProductos());
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        try {
            productoService.eliminarProducto(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
