package com.dongnemarket.comment.repository;

import com.dongnemarket.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByProductIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long productId);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);
}
