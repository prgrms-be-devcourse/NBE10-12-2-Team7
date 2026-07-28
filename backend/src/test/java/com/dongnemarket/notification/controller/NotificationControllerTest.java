package com.dongnemarket.notification.controller;

import java.math.BigDecimal;

import com.dongnemarket.category.entity.Category;
import com.dongnemarket.category.repository.CategoryRepository;
import com.dongnemarket.chat.entity.ChatMessage;
import com.dongnemarket.chat.entity.ChatRoom;
import com.dongnemarket.chat.repository.ChatMessageRepository;
import com.dongnemarket.chat.repository.ChatRoomRepository;
import com.dongnemarket.comment.repository.CommentRepository;
import com.dongnemarket.global.security.jwt.JwtTokenProvider;
import com.dongnemarket.member.entity.Member;
import com.dongnemarket.member.repository.MemberRepository;
import com.dongnemarket.notification.repository.NotificationRepository;
import com.dongnemarket.product.entity.Product;
import com.dongnemarket.product.repository.ProductRepository;
import com.dongnemarket.region.entity.Region;
import com.dongnemarket.region.repository.RegionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 알림 API 통합 테스트.
 * <p>실제 HTTP 요청으로 사용자 유스케이스(성공·실패·엣지)를 검증한다.
 * 댓글 작성 → 알림 생성은 {@code AFTER_COMMIT} 리스너로 이어지는데, MockMvc 요청은 <b>실제 커밋</b>되므로
 * (테스트 클래스에 {@code @Transactional}을 걸지 않는다) 리스너가 발동해 알림이 저장된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("알림 API 통합 테스트")
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    private Long productId;
    private Long categoryId;
    private Member seller;
    private Member buyer;
    private Product product;
    /** 상품 소유자 = 알림 수신자 */
    private String sellerToken;
    /** 댓글 작성자(구매자) = 알림 유발자 */
    private String buyerToken;

    @BeforeEach
    void setUp() {
        seller = memberRepository.save(Member.createUser("seller@example.com", "encoded-pw", "seller"));
        buyer = memberRepository.save(Member.createUser("buyer@example.com", "encoded-pw", "buyer"));
        // 시드된 기본 카테고리(CategorySeeder)와 이름이 겹치지 않도록 테스트 전용 카테고리를 만든다.
        Category category = categoryRepository.save(new Category("알림테스트전용카테고리"));
        product = productRepository.save(
                Product.create(seller, category, "맥북 프로", "상태 좋음", BigDecimal.valueOf(1_500_000), findRegion("1168010100")));

        categoryId = category.getId();
        productId = product.getId();
        sellerToken = "Bearer " + jwtTokenProvider.createAccessToken(seller.getId(), "ROLE_USER");
        buyerToken = "Bearer " + jwtTokenProvider.createAccessToken(buyer.getId(), "ROLE_USER");
    }

    private Region findRegion(String code) {
        return regionRepository.findByCode(code).orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        commentRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
        // 시드 카테고리는 보존하고 테스트가 만든 카테고리만 제거한다.
        categoryRepository.deleteById(categoryId);
    }

    /** 구매자가 상품에 댓글을 단다(알림 유발). */
    private void postComment(String token, String content) throws Exception {
        mockMvc.perform(post("/api/products/{productId}/comments", productId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"" + content + "\" }"))
                .andExpect(status().isCreated());
    }

    /** 구매자가 상품 방을 열고 메시지 1건을 보낸다 → 판매자에게 안읽음이 생긴다(채팅 알림의 원천). */
    private ChatRoom openRoomWithUnreadMessage(Member roomBuyer) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.of(product, roomBuyer, seller));
        chatMessageRepository.save(ChatMessage.of(room, roomBuyer, "구매 문의드립니다"));
        return room;
    }

    @Nested
    @DisplayName("내 알림 목록 조회 (GET /api/notifications)")
    class GetMyNotifications {

        @Test
        @DisplayName("내 상품에 다른 사용자가 댓글을 달면 소유자에게 댓글 알림이 생긴다")
        void commentByOther_createsNotification() throws Exception {
            postComment(buyerToken, "좋은 상품이네요");

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].type").value("COMMENT"))
                    .andExpect(jsonPath("$.data[0].productId").value(productId.intValue()))
                    .andExpect(jsonPath("$.data[0].message").value(containsString("맥북 프로")))
                    .andExpect(jsonPath("$.data[0].isRead").value(false))
                    .andExpect(jsonPath("$.data[0].occurredAt").exists());
        }

        @Test
        @DisplayName("같은 상품에 댓글이 여러 개 달려도 안읽은 알림은 하나로 합쳐진다(코얼레싱)")
        void multipleComments_coalescedIntoOne() throws Exception {
            postComment(buyerToken, "첫 번째 댓글");
            postComment(buyerToken, "두 번째 댓글");

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].isRead").value(false));
        }

        @Test
        @DisplayName("자기 상품에 스스로 단 댓글은 알림을 만들지 않는다")
        void selfComment_noNotification() throws Exception {
            postComment(sellerToken, "내 상품이지만 댓글을 달아본다");

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("알림 전체 읽음 처리 (POST /api/notifications/read)")
    class ReadAll {

        @Test
        @DisplayName("읽음 처리하면 내 알림이 읽음 상태가 된다")
        void marksMyNotificationsRead() throws Exception {
            postComment(buyerToken, "좋은 상품이네요");

            mockMvc.perform(post("/api/notifications/read")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].isRead").value(true));
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(post("/api/notifications/read"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("채팅 알림 파생 (GET /api/notifications)")
    class ChatFeed {

        @Test
        @DisplayName("안읽은 채팅방이 있으면 피드에 채팅 알림(roomId 포함)이 나타난다")
        void unreadRoom_appearsAsChatItem() throws Exception {
            ChatRoom room = openRoomWithUnreadMessage(buyer);

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].type").value("CHAT"))
                    .andExpect(jsonPath("$.data[0].roomId").value(room.getId().intValue()))
                    .andExpect(jsonPath("$.data[0].productId").value(productId.intValue()))
                    // 문구는 상품명만 포함하고 상대 닉네임은 포함하지 않는다(탈퇴 소급 마스킹 드리프트 방지).
                    .andExpect(jsonPath("$.data[0].message").value(containsString("맥북 프로")))
                    .andExpect(jsonPath("$.data[0].message").value(not(containsString("buyer"))))
                    .andExpect(jsonPath("$.data[0].isRead").value(false));
        }

        @Test
        @DisplayName("구매자가 여럿이면 방(구매자)마다 별도의 채팅 알림이 나타난다")
        void multipleBuyers_eachHasOwnChatItem() throws Exception {
            Member buyer2 = memberRepository.save(Member.createUser("buyer2@example.com", "encoded-pw", "buyer2"));
            ChatRoom room1 = openRoomWithUnreadMessage(buyer);
            ChatRoom room2 = openRoomWithUnreadMessage(buyer2);

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].type").value("CHAT"))
                    .andExpect(jsonPath("$.data[1].type").value("CHAT"))
                    .andExpect(jsonPath("$.data[*].roomId",
                            containsInAnyOrder(room1.getId().intValue(), room2.getId().intValue())));
        }

        @Test
        @DisplayName("방을 읽음 처리하면 해당 채팅 알림이 피드에서 사라진다")
        void afterRoomRead_chatItemDisappears() throws Exception {
            ChatRoom room = openRoomWithUnreadMessage(buyer);

            mockMvc.perform(post("/api/chat-rooms/{roomId}/read", room.getId())
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("댓글 알림과 채팅 알림이 하나의 피드로 병합된다")
        void commentAndChat_merged() throws Exception {
            postComment(buyerToken, "댓글 알림입니다");
            openRoomWithUnreadMessage(buyer);

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[?(@.type=='COMMENT')]").exists())
                    .andExpect(jsonPath("$.data[?(@.type=='CHAT')]").exists());
        }
    }

    @Nested
    @DisplayName("안읽은 알림 개수 (GET /api/notifications/unread-count)")
    class UnreadCount {

        @Test
        @DisplayName("안읽은 댓글 알림과 안읽은 채팅방 수를 합산한다")
        void sumsUnreadCommentsAndRooms() throws Exception {
            postComment(buyerToken, "댓글 하나");
            openRoomWithUnreadMessage(buyer);

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", sellerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(2));
        }

        @Test
        @DisplayName("토큰 없이 요청하면 401과 UNAUTHORIZED를 반환한다")
        void withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/api/notifications/unread-count"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }
    }
}
