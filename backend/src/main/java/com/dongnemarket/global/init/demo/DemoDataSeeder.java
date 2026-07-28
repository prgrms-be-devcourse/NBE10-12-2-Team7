package com.dongnemarket.global.init.demo;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.comment.entity.Comment;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.init.DataSeeder;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.entity.MemberStatus;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.entity.TradeStatus;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import com.dongnemarket.report.entity.Report;
import com.dongnemarket.report.entity.ReportReason;
import com.dongnemarket.report.entity.ReportStatus;
import com.dongnemarket.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [개발/검증 전용] 관리자 콘솔 확인용 더미 데이터 시더.
 * 실행 조건: app.seed.demo=true 이면서 test 프로파일이 아닐 때만.
 * SeedOrchestrator 가 마스터/부트스트랩 시더(order 10~20) 커밋 이후(order 30) 호출하므로
 * 카테고리 존재가 보장된다. 멱등 가드로 재실행 시 중복 시딩을 막는다.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoDataSeeder implements DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String SENTINEL_EMAIL = "user01@dongnemarket.com";

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final RegionRepository regionRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(MemberRepository memberRepository,
                          CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          RegionRepository regionRepository,
                          CommentRepository commentRepository,
                          ReportRepository reportRepository,
                          PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.regionRepository = regionRepository;
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    @Transactional
    public void seed() {
        // 멱등 가드: 이미 시드돼 있으면 아무것도 하지 않는다.
        if (memberRepository.existsByEmail(SENTINEL_EMAIL)) {
            return;
        }

        // ── 2단계: 회원 6명 ──────────────────────────────
        String userPw = passwordEncoder.encode("user1234!");
        String adminPw = passwordEncoder.encode("admin1234!");

        Member user01 = memberRepository.save(Member.createUser("user01@dongnemarket.com", userPw, "상민"));
        Member user02 = memberRepository.save(Member.createUser("user02@dongnemarket.com", userPw, "지훈"));
        Member user03 = memberRepository.save(Member.createUser("user03@dongnemarket.com", userPw, "민서"));

        Member user04 = memberRepository.save(Member.createUser("user04@dongnemarket.com", userPw, "철수"));
        user04.changeStatus(MemberStatus.SUSPENDED);   // 정지

        Member user05 = memberRepository.save(Member.createUser("user05@dongnemarket.com", userPw, "영희"));
        user05.changeStatus(MemberStatus.DELETED);     // 소프트삭제(deletedAt 자동)

        memberRepository.save(Member.createAdmin("admin2@dongnemarket.com", adminPw, "부관리자"));

        // ── 3단계: 상품 6건 (기존 카테고리 8종을 이름으로 재사용) ──
        Map<String, Category> cat = categoryRepository.findAllByOrderByIdAsc().stream()
                .collect(Collectors.toMap(Category::getName, c -> c));

        Product p1 = productRepository.save(Product.create(
                user03, cat.get("디지털기기"), "아이폰 13 128GB", "생활기스 있으나 정상 작동합니다.",
                BigDecimal.valueOf(450000), requiredRegion("1168010100")));
        addViews(p1, 152);

        Product p2 = productRepository.save(Product.create(
                user01, cat.get("가구/인테리어"), "원목 책상 의자", "1년 사용, 상태 양호.",
                BigDecimal.valueOf(60000), requiredRegion("1144012400")));
        p2.changeTradeStatus(TradeStatus.RESERVED);
        addViews(p2, 43);

        Product p3 = productRepository.save(Product.create(
                user03, cat.get("디지털기기"), "에어팟 프로 2세대", "정품, 구성품 모두 포함.",
                BigDecimal.valueOf(180000), requiredRegion("1168010100")));
        p3.complete();
        addViews(p3, 88);

        Product p4 = productRepository.save(Product.create(
                user01, cat.get("의류"), "겨울 패딩 (L)", "따뜻한 롱패딩입니다.",
                BigDecimal.valueOf(90000), requiredRegion("1144012400")));
        p4.hide();
        addViews(p4, 12);

        Product p5 = productRepository.save(Product.create(
                user04, cat.get("스포츠/레저"), "캠핑 텐트 4인용", "방수 우수, 몇 회 사용.",
                BigDecimal.valueOf(120000), requiredRegion("4113511400")));
        p5.softDelete();
        addViews(p5, 5);

        Product p6 = productRepository.save(Product.create(
                user02, cat.get("반려동물용품"), "강아지 사료 5kg", "미개봉 새 제품.",
                BigDecimal.valueOf(35000), requiredRegion("1171010100")));
        addViews(p6, 27);

        // ── 4단계: 댓글 5건 (정상 + 삭제) ──────────────
        commentRepository.save(Comment.of(user02, p1, "관심있어요! 네고 가능한가요?"));
        commentRepository.save(Comment.of(user01, p1, "직거래 가능합니다"));
        commentRepository.save(Comment.of(user02, p3, "상태 좋네요, 잘 쓸게요"));
        Comment c4 = commentRepository.save(Comment.of(user03, p4, "부적절 내용 예시"));
        c4.softDelete();
        commentRepository.save(Comment.of(user01, p6, "우리 강아지가 잘 먹어요"));

        // ── 4단계: 신고 5건 (상태 4종 + 유형 2종) ───────
        reportRepository.save(Report.ofProduct(
                user02, p1, ReportReason.FRAUD_SUSPECTED, "사기 의심됩니다"));       // RECEIVED

        Report r2 = reportRepository.save(Report.ofProduct(
                user01, p4, ReportReason.PROHIBITED_ITEM, "금지 품목 같아요"));
        r2.changeStatus(ReportStatus.REVIEWING);

        Report r3 = reportRepository.save(Report.ofMember(
                user03, user04, ReportReason.INAPPROPRIATE_CONTENT, "부적절한 언행"));
        r3.changeStatus(ReportStatus.COMPLETED);

        Report r4 = reportRepository.save(Report.ofProduct(
                user02, p3, ReportReason.FAKE_ITEM, "가품 의심"));
        r4.changeStatus(ReportStatus.REJECTED);

        reportRepository.save(Report.ofMember(
                user01, user02, ReportReason.ETC, "기타 신고"));                    // RECEIVED

        log.info("[DemoData] seeded: members=6, products=6, comments=5, reports=5");
    }

    /** viewCount 세터가 없어 증가 메서드를 반복 호출(검증용, 소량). */
    private void addViews(Product product, int count) {
        for (int i = 0; i < count; i++) {
            product.increaseViewCount();
        }
    }

    private Region requiredRegion(String code) {
        return regionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("데모 데이터 지역을 찾을 수 없습니다: " + code));
    }
}
