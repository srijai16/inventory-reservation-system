package com.example.reservation.controller;

import com.example.reservation.dto.ProductResponse;
import com.example.reservation.dto.WarehouseStockDto;
import com.example.reservation.model.Product;
import com.example.reservation.repository.ProductRepository;
import com.example.reservation.service.ReservationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ReservationService reservationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductController(ProductRepository productRepository,
                             ReservationService reservationService,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.reservationService = reservationService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        // First release expired reservations
        reservationService.releaseExpiredReservations();

        String cacheKey = "products:list";
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, new TypeReference<List<ProductResponse>>() {});
            } catch (Exception e) {
                // In case of deserialization failure, fall through to database query
            }
        }

        List<Product> products = productRepository.findAllWithStocksOrderByCreatedAtDesc();

        List<ProductResponse> data = products.stream().map(product -> {
            ProductResponse dto = new ProductResponse();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setPrice(product.getPrice());
            dto.setDescription(product.getDescription());
            dto.setImageUrl(product.getImageUrl());
            dto.setWarehouses(product.getStocks().stream().map(stock -> {
                WarehouseStockDto ws = new WarehouseStockDto();
                ws.setWarehouseId(stock.getWarehouse().getId());
                ws.setWarehouseName(stock.getWarehouse().getName());
                ws.setLocation(stock.getWarehouse().getLocation());
                ws.setTotalUnits(stock.getTotalUnits());
                ws.setReservedUnits(stock.getReservedUnits());
                ws.setAvailableUnits(stock.getTotalUnits() - stock.getReservedUnits());
                return ws;
            }).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());

        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Log writing cache failure
        }

        return data;
    }
}
