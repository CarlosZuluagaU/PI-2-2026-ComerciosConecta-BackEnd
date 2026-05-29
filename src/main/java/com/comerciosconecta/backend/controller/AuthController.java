package com.comerciosconecta.backend.controller;

import com.comerciosconecta.backend.dto.*;
import com.comerciosconecta.backend.entity.*;
import com.comerciosconecta.backend.repository.*;
import com.comerciosconecta.backend.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ComercioRepository comercioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // ===== REGISTER =====
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (usuarioRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ya existe un usuario con ese correo"));
        }

        // Crear comercio
        Comercio comercio = new Comercio();
        comercio.setNombre(req.getComercioNombre());
        comercio.setNit(req.getNit());
        comercio.setDireccion(req.getDireccion());
        comercio.setDepartamento(req.getDepartamento());
        comercio.setCiudad(req.getCiudad());
        comercio.setTelefono(req.getTelefono());
        comercio.setCategorias(req.getCategoria());
        comercio.setEmail(req.getEmail());
        // perfilCompleto = false when skipped (nit placeholder "00000000")
        comercio.setPerfilCompleto(!"00000000".equals(req.getNit()));
        comercio = comercioRepository.save(comercio);

        // Obtener o crear rol ADMIN
        Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN").orElseGet(() -> {
            Rol r = new Rol(); r.setNombre("ROLE_ADMIN"); r.setDescripcion("Administrador");
            return rolRepository.save(r);
        });

        // Crear usuario — dividir nombre completo si no viene apellido separado
        String nombreBase = req.getNombre() != null ? req.getNombre().trim() : "";
        String apellidoBase = req.getApellido() != null && !req.getApellido().isBlank() ? req.getApellido().trim() : null;
        if (apellidoBase == null && nombreBase.contains(" ")) {
            int idx = nombreBase.indexOf(' ');
            apellidoBase = nombreBase.substring(idx + 1).trim();
            nombreBase  = nombreBase.substring(0, idx).trim();
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreBase);
        if (apellidoBase != null && !apellidoBase.isBlank()) usuario.setApellido(apellidoBase);
        usuario.setEmail(req.getEmail());
        usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        if (req.getGoogleSub() != null && !req.getGoogleSub().isBlank()) {
            usuario.setGoogleSub(req.getGoogleSub());
        }
        usuario.setEstado(EstadoGeneral.Activo);
        usuario.setComercio(comercio);
        usuario.getRoles().add(rolAdmin);
        usuarioRepository.save(usuario);

        // Devolver tokens igual que en login
        UserDetails ud = userDetailsService.loadUserByUsername(req.getEmail());
        String accessToken = jwtUtil.generateAccessToken(ud);
        String refreshToken = jwtUtil.generateRefreshToken(ud);
        String fullName = usuario.getNombre() + (usuario.getApellido() != null && !usuario.getApellido().isBlank() ? " " + usuario.getApellido() : "");
        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken, comercio.getId(), fullName));
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // Google sub auth: check stored google_sub before BCrypt (avoids authManager for Google logins)
        if (req.getPassword() != null && req.getPassword().startsWith("google_")) {
            String sub = req.getPassword().substring(7);
            var uOpt = usuarioRepository.findByEmail(req.getEmail());
            if (uOpt.isPresent() && sub.equals(uOpt.get().getGoogleSub())) {
                Usuario u = uOpt.get();
                UserDetails ud2 = userDetailsService.loadUserByUsername(u.getEmail());
                String at = jwtUtil.generateAccessToken(ud2);
                String rt = jwtUtil.generateRefreshToken(ud2);
                Integer cid = u.getComercio() != null ? u.getComercio().getId() : null;
                String fn = u.getNombre() != null ? u.getNombre() : "";
                if (u.getApellido() != null && !u.getApellido().isBlank()) fn += " " + u.getApellido();
                return ResponseEntity.ok(new AuthResponse(at, jwtUtil.getAccessExpirationMs(), rt, cid, fn));
            }
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserDetails ud = (UserDetails) auth.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(ud);
        String refreshToken = jwtUtil.generateRefreshToken(ud);

        var usuarioOpt = usuarioRepository.findByEmail(req.getEmail());
        Integer comercioId = usuarioOpt.map(u -> u.getComercio() != null ? u.getComercio().getId() : null).orElse(null);
        String nombre = usuarioOpt.map(u -> {
            String n = u.getNombre() != null ? u.getNombre() : "";
            return (u.getApellido() != null && !u.getApellido().isBlank()) ? n + " " + u.getApellido() : n;
        }).orElse(null);

        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken, comercioId, nombre));
    }

    // ===== LINK GOOGLE =====
    @Transactional
    @PostMapping("/link-google")
    public ResponseEntity<?> linkGoogle(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String googleAccessToken = body.get("googleAccessToken");
        if (googleAccessToken == null || googleAccessToken.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Token de Google requerido"));

        try {
            // Fetch Google user info
            RestTemplate rt = new RestTemplate();
            org.springframework.http.HttpHeaders gh = new org.springframework.http.HttpHeaders();
            gh.set("Authorization", "Bearer " + googleAccessToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> gData = rt.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(gh),
                Map.class
            ).getBody();

            if (gData == null) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "No se pudo verificar el token de Google"));

            String gEmail = (String) gData.get("email");
            String gSub   = (String) gData.get("sub");
            String gName  = (String) gData.getOrDefault("name", "");
            String gPic   = (String) gData.getOrDefault("picture", "");

            String currentEmail = jwtUtil.extractUsername(header.substring(7));
            if (!currentEmail.equalsIgnoreCase(gEmail)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "El correo de Google (" + gEmail + ") no coincide con tu cuenta (" + currentEmail + ")"
                ));
            }

            var uOpt = usuarioRepository.findByEmail(currentEmail);
            if (uOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

            Usuario u = uOpt.get();
            u.setGoogleSub(gSub);
            usuarioRepository.save(u);

            return ResponseEntity.ok(Map.of(
                "message",       "Cuenta vinculada con Google exitosamente",
                "googleEmail",   gEmail,
                "googleName",    gName,
                "googlePicture", gPic,
                "googleSub",     gSub
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error al vincular: " + e.getMessage()));
        }
    }

    // ===== REFRESH TOKEN =====
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refresh = body.get("refreshToken");
        if (refresh == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            String username = jwtUtil.extractUsername(refresh);
            UserDetails ud = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.isTokenExpired(refresh) && jwtUtil.validateToken(refresh, ud)) {
                String newAccess = jwtUtil.generateAccessToken(ud);

                return ResponseEntity.ok(new AuthResponse(newAccess, jwtUtil.getAccessExpirationMs(), refresh));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // ===== UPDATE PROFILE =====
    @Transactional
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String currentEmail = jwtUtil.extractUsername(header.substring(7));
            var usuarioOpt = usuarioRepository.findByEmail(currentEmail);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            com.comerciosconecta.backend.entity.Usuario u = usuarioOpt.get();

            if (body.containsKey("nombre") && !body.get("nombre").isBlank()) u.setNombre(body.get("nombre"));
            if (body.containsKey("apellido")) u.setApellido(body.get("apellido"));
            if (body.containsKey("telefono")) u.setTelefono(body.get("telefono"));
            if (body.containsKey("tipoDocumento")) u.setTipoDocumento(body.get("tipoDocumento"));
            if (body.containsKey("numeroDocumento")) u.setNumeroDocumento(body.get("numeroDocumento"));
            if (body.containsKey("fechaNacimiento")) u.setFechaNacimiento(body.get("fechaNacimiento"));
            if (body.containsKey("ciudad")) u.setCiudad(body.get("ciudad"));
            if (body.containsKey("direccion")) u.setDireccion(body.get("direccion"));
            if (body.containsKey("biografia")) u.setBiografia(body.get("biografia"));
            if (body.containsKey("password") && !body.get("password").isBlank()) {
                String currentPassword = body.get("currentPassword");
                if (currentPassword == null || currentPassword.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Debes ingresar tu contraseña actual para cambiarla"));
                }
                if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "La contraseña actual es incorrecta"));
                }
                u.setPassword(passwordEncoder.encode(body.get("password")));
            }

            boolean emailChanged = false;
            if (body.containsKey("email") && !body.get("email").isBlank()) {
                String newEmail = body.get("email").trim().toLowerCase();
                if (!newEmail.equals(u.getEmail())) {
                    if (usuarioRepository.findByEmail(newEmail).isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Ya existe una cuenta con ese correo"));
                    }
                    u.setEmail(newEmail);
                    emailChanged = true;
                }
            }

            usuarioRepository.save(u);
            usuarioRepository.flush(); // garantiza que el INSERT/UPDATE llega a la DB dentro de la transacción

            // Re-fetch desde DB para responder con lo que realmente quedó persistido
            com.comerciosconecta.backend.entity.Usuario persisted = usuarioRepository.findByEmail(u.getEmail()).orElse(u);

            java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
            resp.put("nombre",          persisted.getNombre()          != null ? persisted.getNombre()          : "");
            resp.put("apellido",        persisted.getApellido()        != null ? persisted.getApellido()        : "");
            resp.put("email",           persisted.getEmail());
            resp.put("telefono",        persisted.getTelefono()        != null ? persisted.getTelefono()        : "");
            resp.put("tipoDocumento",   persisted.getTipoDocumento()   != null ? persisted.getTipoDocumento()   : "");
            resp.put("numeroDocumento", persisted.getNumeroDocumento() != null ? persisted.getNumeroDocumento() : "");
            resp.put("fechaNacimiento", persisted.getFechaNacimiento() != null ? persisted.getFechaNacimiento() : "");
            resp.put("ciudad",          persisted.getCiudad()          != null ? persisted.getCiudad()          : "");
            resp.put("direccion",       persisted.getDireccion()       != null ? persisted.getDireccion()       : "");
            resp.put("biografia",       persisted.getBiografia()       != null ? persisted.getBiografia()       : "");

            // Si el email cambió, el JWT anterior ya no es válido — devolver nuevos tokens
            if (emailChanged) {
                UserDetails ud = userDetailsService.loadUserByUsername(u.getEmail());
                resp.put("accessToken", jwtUtil.generateAccessToken(ud));
                resp.put("refreshToken", jwtUtil.generateRefreshToken(ud));
            }

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // ===== ME =====
    @GetMapping("/me")
    public ResponseEntity<?> me(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String token = header.substring(7);
            String email = jwtUtil.extractUsername(token);
            var uOpt = usuarioRepository.findByEmail(email);
            if (uOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            com.comerciosconecta.backend.entity.Usuario u = uOpt.get();
            Integer cid = u.getComercio() != null ? u.getComercio().getId() : null;
            java.util.HashMap<String, Object> me = new java.util.HashMap<>();
            me.put("email",           u.getEmail());
            me.put("nombre",          u.getNombre()          != null ? u.getNombre()          : "");
            me.put("apellido",        u.getApellido()        != null ? u.getApellido()        : "");
            me.put("telefono",        u.getTelefono()        != null ? u.getTelefono()        : "");
            me.put("tipoDocumento",   u.getTipoDocumento()   != null ? u.getTipoDocumento()   : "");
            me.put("numeroDocumento", u.getNumeroDocumento() != null ? u.getNumeroDocumento() : "");
            me.put("fechaNacimiento", u.getFechaNacimiento() != null ? u.getFechaNacimiento() : "");
            me.put("ciudad",          u.getCiudad()          != null ? u.getCiudad()          : "");
            me.put("direccion",       u.getDireccion()       != null ? u.getDireccion()       : "");
            me.put("biografia",       u.getBiografia()       != null ? u.getBiografia()       : "");
            me.put("comercioId",       cid != null ? cid : "");
            me.put("comercioNombre",   u.getComercio() != null && u.getComercio().getNombre() != null ? u.getComercio().getNombre() : "");
            me.put("perfilCompleto",   u.getComercio() != null && u.getComercio().isPerfilCompleto());
            me.put("googleLinked",     u.getGoogleSub() != null && !u.getGoogleSub().isBlank());
            me.put("createdAt",        u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return ResponseEntity.ok(me);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // ===== LOGOUT =====
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        return ResponseEntity.ok().build();
    }
}
