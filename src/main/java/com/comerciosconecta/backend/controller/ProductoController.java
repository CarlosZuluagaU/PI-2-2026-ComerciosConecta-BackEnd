package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.ProductoDTO;
import com.comerciosconecta.backend.entity.Comercio;
import com.comerciosconecta.backend.entity.EstadoGeneral;
import com.comerciosconecta.backend.entity.Producto;
import com.comerciosconecta.backend.repository.ComercioRepository;
import com.comerciosconecta.backend.repository.ProductoRepository;
import com.comerciosconecta.backend.repository.UsuarioRepository;
import com.comerciosconecta.backend.security.JwtUtil;
import com.comerciosconecta.backend.service.ProductoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;

    public ProductoController(ProductoService productoService, JwtUtil jwtUtil,
                              UsuarioRepository usuarioRepository, ProductoRepository productoRepository,
                              ComercioRepository comercioRepository) {
        this.productoService = productoService;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.comercioRepository = comercioRepository;
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

    // Categorías distintas del comercio (para combobox dinámico)
    @GetMapping("/categorias")
    public ResponseEntity<List<String>> getCategorias(
            @RequestParam(required = false) Integer comercioId,
            HttpServletRequest request) {
        Integer cid = comercioId != null ? comercioId : extractComercioId(request);
        if (cid == null) return ResponseEntity.ok(List.of());
        List<String> cats = productoRepository.findDistinctCategoriasByComercioId(Long.valueOf(cid));
        cats.sort(String::compareToIgnoreCase);
        return ResponseEntity.ok(cats);
    }

    // Marcas distintas del comercio (para combobox dinámico)
    @GetMapping("/marcas")
    public ResponseEntity<List<String>> getMarcas(
            @RequestParam(required = false) Integer comercioId,
            HttpServletRequest request) {
        Integer cid = comercioId != null ? comercioId : extractComercioId(request);
        if (cid == null) return ResponseEntity.ok(List.of());
        List<String> marcas = productoRepository.findDistinctMarcasByComercioId(Long.valueOf(cid));
        marcas.sort(String::compareToIgnoreCase);
        return ResponseEntity.ok(marcas);
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

    // Carga masiva de productos desde CSV o Excel
    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("comercioId") Long comercioId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo está vacío.");
        }

        Comercio comercio = comercioRepository.findById(comercioId).orElse(null);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        int exitosos = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();
        List<String> omitidosDetalle = new ArrayList<>();

        try {
            List<String[]> filas = new ArrayList<>();

            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                // Parsear Excel
                try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
                    Sheet sheet = wb.getSheetAt(0);
                    boolean primera = true;
                    for (Row row : sheet) {
                        if (primera) { primera = false; continue; } // saltar cabecera
                        String[] cols = new String[9];
                        for (int c = 0; c < 9; c++) {
                            Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            cols[c] = switch (cell.getCellType()) {
                                case NUMERIC -> {
                                    double v = cell.getNumericCellValue();
                                    yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
                                }
                                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                                default -> cell.getStringCellValue().trim();
                            };
                        }
                        filas.add(cols);
                    }
                }
            } else {
                // Parsear CSV
                Reader reader = new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
                CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).build();
                try (CSVParser parser = csvFormat.parse(reader)) {
                    for (CSVRecord rec : parser) {
                        String imagenUrl = "";
                        try { imagenUrl = rec.get("imagen_url"); } catch (Exception ignored) {}
                        // Support both old ("precio") and new ("precio_venta") column names
                        String precioVentaStr = "";
                        try { precioVentaStr = rec.get("precio_venta"); } catch (Exception ignored) {}
                        if (precioVentaStr.isBlank()) {
                            try { precioVentaStr = rec.get("precio"); } catch (Exception ignored) {}
                        }
                        String precioCompraStr = "";
                        try { precioCompraStr = rec.get("precio_compra"); } catch (Exception ignored) {}
                        // Array order matches column processor: nombre, desc, precio_compra, precio_venta, stock, iva, cat, codigo, imagen
                        filas.add(new String[]{
                            rec.get("nombre"), rec.get("descripcion"), precioCompraStr, precioVentaStr,
                            rec.get("stock"), rec.get("iva_porcentaje"), rec.get("categoria"),
                            rec.get("codigo"), imagenUrl
                        });
                    }
                }
            }

            int fila = 1;
            for (String[] cols : filas) {
                fila++;
                try {
                    // Column order: nombre, descripcion, precio_compra, precio_venta, stock, iva_porcentaje, categoria, codigo, imagen_url
                    String nombre       = cols[0].trim();
                    String desc         = cols[1].trim();
                    String pcStr        = cols[2].trim();
                    double precioVenta  = Double.parseDouble(cols[3].trim().replaceAll("[.,](?=\\d{3})", ""));
                    int stock           = Integer.parseInt(cols[4].trim());
                    int iva             = Integer.parseInt(cols[5].trim());
                    String cat          = cols[6].trim();
                    String codigo       = cols[7].trim();
                    String imagenUrl    = cols.length > 8 ? cols[8].trim() : "";

                    if (nombre.isEmpty() || codigo.isEmpty()) {
                        errores.add("Fila " + fila + ": nombre y código son obligatorios.");
                        continue;
                    }
                    if (pcStr.isEmpty()) {
                        errores.add("Fila " + fila + ": precio_compra es obligatorio.");
                        continue;
                    }
                    double precioCompra = Double.parseDouble(pcStr.replaceAll("[.,](?=\\d{3})", ""));
                    if (precioCompra <= 0) {
                        errores.add("Fila " + fila + ": precio_compra debe ser mayor a 0.");
                        continue;
                    }

                    // Verificar duplicado por código + comercio (la misma ref es válida en otro comercio)
                    boolean existe = comercio != null
                        ? productoRepository.existsByReferenciaAndComercio_Id(codigo, comercioId)
                        : productoRepository.existsByReferencia(codigo);
                    if (existe) {
                        omitidos++;
                        omitidosDetalle.add("Fila " + fila + " (" + codigo + "): ya existe, omitido.");
                        continue;
                    }

                    Producto p = new Producto();
                    p.setNombre(nombre);
                    p.setDescripcion(desc);
                    p.setReferencia(codigo);
                    p.setPrecioVenta(precioVenta);
                    p.setPrecioCompra(precioCompra);
                    p.setIva(iva);
                    p.setCategoria(cat);
                    p.setStock(stock);
                    p.setStockMinimo(5);
                    p.setEstado(EstadoGeneral.Activo);
                    p.setFechaActualizacion(LocalDateTime.now());
                    if (!imagenUrl.isEmpty()) p.setImagenUrl(imagenUrl);
                    if (comercio != null) p.setComercio(comercio);

                    productoRepository.save(p);
                    exitosos++;
                } catch (Exception ex) {
                    errores.add("Fila " + fila + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se pudo procesar el archivo: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "exitosos", exitosos,
            "fallidos", errores.size(),
            "errores", errores,
            "omitidos", omitidos,
            "omitidosDetalle", omitidosDetalle
        ));
    }
}
