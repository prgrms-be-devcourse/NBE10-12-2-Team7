package com.dongnemarket.chat.repository;

import com.dongnemarket.chat.entity.ChatMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 방의 메시지를 최신순(id DESC)으로 커서 페이지네이션 조회한다.
     * <p><b>왜 offset이 아니라 커서인가</b>: 메시지는 무한히 쌓이고 조회 중에도 계속 삽입된다.
     * offset은 뒤로 갈수록 느려지고, 새 메시지가 들어오면 페이지 경계가 밀려 중복/누락이 생긴다.
     * <p><b>왜 created_at이 아니라 id 커서인가</b>: id는 IDENTITY라 삽입순으로 단조증가한다.
     * 따라서 id 하나만으로 완전한 정렬키가 되어(동시각 타이브레이크 불필요) 커서가 단순해진다.
     * <p>{@code cursor}가 null이면 첫 페이지(가장 최근부터), 값이 있으면 그보다 오래된(id가 작은) 메시지부터.
     * sender는 화면 표시에 필요하므로 {@code JOIN FETCH}로 함께 로딩한다.
     */
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.chatRoom.id = :roomId " +
            "AND (:cursor IS NULL OR m.id < :cursor) " +
            "ORDER BY m.id DESC")
    List<ChatMessage> findPageByRoom(@Param("roomId") Long roomId,
                                     @Param("cursor") Long cursor,
                                     Limit limit);

    /**
     * 여러 방의 <b>마지막 메시지</b>를 한 번에 조회한다(방 목록 미리보기용). 방별 최대 id = 최신 메시지(id 단조증가).
     * 방 하나당 최대 한 건이라 N+1 없이 목록의 마지막 메시지를 채운다.
     */
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN FETCH m.sender " +
            "WHERE m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 " +
            "WHERE m2.chatRoom.id IN :roomIds GROUP BY m2.chatRoom.id)")
    List<ChatMessage> findLatestPerRoom(@Param("roomIds") List<Long> roomIds);
}
