package com.world.alfs.service.basket.dto;

import com.world.alfs.domain.basket.Basket;
import com.world.alfs.domain.member.Member;
import com.world.alfs.domain.product.Product;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddBasketDto {
    private Member member;
    private Product product;
    private int count;
    private int status;
    private Boolean isBigSale;

    @Builder
    public AddBasketDto(Member member, Product product, int count, int status, Boolean isBigSale) {
        this.member = member;
        this.product = product;
        this.count = count;
        this.status = status;
        this.isBigSale = isBigSale;
    }

    public Basket toEntity(){
        return Basket.builder()
                .member(member)
                .product(product)
                .status(status)
                .count(count)
                .purchase(null)
                .purchaseDate(null)
                .isBigSale(isBigSale)
                .build();
    }
}
