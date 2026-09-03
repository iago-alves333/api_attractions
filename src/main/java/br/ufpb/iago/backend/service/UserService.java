package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.LoginRequestDTO;
import br.ufpb.iago.backend.dto.UpdateProfileDTO;
import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.exception.BusinessException;
import br.ufpb.iago.backend.exception.InvalidCredentialsException;
import br.ufpb.iago.backend.exception.ResourceNotFoundException;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    /**
     * Valida as credenciais e retorna o User autenticado.
     * Lança InvalidCredentialsException (→ 401) em qualquer falha,
     * sem distinguir "e-mail não existe" de "senha errada" para evitar
     * enumeração de usuários.
     */
    @Transactional(readOnly = true)
    public User login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public UserResponseDTO saveUser(UserRequestDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BusinessException("Email já cadastrado");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.TOURIST);

        return convertToDTO(userRepository.save(user));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return convertToDTO(user);
    }

    // ─── UPDATE PROFILE ───────────────────────────────────────────────────────

    /**
     * Atualiza o perfil do próprio usuário autenticado.
     * Apenas campos não-nulos são atualizados (partial update).
     */
    @Transactional
    public UserResponseDTO updateProfile(UUID userId, UpdateProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!dto.getEmail().equals(user.getEmail()) &&
                    userRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new BusinessException("Email já cadastrado por outro usuário");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return convertToDTO(userRepository.save(user));
    }

    // ─── PROMOTE ──────────────────────────────────────────────────────────────

    /**
     * Promove um usuário TOURIST para GUIDE.
     * Apenas ADMIN pode executar esta operação (controlado pelo SecurityConfig).
     */
    @Transactional
    public UserResponseDTO promoteToGuide(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getRole() == Role.GUIDE) {
            throw new BusinessException("Este usuário já é um GUIDE");
        }

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("Não é possível alterar o role de um ADMIN");
        }

        user.setRole(Role.GUIDE);
        return convertToDTO(userRepository.save(user));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        userRepository.deleteById(id);
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    public UserResponseDTO convertToDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}