package br.ufpb.iago.backend.service;

import br.ufpb.iago.backend.dto.LoginRequestDTO;
import br.ufpb.iago.backend.dto.UpdateProfileDTO;
import br.ufpb.iago.backend.dto.UserRequestDTO;
import br.ufpb.iago.backend.dto.UserResponseDTO;
import br.ufpb.iago.backend.exception.AdminRoleChangeException;
import br.ufpb.iago.backend.exception.AlreadyGuideException;
import br.ufpb.iago.backend.exception.DuplicateEmailException;
import br.ufpb.iago.backend.exception.InvalidCredentialsException;
import br.ufpb.iago.backend.exception.UserNotFoundException;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User tourist;
    private User guide;
    private User admin;

    @BeforeEach
    void setUp() {
        tourist = createUser("Turista", "turista@test.com", Role.TOURIST);
        guide = createUser("Guia", "guia@test.com", Role.GUIDE);
        admin = createUser("Admin", "admin@test.com", Role.ADMIN);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private User createUser(String name, String email, Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }

    private UserRequestDTO createRegisterDTO(String name, String email, String password) {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    private LoginRequestDTO createLoginDTO(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Deve autenticar com credenciais válidas")
        void login_comCredenciaisValidas_retornaUser() {
            LoginRequestDTO dto = createLoginDTO("turista@test.com", "senha123");

            when(userRepository.findByEmail("turista@test.com")).thenReturn(Optional.of(tourist));
            when(passwordEncoder.matches("senha123", "encoded-password")).thenReturn(true);

            User result = userService.login(dto);

            assertNotNull(result);
            assertEquals("turista@test.com", result.getEmail());
            assertEquals("Turista", result.getName());
            verify(userRepository).findByEmail("turista@test.com");
        }

        @Test
        @DisplayName("Deve lançar InvalidCredentialsException quando email não existe")
        void login_emailInexistente_lancaInvalidCredentials() {
            LoginRequestDTO dto = createLoginDTO("naoexiste@test.com", "senha123");

            when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> userService.login(dto));
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Deve lançar InvalidCredentialsException quando senha está errada")
        void login_senhaErrada_lancaInvalidCredentials() {
            LoginRequestDTO dto = createLoginDTO("turista@test.com", "senha-errada");

            when(userRepository.findByEmail("turista@test.com")).thenReturn(Optional.of(tourist));
            when(passwordEncoder.matches("senha-errada", "encoded-password")).thenReturn(false);

            assertThrows(InvalidCredentialsException.class, () -> userService.login(dto));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE USER (REGISTER)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("saveUser()")
    class SaveUserTests {

        @Test
        @DisplayName("Deve criar usuário com role TOURIST")
        void saveUser_dadosValidos_criaComoTourist() {
            UserRequestDTO dto = createRegisterDTO("Novo User", "novo@test.com", "senha123");

            when(userRepository.findByEmail("novo@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("senha123")).thenReturn("encoded-senha123");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            UserResponseDTO result = userService.saveUser(dto);

            assertNotNull(result);
            assertEquals("Novo User", result.name());
            assertEquals(Role.TOURIST, result.role());

            // Verifica que a senha foi codificada e o role é TOURIST
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();
            assertEquals("encoded-senha123", savedUser.getPassword());
            assertEquals(Role.TOURIST, savedUser.getRole());
        }

        @Test
        @DisplayName("Deve lançar DuplicateEmailException quando email já existe")
        void saveUser_emailDuplicado_lancaDuplicateEmail() {
            UserRequestDTO dto = createRegisterDTO("Outro", "turista@test.com", "senha123");

            when(userRepository.findByEmail("turista@test.com")).thenReturn(Optional.of(tourist));

            assertThrows(DuplicateEmailException.class, () -> userService.saveUser(dto));
            verify(userRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIND ALL
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Deve retornar lista de todos os usuários")
        void findAll_retornaListaDeUsers() {
            when(userRepository.findAll()).thenReturn(List.of(tourist, guide, admin));

            List<UserResponseDTO> result = userService.findAll();

            assertEquals(3, result.size());
            assertEquals("Turista", result.get(0).name());
            assertEquals("Guia", result.get(1).name());
            assertEquals("Admin", result.get(2).name());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há usuários")
        void findAll_semUsers_retornaListaVazia() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserResponseDTO> result = userService.findAll();

            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FIND BY ID
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar usuário quando ID existe")
        void findById_idExistente_retornaUser() {
            UUID id = tourist.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));

            UserResponseDTO result = userService.findById(id);

            assertNotNull(result);
            assertEquals("Turista", result.name());
            assertEquals(Role.TOURIST, result.role());
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando ID não existe")
        void findById_idInexistente_lancaUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.findById(id));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE PROFILE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("Deve atualizar apenas o nome quando só o nome é enviado")
        void updateProfile_apenasNome_atualizaSoNome() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setName("Novo Nome");

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponseDTO result = userService.updateProfile(id, dto);

            assertEquals("Novo Nome", result.name());
            assertEquals("turista@test.com", tourist.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Deve atualizar o email quando novo email é válido")
        void updateProfile_emailNovo_atualizaEmail() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setEmail("novoemail@test.com");

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.findByEmail("novoemail@test.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateProfile(id, dto);

            assertEquals("novoemail@test.com", tourist.getEmail());
        }

        @Test
        @DisplayName("Deve lançar DuplicateEmailException quando novo email já existe")
        void updateProfile_emailJaExiste_lancaDuplicateEmail() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setEmail("guia@test.com");

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.findByEmail("guia@test.com")).thenReturn(Optional.of(guide));

            assertThrows(DuplicateEmailException.class,
                    () -> userService.updateProfile(id, dto));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve permitir manter o mesmo email sem erro")
        void updateProfile_mesmoEmail_naoLancaErro() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setEmail("turista@test.com");

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            assertDoesNotThrow(() -> userService.updateProfile(id, dto));
        }

        @Test
        @DisplayName("Deve codificar a nova senha ao atualizar")
        void updateProfile_novaSenha_codificaSenha() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setPassword("nova-senha");

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(passwordEncoder.encode("nova-senha")).thenReturn("encoded-nova-senha");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateProfile(id, dto);

            assertEquals("encoded-nova-senha", tourist.getPassword());
            verify(passwordEncoder).encode("nova-senha");
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando ID não existe")
        void updateProfile_idInexistente_lancaUserNotFound() {
            UUID id = UUID.randomUUID();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            dto.setName("Qualquer");

            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateProfile(id, dto));
        }

        @Test
        @DisplayName("Deve ignorar campos nulos — não altera nada")
        void updateProfile_camposNulos_naoAltera() {
            UUID id = tourist.getId();
            UpdateProfileDTO dto = new UpdateProfileDTO();
            // name, email, password todos null

            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateProfile(id, dto);

            assertEquals("Turista", tourist.getName());
            assertEquals("turista@test.com", tourist.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROMOTE TO GUIDE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("promoteToGuide()")
    class PromoteToGuideTests {

        @Test
        @DisplayName("Deve promover TOURIST para GUIDE com sucesso")
        void promoteToGuide_tourist_promovidoComSucesso() {
            UUID id = tourist.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(tourist));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponseDTO result = userService.promoteToGuide(id);

            assertEquals(Role.GUIDE, result.role());
            assertEquals(Role.GUIDE, tourist.getRole());
        }

        @Test
        @DisplayName("Deve lançar AlreadyGuideException quando já é GUIDE")
        void promoteToGuide_jaEhGuide_lancaAlreadyGuide() {
            UUID id = guide.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(guide));

            assertThrows(AlreadyGuideException.class, () -> userService.promoteToGuide(id));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar AdminRoleChangeException quando é ADMIN")
        void promoteToGuide_ehAdmin_lancaAdminRoleChange() {
            UUID id = admin.getId();
            when(userRepository.findById(id)).thenReturn(Optional.of(admin));

            assertThrows(AdminRoleChangeException.class, () -> userService.promoteToGuide(id));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando ID não existe")
        void promoteToGuide_idInexistente_lancaUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.promoteToGuide(id));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar usuário existente")
        void delete_idExistente_deletaComSucesso() {
            UUID id = tourist.getId();
            when(userRepository.existsById(id)).thenReturn(true);

            assertDoesNotThrow(() -> userService.delete(id));
            verify(userRepository).deleteById(id);
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando ID não existe")
        void delete_idInexistente_lancaUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.existsById(id)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.delete(id));
            verify(userRepository, never()).deleteById(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONVERT TO DTO
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("convertToDTO()")
    class ConvertToDTOTests {

        @Test
        @DisplayName("Deve converter User para UserResponseDTO corretamente")
        void convertToDTO_converteCorretamente() {
            UserResponseDTO dto = userService.convertToDTO(tourist);

            assertEquals(tourist.getId(), dto.id());
            assertEquals("Turista", dto.name());
            assertEquals(Role.TOURIST, dto.role());
        }
    }
}
