package me.muheun.moaspace.repository.impl

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.post.QPost
import me.muheun.moaspace.domain.user.QUser
import me.muheun.moaspace.mapper.PostMapper
import me.muheun.moaspace.query.dto.PostSearchFilter
import me.muheun.moaspace.repository.PostCustomRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

// 게시글 동적 검색 (QueryDSL + MyBatis)
@Repository
class PostCustomRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
    private val postMapper: PostMapper
) : PostCustomRepository {

    companion object {
        private val logger = LoggerFactory.getLogger(PostCustomRepositoryImpl::class.java)
        private val post = QPost.post
        private val author = QUser.user
    }

    // 통합 동적 검색
    override fun search(
        filter: PostSearchFilter,
        pageable: Pageable
    ): Page<Post> {
        logger.debug("search 호출: filter={}, page={}, size={}",
            filter, pageable.pageNumber, pageable.pageSize)

        // 해시태그 검색은 MyBatis 위임 (PostgreSQL ANY 배열 연산)
        if (filter.hashtag != null) {
            return searchByHashtagWithMyBatis(filter, pageable)
        }

        // QueryDSL 동적 검색 (title, author, deleted)
        val builder = BooleanBuilder()

        // 제목 검색 (대소문자 구분 없음, 부분 일치)
        filter.title?.let {
            builder.and(post.title.containsIgnoreCase(it))
        }

        // 작성자 검색 (이름으로 검색)
        filter.author?.let {
            builder.and(author.name.eq(it))
        }

        // 삭제 여부 필터 (기본값: deleted=false)
        val deleted = filter.deleted ?: false
        builder.and(post.deleted.eq(deleted))

        // 쿼리 실행 (페이지네이션)
        val query = jpaQueryFactory
            .selectFrom(post)
            .leftJoin(post.author, author).fetchJoin()
            .where(builder)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        // 정렬 적용
        pageable.sort.forEach { order ->
            when (order.property) {
                "createdAt" -> query.orderBy(
                    if (order.isAscending) post.createdAt.asc() else post.createdAt.desc()
                )
                "updatedAt" -> query.orderBy(
                    if (order.isAscending) post.updatedAt.asc() else post.updatedAt.desc()
                )
                "title" -> query.orderBy(
                    if (order.isAscending) post.title.asc() else post.title.desc()
                )
            }
        }

        // 정렬이 없으면 기본 정렬 (createdAt 내림차순)
        if (!pageable.sort.isSorted) {
            query.orderBy(post.createdAt.desc())
        }

        val results = query.fetch()

        // 전체 개수 조회 (카운트 쿼리)
        val totalCount = jpaQueryFactory
            .select(post.count())
            .from(post)
            .leftJoin(post.author, author)
            .where(builder)
            .fetchOne() ?: 0L

        logger.info("search 완료: 검색된 게시글 수={}, 전체={}, 조건={}",
            results.size, totalCount, filter)

        return PageImpl(results, pageable, totalCount)
    }

    // 해시태그 검색 (MyBatis)
    private fun searchByHashtagWithMyBatis(filter: PostSearchFilter, pageable: Pageable): Page<Post> {
        val deleted = filter.deleted ?: false
        val hashtag = filter.hashtag!!

        logger.debug("searchByHashtagWithMyBatis 호출: hashtag={}, deleted={}", hashtag, deleted)

        val results = postMapper.findByHashtag(
            hashtag = hashtag,
            deleted = deleted,
            limit = pageable.pageSize,
            offset = pageable.offset
        )

        val totalCount = postMapper.countByHashtagForSearch(hashtag, deleted)

        logger.info("searchByHashtagWithMyBatis 완료: 검색된 게시글 수={}, 전체={}", results.size, totalCount)

        return PageImpl(results, pageable, totalCount)
    }

    // 해시태그 개수 조회 (MyBatis)
    override fun countByHashtag(hashtag: String): Long {
        logger.debug("countByHashtag 호출: hashtag={}", hashtag)

        val count = postMapper.countByHashtag(hashtag)

        logger.info("countByHashtag 완료: hashtag={}, count={}", hashtag, count)

        return count
    }
}
