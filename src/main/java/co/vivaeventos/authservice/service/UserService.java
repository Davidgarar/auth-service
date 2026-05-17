package co.vivaeventos.authservice.service;

import co.vivaeventos.authservice.dto.AuthResponse;
import co.vivaeventos.authservice.dto.LoginRequest;
import co.vivaeventos.authservice.dto.RegisterRequest;
import co.vivaeventos.authservice.model.User;
import co.vivaeventos.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando usuario: {}", request.getEmail());

        // Verificar si el email ya existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        // Asignar rol (por defecto CLIENT)
        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT";
        if (!role.equals("ADMIN") && !role.equals("ORGANIZER") && !role.equals("CLIENT")) {
            role = "CLIENT";
        }
        user.setRole(role);

        userRepository.save(user);
        log.info("Usuario registrado exitosamente: {}", user.getEmail());

        // Generar token
        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getName());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login de usuario: {}", request.getEmail());

        // Buscar usuario
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email o contraseña incorrectos"));

        // Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email o contraseña incorrectos");
        }

        log.info("Login exitoso: {}", user.getEmail());

        // Generar token
        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getName());
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}