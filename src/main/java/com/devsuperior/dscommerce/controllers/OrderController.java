package com.devsuperior.dscommerce.controllers;

// Disponibiliza os recursos da aplicação.
// Um recurso é implementado pelo Controller na API REST.

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.dto.ProductMinDTO;
import com.devsuperior.dscommerce.entities.Order;
import com.devsuperior.dscommerce.services.OrderService;
import com.devsuperior.dscommerce.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping (value = "/orders")
public class OrderController {

    @Autowired
    private OrderService service;

    @GetMapping (value = "/{id}")
    public ResponseEntity<OrderDTO> findById(@PathVariable Long id) {
        OrderDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT')")
    @PostMapping
    // O @Valid é a notação do Jakarta que sinaliza que há uma validação a ser feita no DTO
    public ResponseEntity<OrderDTO>insert(@Valid @RequestBody OrderDTO orderDto){
        OrderDTO dto = service.insert(orderDto);
        // A URI é o link para o recurso criado, retornando o LOCATION nos HEADERS
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(dto.getId()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }
}
