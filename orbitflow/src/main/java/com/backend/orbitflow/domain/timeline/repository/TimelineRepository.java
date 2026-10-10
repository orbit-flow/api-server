package com.backend.orbitflow.domain.timeline.repository;

import com.backend.orbitflow.domain.category.repository.CategoryRepository;
import com.backend.orbitflow.domain.post.entity.Post;
import com.backend.orbitflow.domain.timeline.dto.TimelineResponse;
import com.backend.orbitflow.domain.timeline.dto.TimelineRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

/**
 * 타임라인 : 팔로우한 계정의 투두 기반 게시글 + 투두 완료 활동 (별도 저장 없이 조회 시점에 합침)
 *
 * <p>두 출처를 DB에서 UNION ALL로 합쳐 (발생 시각 desc, 종류, id desc) 순으로 정렬한 뒤 페이지만큼 잘라내고,
 * 잘라낸 항목에만 작성자·투두·카테고리·대표 사진·좋아요·댓글 수를 붙여 평면 행으로 반환
 * <ul>
 *   <li>요청당 쿼리 2회 (목록 + 전체 개수), 페이지 크기와 무관</li>
 *   <li>열람 권한(카테고리 공개 범위)도 조회 사용자 id로 SQL에서 판정하므로 페이지 크기가 항상 정확함</li>
 *   <li>개인 카테고리 : 작성자 = 카테고리 소유자 = 수락된 팔로우 대상이고 차단 시 팔로우가 삭제되므로 PRIVATE만 제외</li>
 *   <li>팀 카테고리 : CategoryRepository.TEAM_CATEGORY_VIEWABLE</li>
 *   <li>탈퇴한·정지(BANNED)된 작성자, 삭제된 팀, 소유자가 탈퇴했거나 정지된 카테고리는 제외</li>
 *   <li>좋아요·댓글 수는 게시글 API(PostLikeRepository·CommentRepository.countByPostIn)와 같은 조건 : 탈퇴한 사용자, 조회 사용자와 차단 관계인 사용자 제외</li>
 * </ul>
 */
public interface TimelineRepository extends Repository<Post, Long> {

    String POST_ITEMS = """
            select 'POST' as kind, p0.id as item_id, p0.created_at as occurred_at, p0.user_id as actor_id, p0.todo_id as todo_id
            from posts p0
            join users u0 on u0.id = p0.user_id
            join todos t0 on t0.id = p0.todo_id
            join categories c on c.id = t0.category_id
            left join users cu on cu.id = c.user_id
            left join teams ct on ct.id = c.team_id
            where p0.user_id in (select f.followee_id from follows f where f.follower_id = :viewerId and f.status = 'ACCEPTED')
              and u0.deleted_at is null and u0.status <> 'BANNED'
              and ((c.team_id is null and cu.deleted_at is null and cu.status <> 'BANNED' and c.visibility <> 'PRIVATE')
                   or (c.team_id is not null and ct.deleted_at is null and
            """ + CategoryRepository.TEAM_CATEGORY_VIEWABLE + "))";

    String TODO_ITEMS = """
            select 'TODO_COMPLETED' as kind, t1.id as item_id, t1.completed_at as occurred_at, t1.assignee_id as actor_id, t1.id as todo_id
            from todos t1
            join users u1 on u1.id = t1.assignee_id
            join categories c on c.id = t1.category_id
            left join users cu on cu.id = c.user_id
            left join teams ct on ct.id = c.team_id
            where t1.assignee_id in (select f.followee_id from follows f where f.follower_id = :viewerId and f.status = 'ACCEPTED')
              and u1.deleted_at is null and u1.status <> 'BANNED'
              and t1.is_completed = 1
              and t1.completed_at is not null
              and t1.deleted_at is null
              and ((c.team_id is null and cu.deleted_at is null and cu.status <> 'BANNED' and c.visibility <> 'PRIVATE')
                   or (c.team_id is not null and ct.deleted_at is null and
            """ + CategoryRepository.TEAM_CATEGORY_VIEWABLE + "))";

    default Page<TimelineResponse> findTimeline(Long viewerId, Pageable pageable) {
        List<TimelineResponse> content = findTimelineRows(viewerId, pageable.getPageSize(), pageable.getOffset()).stream()
                .map(TimelineResponse::from)
                .toList();
        return PageableExecutionUtils.getPage(content, pageable, () -> countTimeline(viewerId));
    }

    @Query(nativeQuery = true, value = """
            select x.kind as kind,
                   x.item_id as itemId,
                   x.occurred_at as occurredAt,
                   u.uuid as actorUuid,
                   u.name as actorName,
                   u.profile_image as actorProfileImage,
                   t.id as todoId,
                   t.name as todoName,
                   (t.deleted_at is not null) as todoDeleted,
                   c.id as categoryId,
                   c.name as categoryName,
                   c.color as categoryColor,
                   p.content as content,
                   (select pi.image_url from post_images pi where pi.post_id = p.id order by pi.sort_order limit 1) as thumbnailUrl,
                   (select count(*) from post_images pi where pi.post_id = p.id) as imageCount,
                   (select count(*) from likes l
                    join users lu on lu.id = l.user_id
                    where l.post_id = p.id
                      and lu.deleted_at is null
                      and not exists (select 1 from blocks lb
                                      where (lb.blocker_id = :viewerId and lb.blockee_id = lu.id) or (lb.blocker_id = lu.id and lb.blockee_id = :viewerId))) as likeCount,
                   (select count(*) from comments cm
                    join users cmu on cmu.id = cm.user_id
                    where cm.post_id = p.id
                      and cmu.deleted_at is null
                      and not exists (select 1 from blocks cb
                                      where (cb.blocker_id = :viewerId and cb.blockee_id = cmu.id) or (cb.blocker_id = cmu.id and cb.blockee_id = :viewerId))) as commentCount,
                   exists(select 1 from likes l where l.post_id = p.id and l.user_id = :viewerId) as liked
            from ((""" + POST_ITEMS + ") union all (" + TODO_ITEMS + """
                ) order by occurred_at desc, kind asc, item_id desc
                limit :limit offset :offset
            ) x
            join users u on u.id = x.actor_id
            join todos t on t.id = x.todo_id
            join categories c on c.id = t.category_id
            left join posts p on x.kind = 'POST' and p.id = x.item_id
            order by x.occurred_at desc, x.kind asc, x.item_id desc
            """)
    List<TimelineRow> findTimelineRows(@Param("viewerId") Long viewerId, @Param("limit") int limit, @Param("offset") long offset);

    @Query(nativeQuery = true, value = "select count(*) from ((" + POST_ITEMS + ") union all (" + TODO_ITEMS + ")) z")
    long countTimeline(@Param("viewerId") Long viewerId);
}
