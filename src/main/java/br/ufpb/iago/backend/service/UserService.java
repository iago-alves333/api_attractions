package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.mapper.UserMapper;

import br.ufpb.iago.backend.dto.LoginRequestDTO;
import br.ufpb.iago.backend.dto.UpdateProfileDTO;
import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.exception.AdminRoleChangeException;
import br.ufpb.iago.backend.exception.AlreadyGuideException;
import br.ufpb.iago.backend.exception.BusinessException;
import br.ufpb.iago.backend.exception.DuplicateEmailException;
import br.ufpb.iago.backend.exception.InvalidCredentialsException;
import br.ufpb.iago.backend.exception.UserNotFoundException;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.RefreshTokenRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.refreshTokenRepository = refreshTokenRepository;
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
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Transactional
    public UserResponseDTO saveUser(UserRequestDTO dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.TOURIST);

        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toResponseDTO(user);
    }

    // ─── UPDATE PROFILE ───────────────────────────────────────────────────────

    /**
     * Atualiza o perfil do próprio usuário autenticado.
     * Apenas campos não-nulos são atualizados (partial update).
     */
    @Transactional
    public UserResponseDTO updateProfile(UUID userId, UpdateProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().isBlank()) {
            if (!dto.email().equals(user.getEmail()) &&
                    userRepository.findByEmail(dto.email()).isPresent()) {
                throw new DuplicateEmailException("Email já cadastrado por outro usuário");
            }
            user.setEmail(dto.email());
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
            refreshTokenRepository.deleteAllByUser(user);
        }

        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // ─── PROMOTE ──────────────────────────────────────────────────────────────

    /**
     * Promove um usuário TOURIST para GUIDE.
     * Apenas ADMIN pode executar esta operação (controlado pelo SecurityConfig).
     */
    @Transactional
    public UserResponseDTO promoteToGuide(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.GUIDE) {
            throw new AlreadyGuideException();
        }

        if (user.getRole() == Role.ADMIN) {
            throw new AdminRoleChangeException();
        }

        user.setRole(Role.GUIDE);
        return userMapper.toResponseDTO(userRepository.save(user));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        if (userRepository.hasLinkedRecords(id)) {
            throw new BusinessException("Não é possível excluir usuário que possui atrações, reservas ou avaliações vinculadas");
        }

        refreshTokenRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

}