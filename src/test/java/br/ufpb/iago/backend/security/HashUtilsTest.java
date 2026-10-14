package br.ufpb.iago.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    @DisplayName("sha256 deve retornar 64 caracteres hexadecimais")
    void sha256_returnsExpectedHexLength() {
        String hash = HashUtils.sha256("test-token");
        assertThat(hash).isNotNull().hasSize(64);
        assertThat(hash).matches("^[a-f0-9]{64}$");
    }

    @Test
    @DisplayName("sha256 deve ser determinístico")
    void sha256_isDeterministic() {
        String hash1 = HashUtils.sha256("secret");
        String hash2 = HashUtils.sha256("secret");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("sha256 com input nulo deve retornar null")
    void sha256_nullInput_returnsNull() {
        assertThat(HashUtils.sha256(null)).isNull();
    }
}
