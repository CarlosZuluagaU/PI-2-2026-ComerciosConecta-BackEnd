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
        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken));
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        UserDetails ud = (UserDetails) auth.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(ud);
        String refreshToken = jwtUtil.generateRefreshToken(ud);

        // Devuelve ambos tokens en el body
        return ResponseEntity.ok(new AuthResponse(accessToken, jwtUtil.getAccessExpirationMs(), refreshToken));
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

    // ===== LOGOUT =====
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        return ResponseEntity.ok().build();
    }
}
