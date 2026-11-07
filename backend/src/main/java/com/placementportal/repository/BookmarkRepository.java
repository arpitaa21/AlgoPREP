package com.placementportal.repository;

import com.placementportal.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    long deleteByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("select b from Bookmark b join fetch b.question q join fetch q.topic "
            + "where b.user.id = :userId order by b.createdAt desc")
    List<Bookmark> findAllByUser(@Param("userId") Long userId);

    /** Which of these question ids did the user bookmark? One query for a whole page (instead of one per row). */
    @Query("select b.question.id from Bookmark b where b.user.id = :userId and b.question.id in :ids")
    List<Long> findBookmarkedIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
