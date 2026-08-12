package br.ufpb.iago.backend.dto;

import br.ufpb.iago.backend.model.Attraction;
import br.ufpb.iago.backend.model.Review;
import br.ufpb.iago.backend.model.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

    public class ReviewRequestDTO {

        @NotNull(message = "O ID da atração é obrigatório")
        private UUID attractionId;

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota mínima é 1")
        @Max(value = 5, message = "A nota máxima é 5")
        private Integer rating;

        @Size(max = 500, message = "O comentário não pode ter mais de 500 caracteres")
        private String comment;

        public UUID getAttractionId() {
            return attractionId;
        }

        public void setAttractionId(UUID attractionId) {
            this.attractionId = attractionId;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

