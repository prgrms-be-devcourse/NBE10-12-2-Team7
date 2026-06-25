package com.dongnemarket.favorite;

import com.dongnemarket.favorite.dto.FavoriteResponse;
import com.dongnemarket.favorite.entity.Favorite;
import com.dongnemarket.favorite.repository.FavoriteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    // 성공: 저장 후 존재 여부 및 DTO 변환
    @Test
    void 관심_저장_및_DTO_변환_성공() {
        Favorite saved = favoriteRepository.saveAndFlush(Favorite.of(1L, 100L));

        assertThat(favoriteRepository.existsByMemberIdAndProductId(1L, 100L)).isTrue();

        FavoriteResponse response = FavoriteResponse.from(saved);
        assertThat(response.getProductId()).isEqualTo(100L);
        assertThat(response.getId()).isEqualTo(saved.getId());
    }

    // 실패 1: 동일 (memberId, productId)를 중복 저장하면 UniqueConstraint 예외
    @Test
    void 중복_관심_저장시_예외발생() {
        favoriteRepository.saveAndFlush(Favorite.of(1L, 100L));

        assertThatThrownBy(() -> favoriteRepository.saveAndFlush(Favorite.of(1L, 100L)))
                .isInstanceOf(Exception.class);
    }

    // 실패 2: 존재하지 않는 (memberId, productId) 조회 시 빈 Optional 반환
    @Test
    void 존재하지_않는_관심_조회시_빈값_반환() {
        Optional<Favorite> result = favoriteRepository.findByMemberIdAndProductId(999L, 999L);

        assertThat(result).isEmpty();
    }
}
