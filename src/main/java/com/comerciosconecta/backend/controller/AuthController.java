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
        comercio.setTelefono(req.getTelefono());
        comercio.setEmail(req.getEmail());
        comercio = comercioRepository.save(comercio);

        // Obtener o crear rol ADMIN
        Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN").orElseGet(() -> {
            Rol r = new Rol(); r.setNombre("ROLE_ADMIN"); r.setDescripcion("Administrador");
            return rolRepository.save(r);
        });

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setEmail(req.getEmail());
        usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        usuario.setEstado(EstadoGeneral.Activo);
        usuario.setComercio(comercio);
        usuario.getRoles().add(rolAdmin);
        usuarioRepository.save(usuario);

        // Devolver tokens igual que en login
        UserDetails ud = userDetailsService.loadUserByUsername(req.getEmail());
        String accessToken = jwtUtil.generateAccessToken(ud);
        String refreshToken = jwtUtil.generateRefreshToken(ud);
        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken, comercio.getId(), usuario.getNombre()));
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserDetails ud = (UserDetails) auth.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(ud);
        String refreshToken = jwtUtil.generateRefreshToken(ud);

        var usuarioOpt = usuarioRepository.findByEmail(req.getEmail());
        Integer comercioId = usuarioOpt.map(u -> u.getComercio() != null ? u.getComercio().getId() : null).orElse(null);
        String nombre = usuarioOpt.map(com.comerciosconecta.backend.entity.Usuario::getNombre).orElse(null);

        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken, comercioId, nombre));
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
            if (body.containsKey("telefono")) u.setTelefono(body.get("telefono"));
            if (body.containsKey("password") && !body.get("password").isBlank())
                u.setPassword(passwordEncoder.encode(body.get("password")));

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

            java.util.HashMap<String, Object> resp = new java.util.HashMap<>();
            resp.put("nombre", u.getNombre() != null ? u.getNombre() : "");
            resp.put("email", u.getEmail());
            resp.put("telefono", u.getTelefono() != null ? u.getTelefono() : "");

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
            Integer cid = usuarioRepository.findByEmail(email)
                    .map(u -> u.getComercio() != null ? u.getComercio().getId() : null)
                    .orElse(null);
            var u2 = usuarioRepository.findByEmail(email).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "email", email,
                "nombre", u2.getNombre() != null ? u2.getNombre() : "",
                "telefono", u2.getTelefono() != null ? u2.getTelefono() : "",
                "comercioId", cid != null ? cid : ""
            ));
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
