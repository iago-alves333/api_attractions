package br.ufpb.iago.backend.integration;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.RefreshToken;
import br.ufpb.iago.backend.model.Reservation;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.Role;
import br.ufpb.iago.backend.model.Status;
import br.ufpb.iago.backend.model.User;
import br.ufpb.iago.backend.repository.AttractionRepository;
import br.ufpb.iago.backend.repository.RefreshTokenRepository;
import br.ufpb.iago.backend.repository.ReservationRepository;
import br.ufpb.iago.backend.repository.ReviewRepository;
import br.ufpb.iago.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgisIntegrationTest {

    static {
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
    }

    private static final DockerImageName POSTGIS_IMAGE = DockerImageName.parse("postgis/postgis:15-3.3")
            .asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("titureco_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AttractionRepository attractionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private User testGuide;
    private User testTourist;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        reviewRepository.deleteAll();
        reservationRepository.deleteAll();
        attractionRepository.deleteAll();
        userRepository.deleteAll();

        testGuide = new User();
        testGuide.setName("Guia Paraibano");
        testGuide.setEmail("guia@titureco.com.br");
        testGuide.setPassword("$2a$10$hashedpasswordforexampleguide");
        testGuide.setRole(Role.GUIDE);
        testGuide = userRepository.save(testGuide);

        testTourist = new User();
        testTourist.setName("Turista Viajante");
        testTourist.setEmail("turista@titureco.com.br");
        testTourist.setPassword("$2a$10$hashedpasswordforexampletourist");
        testTourist.setRole(Role.TOURIST);
        testTourist = userRepository.save(testTourist);
    }

    private Attraction createAttraction(String title, double latitude, double longitude, BigDecimal price, int spots) {
        Attraction attraction = new Attraction();
        attraction.setTitle(title);
        attraction.setDescription("Descrição do passeio " + title);
        attraction.setGuide(testGuide);
        attraction.setPrice(price);
        attraction.setAvailableSpots(spots);
        attraction.setRatingAverage(0.0);
        attraction.setReviewCount(0);

        // Longitude = X, Latitude = Y no plano cartesiano espacial
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        attraction.setLocation(point);

        return attractionRepository.save(attraction);
    }

    @Nested
    @DisplayName("PostGIS Spatial Query Tests (ST_DWithin & ST_Distance)")
    class SpatialQueryTests {

        @Test
        @DisplayName("findNearby deve encontrar apenas atrações dentro do raio especificado em metros")
        void findNearbyWithinRadius() {
            // Farol do Cabo Branco em João Pessoa (-7.1491, -34.7977)
            Attraction farol = createAttraction(
                    "Farol do Cabo Branco",
                    -7.1491,
                    -34.7977,
                    BigDecimal.ZERO,
                    50
            );

            // Açude Velho em Campina Grande (-7.2247, -35.8816) ~ 120km de distância
            Attraction acudeVelho = createAttraction(
                    "Açude Velho",
                    -7.2247,
                    -35.8816,
                    new BigDecimal("25.00"),
                    30
            );

            // Busca centrada no Farol com raio de 10.000 metros (10 km)
            Page<Attraction> nearby10km = attractionRepository.findNearby(
                    -7.1491,
                    -34.7977,
                    10000.0,
                    PageRequest.of(0, 10)
            );

            assertThat(nearby10km.getContent()).hasSize(1);
            assertThat(nearby10km.getContent().get(0).getTitle()).isEqualTo("Farol do Cabo Branco");

            // Busca com raio de 150.000 metros (150 km) - deve incluir os dois, ordenados pela menor distância
            Page<Attraction> nearby150km = attractionRepository.findNearby(
                    -7.1491,
                    -34.7977,
                    150000.0,
                    PageRequest.of(0, 10)
            );

            assertThat(nearby150km.getContent()).hasSize(2);
            assertThat(nearby150km.getContent().get(0).getTitle()).isEqualTo("Farol do Cabo Branco");
            assertThat(nearby150km.getContent().get(1).getTitle()).isEqualTo("Açude Velho");
        }
    }

    @Nested
    @DisplayName("Flyway V5 Partial Unique Constraint Tests")
    class ConstraintTests {

        @Test
        @DisplayName("Impede duas reservas com status PENDING para o mesmo turista e mesma atração (idx_reservations_user_attraction_pending)")
        void preventDuplicatePendingReservation() {
            Attraction farol = createAttraction(
                    "Farol do Cabo Branco",
                    -7.1491,
                    -34.7977,
                    BigDecimal.ZERO,
                    50
            );

            // Primeira reserva PENDING
            Reservation first = new Reservation();
            first.setTourist(testTourist);
            first.setAttraction(farol);
            first.setStatus(Status.PENDING);
            first.setReservedFor(LocalDateTime.now().plusDays(2));
            reservationRepository.saveAndFlush(first);

            // Segunda reserva PENDING para o mesmo turista e atração deve violar o índice parcial único
            Reservation second = new Reservation();
            second.setTourist(testTourist);
            second.setAttraction(farol);
            second.setStatus(Status.PENDING);
            second.setReservedFor(LocalDateTime.now().plusDays(5));

            assertThatThrownBy(() -> reservationRepository.saveAndFlush(second))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Permite nova reserva PENDING se a reserva anterior foi CANCELLED")
        void allowPendingReservationWhenPreviousCancelled() {
            Attraction farol = createAttraction(
                    "Farol do Cabo Branco",
                    -7.1491,
                    -34.7977,
                    BigDecimal.ZERO,
                    50
            );

            // Reserva cancelada
            Reservation cancelled = new Reservation();
            cancelled.setTourist(testTourist);
            cancelled.setAttraction(farol);
            cancelled.setStatus(Status.CANCELLED);
            cancelled.setReservedFor(LocalDateTime.now().plusDays(1));
            reservationRepository.saveAndFlush(cancelled);

            // Nova reserva PENDING permitida pelo índice parcial
            Reservation newPending = new Reservation();
            newPending.setTourist(testTourist);
            newPending.setAttraction(farol);
            newPending.setStatus(Status.PENDING);
            newPending.setReservedFor(LocalDateTime.now().plusDays(3));

            Reservation saved = reservationRepository.saveAndFlush(newPending);
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        }
    }

    @Nested
    @DisplayName("EntityGraph Eager Loading Tests (Anti-N+1)")
    class EntityGraphTests {

        @Test
        @DisplayName("Consultas paginadas carregam associações LAZY em uma query via @EntityGraph")
        void entityGraphsLoadAssociationsEagerly() {
            Attraction farol = createAttraction(
                    "Farol do Cabo Branco",
                    -7.1491,
                    -34.7977,
                    BigDecimal.TEN,
                    20
            );

            Reservation reservation = new Reservation();
            reservation.setTourist(testTourist);
            reservation.setAttraction(farol);
            reservation.setStatus(Status.COMPLETED);
            reservation.setReservedFor(LocalDateTime.now().plusDays(1));
            reservationRepository.saveAndFlush(reservation);

            Review review = new Review();
            review.setTourist(testTourist);
            review.setAttraction(farol);
            review.setRating(5);
            review.setComment("Excelente!");
            reviewRepository.saveAndFlush(review);

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken("sha256-sample-hash");
            refreshToken.setUser(testTourist);
            refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
            refreshTokenRepository.saveAndFlush(refreshToken);

            entityManager.flush();
            entityManager.clear();

            var persistenceUtil = Persistence.getPersistenceUtil();

            Attraction fetchedAttraction = attractionRepository.findAll(PageRequest.of(0, 10)).getContent().get(0);
            assertThat(persistenceUtil.isLoaded(fetchedAttraction, "guide")).isTrue();
            assertThat(fetchedAttraction.getGuide().getName()).isEqualTo("Guia Paraibano");

            Reservation fetchedReservation = reservationRepository.findAllByTouristId(
                    testTourist.getId(), PageRequest.of(0, 10)
            ).getContent().get(0);
            assertThat(persistenceUtil.isLoaded(fetchedReservation, "tourist")).isTrue();
            assertThat(persistenceUtil.isLoaded(fetchedReservation, "attraction")).isTrue();

            Review fetchedReview = reviewRepository.findAllByAttractionId(
                    farol.getId(), PageRequest.of(0, 10)
            ).getContent().get(0);
            assertThat(persistenceUtil.isLoaded(fetchedReview, "tourist")).isTrue();
            assertThat(fetchedReview.getTourist().getName()).isEqualTo("Turista Viajante");

            RefreshToken fetchedToken = refreshTokenRepository.findByToken("sha256-sample-hash").orElseThrow();
            assertThat(persistenceUtil.isLoaded(fetchedToken, "user")).isTrue();
            assertThat(fetchedToken.getUser().getEmail()).isEqualTo("turista@titureco.com.br");
        }
    }

    @Nested
    @DisplayName("Guide Filter, Linked Records & Pessimistic Lock Tests")
    class GuideFilterAndLockTests {

        @Test
        @DisplayName("findAllByGuideId retorna apenas atrações do guia informado com guide carregado")
        void findAllByGuideIdFiltersAndLoadsGuide() {
            User otherGuide = new User();
            otherGuide.setName("Outro Guia");
            otherGuide.setEmail("outroguia@titureco.com.br");
            otherGuide.setPassword("$2a$10$hashedpasswordforexampleguide2");
            otherGuide.setRole(Role.GUIDE);
            otherGuide = userRepository.save(otherGuide);

            createAttraction("Passeio Guia Principal", -7.1491, -34.7977, BigDecimal.TEN, 15);

            Attraction otherAttraction = new Attraction();
            otherAttraction.setTitle("Passeio Outro Guia");
            otherAttraction.setDescription("Outro passeio");
            otherAttraction.setGuide(otherGuide);
            otherAttraction.setPrice(BigDecimal.TEN);
            otherAttraction.setAvailableSpots(10);
            otherAttraction.setRatingAverage(0.0);
            otherAttraction.setReviewCount(0);
            otherAttraction.setLocation(geometryFactory.createPoint(new Coordinate(-34.8, -7.15)));
            attractionRepository.saveAndFlush(otherAttraction);

            entityManager.clear();

            Page<Attraction> filtered = attractionRepository.findAllByGuideId(testGuide.getId(), PageRequest.of(0, 10));
            assertThat(filtered.getContent()).hasSize(1);
            assertThat(filtered.getContent().get(0).getTitle()).isEqualTo("Passeio Guia Principal");
            assertThat(Persistence.getPersistenceUtil().isLoaded(filtered.getContent().get(0), "guide")).isTrue();
        }

        @Test
        @DisplayName("hasLinkedRecords detecta reservas ou avaliações vinculadas e findByIdWithLock bloqueia a linha")
        void hasLinkedRecordsAndPessimisticLockWorkAgainstPostgres() {
            Attraction semVinculos = createAttraction("Atração Livre", -7.1491, -34.7977, BigDecimal.TEN, 20);
            Attraction comReserva = createAttraction("Atração Reservada", -7.1492, -34.7978, BigDecimal.TEN, 20);
            Attraction comAvaliacao = createAttraction("Atração Avaliada", -7.1493, -34.7979, BigDecimal.TEN, 20);

            Reservation reservation = new Reservation();
            reservation.setTourist(testTourist);
            reservation.setAttraction(comReserva);
            reservation.setStatus(Status.PENDING);
            reservation.setReservedFor(LocalDateTime.now().plusDays(2));
            reservationRepository.saveAndFlush(reservation);

            Review review = new Review();
            review.setTourist(testTourist);
            review.setAttraction(comAvaliacao);
            review.setRating(5);
            review.setComment("Ótima experiência");
            reviewRepository.saveAndFlush(review);

            entityManager.flush();

            assertThat(attractionRepository.hasLinkedRecords(semVinculos.getId())).isFalse();
            assertThat(attractionRepository.hasLinkedRecords(comReserva.getId())).isTrue();
            assertThat(attractionRepository.hasLinkedRecords(comAvaliacao.getId())).isTrue();

            Attraction locked = attractionRepository.findByIdWithLock(semVinculos.getId()).orElseThrow();
            assertThat(locked.getId()).isEqualTo(semVinculos.getId());

            Page<Attraction> search = attractionRepository.searchByKeywordAndLocation(
                    "Livre", -7.1491, -34.7977, 5000.0, PageRequest.of(0, 10)
            );
            assertThat(search.getContent()).hasSize(1);
            assertThat(search.getContent().get(0).getId()).isEqualTo(semVinculos.getId());
        }
    }
}
