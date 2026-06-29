package com.dongnemarket.admin.service;

import com.dongnemarket.admin.dto.AdminProductResponse;
import com.dongnemarket.admin.repository.AdminProductRepository;
import com.dongnemarket.global.exception.BusinessException;
import com.dongnemarket.global.exception.ErrorCode;
import com.dongnemarket.product.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminProductService {

    private final AdminProductRepository adminProductRepository;

    public AdminProductService(AdminProductRepository adminProductRepository) {
        this.adminProductRepository = adminProductRepository;
    }

    /** 전체 상품 목록 (숨김·삭제 무관) */
    public List<AdminProductResponse> getProducts() {
        return adminProductRepository.findAll().stream()
                .map(AdminProductResponse::from)
                .toList();
    }

    /** 상품 단건 상세 */
    public AdminProductResponse getProduct(Long productId) {
        Product product = adminProductRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return AdminProductResponse.from(product);
    }

    /** 상품 숨김 처리 (관리자는 작성자가 아니어도 가능, 한방향) */
    @Transactional
    public void hideProduct(Long productId) {
        Product product = adminProductRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.hide();
    }

    /** 상품 소프트 삭제 (관리자) */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = adminProductRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.softDelete();
    }
}