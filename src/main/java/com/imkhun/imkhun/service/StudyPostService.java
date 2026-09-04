package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.PostComment;
import com.imkhun.imkhun.domain.PostReaction;
import com.imkhun.imkhun.domain.StudyPost;
import com.imkhun.imkhun.dto.CommentResponse;
import com.imkhun.imkhun.dto.CreatePostRequest;
import com.imkhun.imkhun.dto.PostResponse;
import com.imkhun.imkhun.dto.UpdatePostRequest;
import com.imkhun.imkhun.repository.PostCommentRepository;
import com.imkhun.imkhun.repository.PostReactionRepository;
import com.imkhun.imkhun.repository.StudyPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostCommentRepository postCommentRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_TOPICS = Set.of("korean", "japanese", "thai", "english", "computer");
    private static final Set<String> VALID_CATEGORIES = Set.of("VOCAB", "GRAMMAR", "WRITING", "OTHER");
    private static final Set<String> VALID_REACTIONS = Set.of("LIKE", "DISLIKE");

    public StudyPostService(StudyPostRepository studyPostRepository, PostReactionRepository postReactionRepository,
                            PostCommentRepository postCommentRepository) {
        this.studyPostRepository = studyPostRepository;
        this.postReactionRepository = postReactionRepository;
        this.postCommentRepository = postCommentRepository;
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request, String username, String nickname) {
        if (request.topic() == null || !VALID_TOPICS.contains(request.topic())) {
            throw new IllegalStateException("올바르지 않은 주제예요.");
        }
        String category = "computer".equals(request.topic()) ? null : request.category();
        validateContent(request.topic(), category, request.title(), request.content());

        StudyPost post = StudyPost.create(request.topic(), category, request.title(), request.content(), username, nickname);
        StudyPost saved = studyPostRepository.save(post);
        return toResponse(saved, username);
    }

    // category가 없으면(null/빈 값) "컴퓨터"처럼 항목 구분 없이 그 주제 전체를 보여줌
    @Transactional(readOnly = true)
    public List<PostResponse> getPosts(String topic, String category, String viewerUsername) {
        List<StudyPost> posts = (category == null || category.isBlank())
                ? studyPostRepository.findByTopicOrderByCreatedAtDesc(topic)
                : studyPostRepository.findByTopicAndCategoryOrderByCreatedAtDesc(topic, category);

        return posts.stream().map(p -> toResponse(p, viewerUsername)).toList();
    }

    // 마이페이지 "내가 쓴 글"
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(String username) {
        return studyPostRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(p -> toResponse(p, username))
                .toList();
    }

    @Transactional
    public PostResponse updatePost(Long id, UpdatePostRequest request, String username) {
        StudyPost post = findOwnedPost(id, username, "본인이 쓴 글만 수정할 수 있어요.");

        String category = "computer".equals(post.getTopic()) ? null : request.category();
        validateContent(post.getTopic(), category, request.title(), request.content());

        post.update(category, request.title(), request.content());
        StudyPost saved = studyPostRepository.save(post);
        return toResponse(saved, username);
    }

    @Transactional
    public void deletePost(Long id, String username) {
        findOwnedPost(id, username, "본인이 쓴 글만 삭제할 수 있어요.");
        studyPostRepository.deleteById(id);
    }

    // ---- 좋아요 / 싫어요 ----

    @Transactional
    public PostResponse toggleReaction(Long postId, String type, String username) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalStateException("글을 찾을 수 없어요."));
        if (type == null || !VALID_REACTIONS.contains(type)) {
            throw new IllegalStateException("올바르지 않은 반응이에요.");
        }

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUsername(postId, username);
        if (existing.isPresent()) {
            PostReaction reaction = existing.get();
            if (reaction.getType().equals(type)) {
                // 같은 걸 다시 누르면 취소돼요
                postReactionRepository.delete(reaction);
            } else {
                reaction.changeType(type);
                postReactionRepository.save(reaction);
            }
        } else {
            postReactionRepository.save(PostReaction.create(postId, username, type));
        }

        return toResponse(post, username);
    }

    // ---- 댓글 ----

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        return postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(c -> new CommentResponse(c.getId(), c.getNickname(), c.getContent(), c.getCreatedAt().format(DATE_FORMAT), c.isAdmin()))
                .toList();
    }

    @Transactional
    public CommentResponse addComment(Long postId, String content, String username, String nickname) {
        if (!studyPostRepository.existsById(postId)) {
            throw new IllegalStateException("글을 찾을 수 없어요.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("댓글 내용을 입력해주세요.");
        }
        PostComment saved = postCommentRepository.save(PostComment.create(postId, username, nickname, content));
        return new CommentResponse(saved.getId(), saved.getNickname(), saved.getContent(), saved.getCreatedAt().format(DATE_FORMAT), saved.isAdmin());
    }

    // 관리자가 학생 글에 남기는 댓글 — 학생 화면에는 "관리자 댓글"로 표시돼요.
    @Transactional
    public CommentResponse addAdminComment(Long postId, String adminUsername, String content) {
        if (!studyPostRepository.existsById(postId)) {
            throw new IllegalStateException("글을 찾을 수 없어요.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("댓글 내용을 입력해주세요.");
        }
        PostComment saved = postCommentRepository.save(PostComment.createByAdmin(postId, adminUsername, content));
        return new CommentResponse(saved.getId(), saved.getNickname(), saved.getContent(), saved.getCreatedAt().format(DATE_FORMAT), saved.isAdmin());
    }

    // ---- 관리자: 게시판 관리 (전체 글 조회 / 작성자 상관없이 삭제) ----

    @Transactional(readOnly = true)
    public List<PostResponse> getAllPostsForAdmin() {
        return studyPostRepository.findAll(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    @Transactional
    public void deletePostAsAdmin(Long id) {
        if (!studyPostRepository.existsById(id)) {
            throw new IllegalStateException("글을 찾을 수 없어요.");
        }
        studyPostRepository.deleteById(id);
    }

    private StudyPost findOwnedPost(Long id, String username, String ownershipErrorMessage) {
        StudyPost post = studyPostRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("글을 찾을 수 없어요."));
        if (!post.getUsername().equals(username)) {
            throw new IllegalStateException(ownershipErrorMessage);
        }
        return post;
    }

    private void validateContent(String topic, String category, String title, String content) {
        if (!"computer".equals(topic)) {
            if (category == null || !VALID_CATEGORIES.contains(category)) {
                throw new IllegalStateException("올바르지 않은 항목이에요.");
            }
        }
        if (title == null || title.isBlank()) {
            throw new IllegalStateException("제목을 입력해주세요.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("내용을 입력해주세요.");
        }
    }

    private PostResponse toResponse(StudyPost post, String viewerUsername) {
        int likeCount = (int) postReactionRepository.countByPostIdAndType(post.getId(), "LIKE");
        int dislikeCount = (int) postReactionRepository.countByPostIdAndType(post.getId(), "DISLIKE");
        int commentCount = (int) postCommentRepository.countByPostId(post.getId());
        String myReaction = viewerUsername == null ? null :
                postReactionRepository.findByPostIdAndUsername(post.getId(), viewerUsername)
                        .map(PostReaction::getType)
                        .orElse(null);

        return new PostResponse(post.getId(), post.getNickname(), post.getTopic(), post.getCategory(),
                post.getTitle(), post.getContent(), post.getCreatedAt().format(DATE_FORMAT),
                likeCount, dislikeCount, commentCount, myReaction);
    }
}