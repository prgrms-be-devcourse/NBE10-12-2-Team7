package com.dongnemarket.admin.ai.config;

import com.dongnemarket.admin.ai.tool.AdminCommentTools;
import com.dongnemarket.admin.ai.tool.AdminDashboardTools;
import com.dongnemarket.admin.ai.tool.AdminMemberTools;
import com.dongnemarket.admin.ai.tool.AdminProductTools;
import com.dongnemarket.admin.ai.tool.AdminReportTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 관리자 AI 어시스턴트용 ChatClient 설정.
 * - 시스템 프롬프트: 역할(조회 도우미)·행동 규칙(반드시 tool 사용, 읽기 전용) 고정
 * - defaultTools: 읽기 전용 tool 5종 등록 → 모든 요청에서 LLM이 호출 가능
 */
@Configuration
public class AdminAiConfig {

    private static final String SYSTEM_PROMPT = """
            너는 '동네마켓' 중고거래 서비스의 관리자 데이터 조회 도우미다.

            행동 규칙:
            1. 회원/상품/댓글/신고/대시보드 데이터가 필요한 질문에는 반드시 제공된 tool을 호출해
               실제 데이터를 확인한 뒤 답한다. 데이터를 추측하거나 지어내지 않는다.
            2. 너는 읽기 전용이다. 회원 정지, 상품 삭제/숨김, 신고 상태 변경 등
               데이터를 변경하는 요청을 받으면 실행하지 말고 "저는 조회만 할 수 있습니다.
               변경은 관리자 화면에서 직접 진행해 주세요."라고 안내한다.
            3. 답변은 한국어로 간결하게, 핵심 수치와 항목 위주로 정리한다.
            4. 관리 데이터와 무관한 질문에는 관리자 데이터 조회 도우미라는 역할을 벗어나지 않는 선에서
               짧게 답하거나 역할 범위를 안내한다.
            """;

    @Bean
    public ChatClient adminChatClient(ChatClient.Builder builder,
                                      AdminMemberTools adminMemberTools,
                                      AdminProductTools adminProductTools,
                                      AdminCommentTools adminCommentTools,
                                      AdminReportTools adminReportTools,
                                      AdminDashboardTools adminDashboardTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                // qwen3 계열은 thinking이 기본 활성 → 최종 답변이 thinking 필드로 새어
                // content가 비는 문제가 있어 반드시 비활성화한다. (2026-07-02 실측 검증)
                .defaultOptions(OllamaChatOptions.builder().disableThinking().build())
                .defaultTools(adminMemberTools, adminProductTools, adminCommentTools,
                        adminReportTools, adminDashboardTools)
                .build();
    }
}
