package com.devsuperior.dscommerce.controllers;

// Disponibiliza os recursos da aplicação.
// Um recurso é implementado pelo Controller na API REST.

import com.devsuperior.dscommerce.controllers.handlers.ControllerExceptionHandler;
import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping (value = "/products")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping (value = "/{id}")
    public ResponseEntity<ProductDTO> findById(@PathVariable Long id) {
        ProductDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> findAll(
            @RequestParam (name = "name", defaultValue = "") String name,
            Pageable pageable) {
        Page<ProductDTO> dto = service.findAll(name, pageable);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    // O @Valid é a notação do Jakarta que sinaliza que há uma validação a ser feita no DTO
    public ResponseEntity<ProductDTO>insert(@Valid @RequestBody ProductDTO productDTO){
        ProductDTO dto = service.insert(productDTO);
        // A URI é o link para o recurso criado, retornando o LOCATION nos HEADERS
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(dto.getId()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @PutMapping (value = "/{id}")
    // O @Valid é a notação do Jakarta que sinaliza que há uma validação a ser feita no DTO
    public ResponseEntity<ProductDTO> update(@PathVariable Long id, @Valid @RequestBody ProductDTO productDTO){
        ProductDTO dto = service.update(id, productDTO);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping (value = "/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
