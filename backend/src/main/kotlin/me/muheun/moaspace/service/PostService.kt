package me.muheun.moaspace.service

import me.muheun.moaspace.config.VectorProperties
import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.vector.VectorEntityType
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.PostSearchRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val vectorProperties: VectorProperties,
    private val vectorIndexingService: VectorIndexingService,
    private val vectorSearchService: VectorSearchService
) {

    private val logger = LoggerFactory.getLogger(PostService::class.java)

    @Transactional
    fun createPost(request: CreatePostRequest, userId: Long): Post {
        logger.info("게시글 생성 시작: userId=$userId, title=${request.title}")

        val author = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("작성자를 찾을 수 없습니다: userId=$userId") }

        val sanitizedTitle = sanitizeTitle(request.title)
        val sanitizedHashtags = request.hashtags.map { sanitizeHashtag(it) }.toTypedArray()
        val sanitizedHtml = sanitizeHtml(request.contentHtml)
        val contentMarkdown = convertHtmlToMarkdown(sanitizedHtml)
        val contentText = extractPlainText(sanitizedHtml)

        val post = Post(
            title = sanitizedTitle,
            contentMarkdown = contentMarkdown,
            contentHtml = sanitizedHtml,
            contentText = contentText,
            author = author,
            hashtags = sanitizedHashtags
        )

        val savedPost = postRepository.save(post)
        logger.info("게시글 저장 완료: postId=${savedPost.id}")

        val vectorFields = vectorIndexingService.extractVectorFields(
            entity = savedPost,
            entityType = VectorEntityType.POST.typeName
        )
        vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = savedPost.id.toString(),
            fields = vectorFields
        )
        logger.info("게시글 벡터화 완료: postId=${savedPost.id}")

        return savedPost
    }

    private fun sanitizeTitle(title: String): String {
        return title.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun sanitizeHashtag(hashtag: String): String {
        return hashtag.replace(Regex("<[^>]*>"), "").trim()
    }

    // HTML → 마크다운 변환 (h1-h6, p, ul/ol, code, blockquote, img, link 등 지원)
    private fun convertHtmlToMarkdown(html: String): String {
        if (html.isBlank()) return ""

        val doc = Jsoup.parse(html)
        val markdown = StringBuilder()

        fun processNode(element: Element, prefix: String = "") {
            for (node in element.childNodes()) {
                when (node) {
                    is TextNode -> {
                        val text = node.text().trim()
                        if (text.isNotEmpty()) {
                            markdown.append(prefix).append(text)
                        }
                    }
                    is Element -> {
                        when (node.tagName()) {
                            "h1" -> markdown.append("\n# ").append(node.text()).append("\n\n")
                            "h2" -> markdown.append("\n## ").append(node.text()).append("\n\n")
                            "h3" -> markdown.append("\n### ").append(node.text()).append("\n\n")
                            "h4" -> markdown.append("\n#### ").append(node.text()).append("\n\n")
                            "h5" -> markdown.append("\n##### ").append(node.text()).append("\n\n")
                            "h6" -> markdown.append("\n###### ").append(node.text()).append("\n\n")
                            "p" -> {
                                processNode(node, "")
                                markdown.append("\n\n")
                            }
                            "br" -> markdown.append("\n")
                            "strong", "b" -> markdown.append("**").append(node.text()).append("**")
                            "em", "i" -> markdown.append("*").append(node.text()).append("*")
                            "code" -> markdown.append("`").append(node.text()).append("`")
                            "pre" -> {
                                val codeElement = node.selectFirst("code")
                                if (codeElement != null) {
                                    markdown.append("\n```\n").append(codeElement.text()).append("\n```\n\n")
                                } else {
                                    markdown.append("\n```\n").append(node.text()).append("\n```\n\n")
                                }
                            }
                            "blockquote" -> {
                                markdown.append("\n> ").append(node.text()).append("\n\n")
                            }
                            "ul" -> {
                                for (li in node.select("li")) {
                                    markdown.append("- ").append(li.text()).append("\n")
                                }
                                markdown.append("\n")
                            }
                            "ol" -> {
                                var index = 1
                                for (li in node.select("li")) {
                                    markdown.append("${index}. ").append(li.text()).append("\n")
                                    index++
                                }
                                markdown.append("\n")
                            }
                            "a" -> {
                                val href = node.attr("href")
                                markdown.append("[").append(node.text()).append("](").append(href).append(")")
                            }
                            "img" -> {
                                val src = node.attr("src")
                                val alt = node.attr("alt")
                                markdown.append("![").append(alt).append("](").append(src).append(")")
                            }
                            "hr" -> markdown.append("\n---\n\n")
                            else -> processNode(node, prefix)
                        }
                    }
                }
            }
        }

        processNode(doc.body())
        return markdown.toString().trim()
    }

    private fun sanitizeHtml(html: String): String {
        val policy = org.owasp.html.Sanitizers.FORMATTING
            .and(org.owasp.html.Sanitizers.BLOCKS)
            .and(org.owasp.html.Sanitizers.LINKS)
            .and(org.owasp.html.Sanitizers.IMAGES)
            .and(org.owasp.html.Sanitizers.TABLES)
        return policy.sanitize(html)
    }

    private fun extractPlainText(html: String): String {
        // HTML 태그 제거
        val withoutTags = html.replace(Regex("<[^>]*>"), " ")
        // HTML 엔티티 디코드
        val decoded = withoutTags
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        // 연속된 공백 제거
        return decoded.replace(Regex("\\s+"), " ").trim()
    }

    // 게시글 조회 (삭제되지 않은 글만 반환)
    fun getPostById(postId: Long): Post {
        logger.debug("게시글 조회: postId=$postId")

        val post = postRepository.findById(postId)
            .orElseThrow { NoSuchElementException("게시글을 찾을 수 없습니다: postId=$postId") }

        if (post.deleted) {
            logger.warn("삭제된 게시글 접근 시도: postId=$postId")
            throw NoSuchElementException("게시글을 찾을 수 없습니다: postId=$postId")
        }

        return post
    }

    @Transactional
    fun updatePost(postId: Long, request: UpdatePostRequest, userId: Long): Post {
        logger.info("게시글 수정 시작: postId=$postId, userId=$userId")

        val post = getPostById(postId)

        if (post.author.id != userId) {
            logger.warn("게시글 수정 권한 없음: postId=$postId, authorId=${post.author.id}, requestUserId=$userId")
            throw IllegalArgumentException("게시글을 수정할 권한이 없습니다")
        }

        val sanitizedTitle = sanitizeTitle(request.title)
        val sanitizedHashtags = request.hashtags.map { sanitizeHashtag(it) }.toTypedArray()
        val sanitizedHtml = sanitizeHtml(request.contentHtml)
        val contentMarkdown = convertHtmlToMarkdown(sanitizedHtml)
        val contentText = extractPlainText(sanitizedHtml)

        post.title = sanitizedTitle
        post.contentMarkdown = contentMarkdown
        post.contentHtml = sanitizedHtml
        post.contentText = contentText
        post.hashtags = sanitizedHashtags

        val updatedPost = postRepository.save(post)
        logger.info("게시글 업데이트 완료: postId=$postId")

        val vectorFields = vectorIndexingService.extractVectorFields(
            entity = updatedPost,
            entityType = VectorEntityType.POST.typeName
        )
        vectorIndexingService.reindexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = updatedPost.id.toString(),
            fields = vectorFields
        )
        logger.info("게시글 벡터 재생성 완료: postId=$postId")

        return updatedPost
    }

    
    fun getAllPosts(pageable: Pageable, hashtag: String?): Page<Post> {
        logger.debug("게시글 목록 조회: page=${pageable.pageNumber}, size=${pageable.pageSize}, hashtag=$hashtag")

        return if (hashtag.isNullOrBlank()) {
            postRepository.findByDeletedFalse(pageable)
        } else {
            // Native Query는 Pageable의 Sort를 올바르게 처리하지 못하므로
            // List와 Count를 분리해서 조회 후 수동으로 Page 생성
            val content = postRepository.findByHashtag(
                hashtag = hashtag,
                limit = pageable.pageSize,
                offset = pageable.offset
            )
            val total = postRepository.countByHashtag(hashtag)
            org.springframework.data.domain.PageImpl(content, pageable, total)
        }
    }

    
    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        logger.info("게시글 삭제 시작: postId=$postId, userId=$userId")

        val post = getPostById(postId)

        if (post.author.id != userId) {
            logger.warn("게시글 삭제 권한 없음: postId=$postId, authorId=${post.author.id}, requestUserId=$userId")
            throw IllegalArgumentException("게시글을 삭제할 권한이 없습니다")
        }

        post.deleted = true
        postRepository.save(post)

        logger.info("게시글 소프트 삭제 완료: postId=$postId (deleted=true)")
    }


    fun searchPosts(request: PostSearchRequest): List<Post> {
        logger.info("게시글 벡터 검색 시작: query=${request.query}, limit=${request.limit}")

        val vectorRequest = VectorSearchRequest(
            query = request.query,
            namespace = vectorProperties.namespace,
            entity = VectorEntityType.POST.typeName,
            limit = request.limit
        )

        val postScores = vectorSearchService.search(vectorRequest)
        logger.debug("벡터 검색 결과: ${postScores.size}개 postId 반환")

        if (postScores.isEmpty()) {
            logger.info("검색 결과 없음")
            return emptyList()
        }

        val postIds = postScores.keys.map { it.toLong() }
        val posts = postRepository.findAllById(postIds)
            .filter { !it.deleted }
            .associateBy { it.id }

        val sortedPosts = postIds.mapNotNull { postId ->
            posts[postId]
        }

        logger.info("게시글 벡터 검색 완료: ${sortedPosts.size}개 게시글 반환")
        return sortedPosts
    }
}
