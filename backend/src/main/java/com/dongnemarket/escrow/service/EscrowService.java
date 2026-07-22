package com.dongnemarket.escrow.service;

import com.dongnemarket.escrow.dto.EscrowCreateRequest;
import com.dongnemarket.escrow.dto.EscrowResponse;
import com.dongnemarket.escrow.entity.Escrow;
import com.dongnemarket.escrow.entity.EscrowStatus;
import com.dongnemarket.escrow.repository.EscrowRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public EscrowService(EscrowRepository escrowRepository,
                         ProductRepository productRepository,
                         MemberRepository memberRepository) {
        this.escrowRepository = escrowRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    /** 거래 시작: 검증 → 상품가 스냅샷으로 예치(IN_ESCROW) → 상품을 거래중(RESERVED)으로. */
    @Transactional
    public EscrowResponse create(Long buyerId, EscrowCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ErrorCode.DELETED_PRODUCT);
        }
        if (product.isHidden()) {
            throw new BusinessException(ErrorCode.HIDDEN_PRODUCT);
        }

        Member seller = product.getMember();
        if (seller.getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.CANNOT_ESCROW_OWN_PRODUCT);
        }
        if (product.getTradeStatus() != TradeStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (escrowRepository.existsByProductIdAndStatus(product.getId(), EscrowStatus.IN_ESCROW)) {
            throw new BusinessException(ErrorCode.ESCROW_ALREADY_EXISTS);
        }

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Escrow escrow = Escrow.create(product, buyer, seller, product.getPrice());
        Escrow saved = escrowRepository.save(escrow);
        product.changeTradeStatus(TradeStatus.RESERVED);

        return EscrowResponse.from(saved);
    }

    /** 거래 조회. */
    public EscrowResponse get(Long escrowId) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ESCROW_NOT_FOUND));
        return EscrowResponse.from(escrow);
    }
}
