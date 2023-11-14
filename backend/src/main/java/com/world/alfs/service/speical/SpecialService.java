package com.world.alfs.service.speical;

import com.world.alfs.common.exception.CustomException;
import com.world.alfs.common.exception.ErrorCode;
import com.world.alfs.controller.speical.response.GetSpecialListResponse;
import com.world.alfs.controller.speical.response.GetSpecialResponse;
import com.world.alfs.domain.allergy.Allergy;
import com.world.alfs.domain.ingredient.Ingredient;
import com.world.alfs.domain.manufacturing_allergy.repository.ManufacturingAllergyRepository;
import com.world.alfs.domain.member.Member;
import com.world.alfs.domain.member.repository.MemberRepository;
import com.world.alfs.domain.member_allergy.MemberAllergy;
import com.world.alfs.domain.member_allergy.repository.MemberAllergyRepository;
import com.world.alfs.domain.product.Product;
import com.world.alfs.domain.product.repository.ProductRepository;
import com.world.alfs.domain.product_img.ProductImg;
import com.world.alfs.domain.product_img.repostiory.ProductImgRepository;
import com.world.alfs.domain.product_ingredient.ProductIngredient;
import com.world.alfs.domain.product_ingredient.repostiory.ProductIngredientRepository;
import com.world.alfs.domain.special.Special;
import com.world.alfs.domain.special.repository.SpecialRepository;
import com.world.alfs.domain.supervisor.Supervisor;
import com.world.alfs.domain.supervisor.repository.SupervisorRepository;
import com.world.alfs.domain.wining.Wining;
import com.world.alfs.domain.wining.repository.WiningRepository;
import com.world.alfs.service.basket.BasketService;
import com.world.alfs.service.speical.dto.AddSpecialDto;
import com.world.alfs.service.speical.dto.AddSpecialQueueDto;
import com.world.alfs.service.speical.dto.DeleteSpecialDto;
import com.world.alfs.service.speical.dto.SetSpecialDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.world.alfs.common.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.world.alfs.common.exception.ErrorCode.SPECIAL_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class SpecialService {

    private final SpecialRepository specialRepository;
    private final ProductRepository productRepository;
    private final SupervisorRepository supervisorRepository;
    private final ProductImgRepository productImgRepository;
    private final MemberRepository memberRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final MemberAllergyRepository memberAllergyRepository;
    private final ManufacturingAllergyRepository manufacturingAllergyRepository;
    private final WiningRepository winingRepository;

    private final BasketService basketService;

    private final RedisTemplate<String, Object> redisTemplate;

    public Long addSpecial(AddSpecialDto dto) {
        Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        Supervisor supervisor = supervisorRepository.findById(dto.getSupervisorId()).orElseThrow(() -> new CustomException(ErrorCode.SUPERVISOR_NOT_FOUND));

        Optional<Special> existingSpecial = specialRepository.findById(dto.getProductId());
        if (existingSpecial.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_SPECIAL_ID);
        }

        Special special = dto.toEntity(product, supervisor);
        specialRepository.save(special);

        return special.getId();
    }

    public List<GetSpecialListResponse> getAllSpecial() {

        List<Special> specialList = specialRepository.findAll();

        return specialList.stream()
                .map(special -> {
                    ProductImg img = productImgRepository.findByProductId(special.getProduct().getId());
                    return special.toGetSpecialListResponse(img);
                })
                .collect(Collectors.toList());
    }

    public List<GetSpecialListResponse> getAllSpecial(Long memberId) {
        memberRepository.findById(memberId).filter(Member::getActivate)
                .orElseThrow(()->new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Special> specialList = specialRepository.findAll();
        List<GetSpecialListResponse> responseList = new ArrayList<>();

        for (int i = 0; i < specialList.size(); i++) {
            Special special = specialList.get(i);

            ProductImg img = productImgRepository.findByProductId(special.getProduct().getId());
            responseList.add(special.toGetSpecialListResponse(img));

            Product product = special.getProduct();

            // 알러지 및 기피 필터링
            Set<Integer> filterCode = new HashSet<>();

            List<Ingredient> productIngredientList = productIngredientRepository.findAllByProduct(product)
                    .stream().map(ProductIngredient::getIngredient).collect(Collectors.toList());

            List<Allergy> memberAllergyList = memberAllergyRepository.findByMemberId(memberId)
                    .stream().map(MemberAllergy::getAllergy)
                    .collect(Collectors.toList());

            for (Ingredient ingredient : productIngredientList) {
                for (Allergy allergy : memberAllergyList) {
                    if (ingredient.getName().equals(allergy.getAllergyName())) {
                        filterCode.add(allergy.getAllergyType());
                    }
                }
            }

            // 제조시설 알러지 필터링
            if (manufacturingAllergyRepository.findCountByProductAndAllergy(product.getId(), memberAllergyList.stream().map(memberAllergy -> memberAllergy.getId()).collect(Collectors.toList())) > 0){
                filterCode.add(2);
            }

            // 필터링된 게 없으면 안전
            if (filterCode.isEmpty()){
                filterCode.add(3);
            }

            responseList.get(i).setCode(filterCode);
        }

        return responseList;
    }

    public GetSpecialResponse getSpecial(Long id) {
        Special special = specialRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductImg img = productImgRepository.findByProductId(special.getProduct().getId());
        return special.toGetSpecialResponse(img);
    }

    public Long setSpecial(Long id, SetSpecialDto dto) {

        // 이벤트 특가상품이 존재하는지 확인
        Product product = productRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        Special special = specialRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 이벤트 특가상품의 관리자와 dto에 보내준 관리자 id와 일치하는지 비교
        Supervisor supervisor = supervisorRepository.findById(special.getSupervisor().getId()).orElseThrow(() -> new CustomException(ErrorCode.SUPERVISOR_NOT_FOUND));
        if (!special.getSupervisor().getId().equals(dto.getSupervisorId())) {
            throw new CustomException(ErrorCode.SUPERVISOR_ID_MISMATCH);
        }

        special.setSpecial(dto, product, supervisor);
        return special.getId();
    }

    public Long deleteSpecial(Long id, DeleteSpecialDto dto) {

        // 이벤트 특가상품이 존재하는지 확인
        Special special = specialRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // id에 해당하는 이벤트 특가상품의 관리자와 dto에 보내준 관리자 id와 일치하는지 비교
        if (!special.getSupervisor().getId().equals(dto.getSupervisorId())) {
            throw new CustomException(ErrorCode.SUPERVISOR_ID_MISMATCH);
        }

        specialRepository.deleteById(id);
        return id;
    }

    // 선착순 대기열에 추가
    public void addQueue(AddSpecialQueueDto dto) {
        double now = System.currentTimeMillis();

        redisTemplate.opsForZSet().addIfAbsent(dto.getProductId().toString(), dto.getMemberId(), now);
        log.debug("key: {} value: {} score: ({}초)", dto.getProductId(), dto.getMemberId(), now);

        Long waitingOrder = redisTemplate.opsForZSet().rank(dto.getProductId().toString(), dto.getMemberId());

    }

    // 대기 순서 가져오기
    public Long getWaitingOrder(Long productId, Object value) {
        return redisTemplate.opsForZSet().rank(productId.toString(), value);
    }

    // 대기열에서 삭제
    public Long remove(Long productId, Object value) {
        return redisTemplate.opsForZSet().remove(productId.toString(), value);
    }

    // 대기열에 있는 인원수
    public long geSize(Long productId) {
        return redisTemplate.opsForZSet().size(productId.toString());
    }

    // 장바구니에 담기
    public void addCart(Long productId, Long startRank, Long endRank) {

        Set<Object> queue = redisTemplate.opsForZSet().range(productId.toString(), startRank, endRank);
        List<Wining> list = new ArrayList<>();

        Special special = specialRepository.findById(productId)
                        .orElseThrow(() -> new CustomException(SPECIAL_NOT_FOUND));

        for (Object people : queue) {
            if (special.getCount() <= 0) {
                log.debug("재고가 소진되었습니다.");
            }
            Member member = memberRepository.findById(Long.parseLong(people.toString()))
                            .orElseThrow(() -> new CustomException(MEMBER_NOT_FOUND));

            // wining 테이블에 추가
            Wining wining = Wining.builder()
                    .special(special)
                    .member(member)
                    .build();

            list.add(wining);

            log.debug("memberId: {}, productId: {}", people, productId);
            redisTemplate.opsForZSet().remove(productId.toString(), people);
        }
        winingRepository.saveAll(list);

        // 특가 재고 감소
        special.changeCount(queue.size());
    }

}
