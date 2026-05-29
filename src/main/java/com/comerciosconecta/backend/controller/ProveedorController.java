package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.ProveedorDTO;
import com.comerciosconecta.backend.entity.EstadoProveedor;
import com.comerciosconecta.backend.entity.TipoProveedor;
import com.comerciosconecta.backend.repository.ProveedorRepository;
import com.comerciosconecta.backend.service.ProveedorService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;
    private final ProveedorRepository proveedorRepository;

    public ProveedorController(ProveedorService proveedorService, ProveedorRepository proveedorRepository) {
        this.proveedorService = proveedorService;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Integer comercioId) {
        if (comercioId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "comercioId es requerido"));
        }
        return ResponseEntity.ok(proveedorService.listarProveedores(comercioId));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProveedorDTO dto) {
        try {
            if (dto.getNombre() == null || dto.getNombre().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es requerido"));
            }
            return ResponseEntity.ok(proveedorService.guardarProveedor(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProveedorDTO dto) {
        try {
            return ResponseEntity.ok(proveedorService.actualizarProveedor(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, @RequestParam(required = false) Integer comercioId) {
        try {
            proveedorService.eliminarProveedor(id, comercioId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("comercioId") Integer comercioId) {

        if (file.isEmpty()) return ResponseEntity.badRequest().body("El archivo está vacío.");

        int exitosos = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();
        List<String> omitidosDetalle = new ArrayList<>();

        try {
            Reader reader = new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            CSVFormat fmt = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            try (CSVParser parser = fmt.parse(reader)) {
                int fila = 1;
                for (CSVRecord rec : parser) {
                    fila++;
                    try {
                        String nombre = rec.get("nombre").trim();
                        if (nombre.isEmpty()) {
                            errores.add("Fila " + fila + ": nombre es obligatorio.");
                            continue;
                        }

                        // Verificar duplicado por nombre + comercio
                        if (proveedorRepository.existsByNombreAndComercioId(nombre, comercioId)) {
                            omitidos++;
                            omitidosDetalle.add("Fila " + fila + " (" + nombre + "): ya existe, omitido.");
                            continue;
                        }

                        String contacto  = safeGet(rec, "contacto");
                        String telefono  = safeGet(rec, "telefono");
                        String email     = safeGet(rec, "email");
                        String direccion = safeGet(rec, "direccion");
                        String tipo      = safeGet(rec, "tipo");
                        String estado    = safeGet(rec, "estado");

                        TipoProveedor tipoEnum;
                        try { tipoEnum = TipoProveedor.valueOf(tipo.isEmpty() ? "General" : tipo); }
                        catch (Exception e) { tipoEnum = TipoProveedor.General; }

                        EstadoProveedor estadoEnum;
                        try { estadoEnum = EstadoProveedor.valueOf(estado.isEmpty() ? "Activo" : estado); }
                        catch (Exception e) { estadoEnum = EstadoProveedor.Activo; }

                        ProveedorDTO dto = new ProveedorDTO();
                        dto.setComercioId(comercioId);
                        dto.setNombre(nombre);
                        dto.setContacto(contacto);
                        dto.setTelefono(telefono);
                        dto.setEmail(email);
                        dto.setDireccion(direccion);
                        dto.setTipo(tipoEnum);
                        dto.setEstado(estadoEnum);
                        dto.setProductos(List.of());

                        proveedorService.guardarProveedor(dto);
                        exitosos++;
                    } catch (Exception e) {
                        errores.add("Fila " + fila + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error procesando el archivo: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "exitosos", exitosos,
            "fallidos", errores.size(),
            "errores", errores,
            "omitidos", omitidos,
            "omitidosDetalle", omitidosDetalle
        ));
    }

    private String safeGet(CSVRecord rec, String col) {
        try { return rec.get(col).trim(); } catch (Exception e) { return ""; }
    }
}
