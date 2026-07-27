package com.dongnemarket.product.init;

import java.util.ArrayList;
import java.util.List;

import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 기존 문자열 지역을 계층형 Region FK로 연결하는 일회성/멱등 백필 시더. */
@Component
public class ProductRegionBackfillSeeder implements DataSeeder {

	private final ProductRepository productRepository;
	private final RegionRepository regionRepository;

	public ProductRegionBackfillSeeder(ProductRepository productRepository, RegionRepository regionRepository) {
		this.productRepository = productRepository;
		this.regionRepository = regionRepository;
	}

	@Override
	public int order() {
		return 12;
	}

	@Override
	@Transactional
	public void seed() {
		List<Product> products = productRepository.findAllByRegionRefIsNull();
		List<BackfillFailure> failures = new ArrayList<>();
		for (Product product : products) {
			Region region = regionRepository.findByName(product.getRegion()).orElse(null);
			if (region == null) {
				failures.add(new BackfillFailure(product.getId(), product.getRegion(), "Region.name 매칭 실패"));
				continue;
			}
			product.backfillRegion(region);
		}

		if (!failures.isEmpty()) {
			throw new IllegalStateException("상품 지역 백필 실패: " + failures);
		}
	}

	private record BackfillFailure(Long productId, String region, String reason) {
	}
}
