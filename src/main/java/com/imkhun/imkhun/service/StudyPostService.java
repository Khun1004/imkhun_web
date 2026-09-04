package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.CommentReaction;
import com.imkhun.imkhun.domain.PostComment;
import com.imkhun.imkhun.domain.PostReaction;
import com.imkhun.imkhun.domain.StudyPost;
import com.imkhun.imkhun.dto.CommentResponse;
import com.imkhun.imkhun.dto.CreatePostRequest;
import com.imkhun.imkhun.dto.PostResponse;
import com.imkhun.imkhun.dto.UpdatePostRequest;
import com.imkhun.imkhun.repository.CommentReactionRepository;
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
    private final CommentReactionRepository commentReactionRepository;
    private final NotificationService notificationService;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Set<String> VALID_TOPICS = Set.of("korean", "japanese", "thai", "english", "computer");
    private static final Set<String> VALID_CATEGORIES = Set.of("VOCAB", "GRAMMAR", "WRITING", "OTHER");
    private static final Set<String> VALID_REACTIONS = Set.of("LIKE", "DISLIKE");

    public StudyPostService(StudyPostRepository studyPostRepository, PostReactionRepository postReactionRepository,
                            PostCommentRepository postCommentRepository, CommentReactionRepository commentReactionRepository,
                            NotificationService notificationService) {
        this.studyPostRepository = studyPostRepository;
        this.postReactionRepository = postReactionRepository;
        this.postCommentRepository = postCommentRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.notificationService = notificationService;
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

    // ---- 댓글 (답글 포함) ----

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, String viewerUsername) {
        return postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(c -> toCommentResponse(c, viewerUsername))
                .toList();
    }

    // parentCommentId가 있으면 그 댓글에 대한 답글로 달려요 (A의 댓글에 B가 답글, B의 답글에 A가 다시 답글하는 식)
    @Transactional
    public CommentResponse addComment(Long postId, String content, String username, String nickname, Long parentCommentId) {
        validateNewComment(postId, content, parentCommentId);
        PostComment saved = postCommentRepository.save(PostComment.create(postId, username, nickname, content, parentCommentId));
        notifyAboutComment(postId, parentCommentId, username, nickname);
        return toCommentResponse(saved, username);
    }

    // 관리자가 학생 글/댓글에 남기는 댓글 — 학생 화면에는 "관리자 댓글"로 표시돼요.
    @Transactional
    public CommentResponse addAdminComment(Long postId, String adminUsername, String content, Long parentCommentId) {
        validateNewComment(postId, content, parentCommentId);
        PostComment saved = postCommentRepository.save(PostComment.createByAdmin(postId, adminUsername, content, parentCommentId));
        notifyAboutComment(postId, parentCommentId, adminUsername, "관리자");
        return toCommentResponse(saved, adminUsername);
    }

    // 답글이면 원댓글 작성자(학생일 때만)에게, 새 댓글이면 글쓴이에게 알림을 보내요. 본인 글/댓글에는 안 보내요.
    private void notifyAboutComment(Long postId, Long parentCommentId, String actingUsername, String actingNickname) {
        if (parentCommentId != null) {
            postCommentRepository.findById(parentCommentId).ifPresent(parent -> {
                if (!parent.isAdmin() && !parent.getUsername().equals(actingUsername)) {
                    notificationService.notifyStudent(parent.getUsername(), "COMMENT_REPLY",
                            actingNickname + "님이 회원님의 댓글에 답글을 남겼어요.", postId);
                }
            });
        } else {
            studyPostRepository.findById(postId).ifPresent(post -> {
                if (!post.getUsername().equals(actingUsername)) {
                    notificationService.notifyStudent(post.getUsername(), "POST_COMMENT",
                            actingNickname + "님이 회원님의 글에 댓글을 남겼어요.", postId);
                }
            });
        }
    }

    // 댓글 좋아요/싫어요 — 학생, 관리자 둘 다 쓸 수 있어요 (username은 학생 아이디 또는 관리자 아이디)
    @Transactional
    public CommentResponse toggleCommentReaction(Long commentId, String type, String username) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalStateException("댓글을 찾을 수 없어요."));
        if (type == null || !VALID_REACTIONS.contains(type)) {
            throw new IllegalStateException("올바르지 않은 반응이에요.");
        }

        Optional<CommentReaction> existing = commentReactionRepository.findByCommentIdAndUsername(commentId, username);
        if (existing.isPresent()) {
            CommentReaction reaction = existing.get();
            if (reaction.getType().equals(type)) {
                commentReactionRepository.delete(reaction);
            } else {
                reaction.changeType(type);
                commentReactionRepository.save(reaction);
            }
        } else {
            commentReactionRepository.save(CommentReaction.create(commentId, username, type));
        }

        return toCommentResponse(comment, username);
    }

    private void validateNewComment(Long postId, String content, Long parentCommentId) {
        StudyPost post = studyPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalStateException("글을 찾을 수 없어요."));
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("댓글 내용을 입력해주세요.");
        }
        if (parentCommentId != null) {
            PostComment parent = postCommentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalStateException("답글을 달 댓글을 찾을 수 없어요."));
            if (!parent.getPostId().equals(post.getId())) {
                throw new IllegalStateException("같은 글의 댓글에만 답글을 달 수 있어요.");
            }
        }
    }

    private CommentResponse toCommentResponse(PostComment comment, String viewerUsername) {
        int likeCount = (int) commentReactionRepository.countByCommentIdAndType(comment.getId(), "LIKE");
        int dislikeCount = (int) commentReactionRepository.countByCommentIdAndType(comment.getId(), "DISLIKE");
        String myReaction = viewerUsername == null ? null :
                commentReactionRepository.findByCommentIdAndUsername(comment.getId(), viewerUsername)
                        .map(CommentReaction::getType)
                        .orElse(null);

        return new CommentResponse(comment.getId(), comment.getNickname(), comment.getContent(),
                comment.getCreatedAt().format(DATE_FORMAT), comment.isAdmin(), comment.getParentCommentId(),
                likeCount, dislikeCount, myReaction);
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