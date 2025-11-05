package com.devsuperior.dscommerce.services;

import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.dto.ProductMinDTO;
import com.devsuperior.dscommerce.entities.Product;
import com.devsuperior.dscommerce.repositories.ProductRepository;
import com.devsuperior.dscommerce.services.exceptions.DatabaseException;
import com.devsuperior.dscommerce.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Transactional(readOnly = true) // Usamos isso para não dar lock no banco de dados
    // O Service precisa retornar DTO, pois ele devolve um DTO para o Controller e não uma entidade
    public ProductDTO findById(Long id) {
        Optional<Product> result = repository.findById(id);
        Product product = result.orElseThrow(
                () -> new ResourceNotFoundException("Recurso com ID " + id + " não encontrado"));
        return new ProductDTO(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductMinDTO> findAll (String name, Pageable pageable) {
        Page<Product> result = repository.searchByName(name, pageable);
        return result.map(ProductMinDTO::new);
    }

    @Transactional
    public ProductDTO insert(ProductDTO productDTO) {
            Product entity = new Product();
            copyDtoToEntity(productDTO, entity);
            entity = repository.save(entity);
            return new ProductDTO(entity);
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO productDTO) {
        try {
            // Essa operação não vai ao banco de dados, ele prepara um objeto, ainda é um objeto monitorado pela JPA
            Product entity = repository.getReferenceById(id);
            copyDtoToEntity(productDTO, entity);
            entity = repository.save(entity);
            return new ProductDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Recurso com ID " + id + " não encontrado");
        }
    }

    // Como essa exceção vem do banco de dados, precisamos colocar o propagation para que execute este método Transactional
    // caso esteja no contexto de outra transação. Caso contrário, não envolve.
    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Recurso com ID " + id + " não encontrado");
        }
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // A exception do catch precisa ser a do framework e não da dependência
            throw new DatabaseException("Falha de integridade referencial");
        }
    }

    private void copyDtoToEntity(ProductDTO productDTO, Product entity) {
        entity.setName(productDTO.getName());
        entity.setDescription(productDTO.getDescription());
        entity.setPrice(productDTO.getPrice());
        entity.setImgUrl(productDTO.getImgUrl());
    }


}

