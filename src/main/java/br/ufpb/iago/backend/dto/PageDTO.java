package br.ufpb.iago.backend.dto;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

public record PageDTO<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int currentPage,
        boolean last
) implements Serializable {
    public PageDTO(Page<T> page) {
        this(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.getNumber(), page.isLast());
    }
}
