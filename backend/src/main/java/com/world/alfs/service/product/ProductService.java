package com.world.alfs.service.product;


import com.world.alfs.common.exception.CustomException;
import com.world.alfs.controller.ApiResponse;
import com.world.alfs.controller.product.response.GetProductListResponse;
import com.world.alfs.controller.product.response.ProductResponse;
import com.world.alfs.domain.product.Product;
import com.world.alfs.domain.product.repository.ProductRepository;
import com.world.alfs.domain.product_img.ProductImg;
import com.world.alfs.domain.product_img.repostiory.ProductImgRepository;
import com.world.alfs.service.product.dto.AddProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.world.alfs.common.exception.ErrorCode.PRODUCT_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImgRepository productImgRepository;

    public Long addProduct(AddProductDto dto) {
        Product product = dto.toEntity();
        ProductImg productImg = dto.toImgEntity();
        Product savedProduct = productRepository.save(product);
        productImgRepository.save(productImg);
        return savedProduct.getId();
    }

    public Optional<ProductResponse> getProduct(Long id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(PRODUCT_NOT_FOUND));

        ProductImg img = productImgRepository.findByProductId(product.getId());
        ProductResponse response = product.toResponse(img);
        return Optional.ofNullable(response);
    }

    public Long setProduct(AddProductDto dto) {
        Optional<Product> product = productRepository.findById(dto.getId());
        product.get().setProduct(dto);
        return product.get().getId();
    }

    public List<GetProductListResponse> getAllProduct() {
        List<Product> productList = productRepository.findAll();
        List<GetProductListResponse> productResponseList = new ArrayList<>();
        for(int i=0; i<productList.size(); i++){
            ProductImg img = productImgRepository.findByProductId(productList.get(i).getId());
            GetProductListResponse getProductListResponse = GetProductListResponse.builder()
                    .id(productList.get(i).getId())
                    .title(productList.get(i).getTitle())
                    .name(productList.get(i).getName())
                    .price(productList.get(i).getPrice())
                    .sale(productList.get(i).getSale())
                    .img(img.getImg_1())
                    .build();
            productResponseList.add(getProductListResponse);
        }
        return productResponseList;
    }

    public Long deleteProduct(Long id) {
        productImgRepository.deleteById(id);
        return id;

    }
}