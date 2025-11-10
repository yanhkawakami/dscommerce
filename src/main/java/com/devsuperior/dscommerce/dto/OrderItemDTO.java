package com.devsuperior.dscommerce.dto;

import com.devsuperior.dscommerce.entities.OrderItem;

public class OrderItemDTO {
    private Long productId;
    private String name;
    private String imgUrl;
    private Double price;
    private Integer quantity;

    public OrderItemDTO(){}

    public OrderItemDTO(Long productId, String name, String imgUrl,Double price, Integer quantity) {
        this.productId = productId;
        this.name = name;
        this.imgUrl = imgUrl;
        this.price = price;
        this.quantity = quantity;
    }

    public OrderItemDTO(OrderItem entity) {
        productId = entity.getProduct().getId();
        name = entity.getProduct().getName();
        imgUrl = entity.getProduct().getImgUrl();
        price = entity.getPrice();
        quantity = entity.getQuantity();
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getSubtotal() {
        return quantity * price;
    }
}
