package com.dongnemarket.manner.repository;

import com.dongnemarket.manner.entity.MannerScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MannerScoreRepository extends JpaRepository<MannerScore, Long> {

    Optional<MannerScore> findByMember_Id(Long memberId);

    /** 관리자 저신뢰 회원 모니터링: 온도가 threshold 이하인 회원을 낮은 순으로 조회한다. */
    @Query("SELECT ms FROM MannerScore ms WHERE ms.score <= :threshold ORDER BY ms.score ASC")
    List<MannerScore> findAllByScoreLessThanEqualOrderByScoreAsc(@Param("threshold") BigDecimal threshold);

    /** 회복 배치 대상: 아직 최대치(기본값)에 도달하지 못한 회원만 골라 불필요한 순회를 줄인다. */
    List<MannerScore> findAllByScoreLessThan(BigDecimal score);
}
