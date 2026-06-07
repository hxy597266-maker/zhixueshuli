package com.zhixue.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.common.Result;
import com.zhixue.entity.Comment;
import com.zhixue.entity.Like;
import com.zhixue.entity.Post;
import com.zhixue.entity.User;
import com.zhixue.mapper.CommentMapper;
import com.zhixue.mapper.LikeMapper;
import com.zhixue.mapper.PostMapper;
import com.zhixue.mapper.UserMapper;
import com.zhixue.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/post")
@Tag(name = "帖子模块")
public class PostController {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String POST_LIST_CACHE_KEY = "post:list:";
    private static final String POST_DETAIL_CACHE_KEY = "post:detail:";
    private static final String HOT_POSTS_CACHE_KEY = "post:hot";
    private static final String POST_LIKES_CACHE_KEY = "post:likes:";
    private static final long CACHE_EXPIRE_MINUTES = 10;
    private static final long HOT_POSTS_EXPIRE_MINUTES = 30;

    @Operation(summary = "测试接口")
    @GetMapping("/test")
    public String test(){
        return "post controller ok";
    }

    @Operation(summary = "异常测试")
    @GetMapping("/error")
    public String error(){
        int a = 1 / 0;
        return "ok";
    }

    @Operation(summary = "获取帖子列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        String cacheKey = POST_LIST_CACHE_KEY + page + ":" + size;

        Object cacheData = redisTemplate.opsForValue().get(cacheKey);

        if(cacheData != null){
            System.out.println("✓ 从 Redis 获取帖子列表");
            return Result.success((Map<String,Object>) cacheData);
        }

        System.out.println("从 MySQL 查询帖子列表");
        
        Page<Post> postPage = new Page<>(page, size);

        Page<Post> resultPage = postMapper.selectPage(
                postPage,
                new QueryWrapper<Post>().orderByDesc("id")
        );

        List<Post> records = resultPage.getRecords();

        Long currentUserId = null;
        try {
            String token = request.getHeader("Authorization");
            if(token != null && token.startsWith("Bearer ")) {
                token = token.replace("Bearer ", "");
                currentUserId = JwtUtil.getUserId(token);
            }
        } catch (Exception e) { }

        for(Post post : records){
            Long likeCount = likeMapper.selectCount(
                    new QueryWrapper<Like>()
                            .eq("post_id", post.getId())
            );
            post.setLikeCount(likeCount.intValue());

            Long commentCount = commentMapper.selectCount(
                    new QueryWrapper<Comment>()
                            .eq("post_id", post.getId())
            );
            post.setCommentCount(commentCount.intValue());

            if (currentUserId != null) {
                Long userLikeCount = likeMapper.selectCount(
                        new QueryWrapper<Like>()
                                .eq("post_id", post.getId())
                                .eq("user_id", currentUserId)
                );
                post.setLikedByCurrentUser(userLikeCount > 0);
            } else {
                post.setLikedByCurrentUser(false);
            }

            User author = userMapper.selectById(post.getUserId());
            if (author != null && author.getAvatar() != null) {
                post.setUserAvatar(author.getAvatar());
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("list", records);
        map.put("total", resultPage.getTotal());
        map.put("page", resultPage.getCurrent());
        map.put("size", resultPage.getSize());
        map.put("pages", resultPage.getPages());

        redisTemplate.opsForValue().set(
                cacheKey,
                map,
                CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        System.out.println("✓ 帖子列表存入 Redis");

        return Result.success(map);
    }

    @Operation(summary = "获取热门帖子")
    @GetMapping("/hot")
    public Result<List<Post>> getHotPosts(HttpServletRequest request) {
        Object cachedData = redisTemplate.opsForValue().get(HOT_POSTS_CACHE_KEY);
        if (cachedData != null) {
            System.out.println("✓ 从 Redis 缓存获取热门帖子");
            return Result.success((List<Post>) cachedData);
        }

        System.out.println("从 MySQL 数据库查询热门帖子");
        List<Post> allPosts = postMapper.selectList(
                new QueryWrapper<Post>().orderByDesc("id")
        );

        List<Post> hotPosts = new ArrayList<>();
        for (Post post : allPosts) {
            Long likeCount = likeMapper.selectCount(
                    new QueryWrapper<Like>()
                            .eq("post_id", post.getId())
            );
            post.setLikeCount(likeCount.intValue());

            Long commentCount = commentMapper.selectCount(
                    new QueryWrapper<Comment>()
                            .eq("post_id", post.getId())
            );
            post.setCommentCount(commentCount.intValue());

            User author = userMapper.selectById(post.getUserId());
            if (author != null && author.getAvatar() != null) {
                post.setUserAvatar(author.getAvatar());
            }

            hotPosts.add(post);
        }

        hotPosts.sort((p1, p2) -> {
            int score1 = p1.getLikeCount() * 2 + p1.getCommentCount();
            int score2 = p2.getLikeCount() * 2 + p2.getCommentCount();
            return score2 - score1;
        });

        List<Post> topHotPosts = hotPosts.size() > 10 ? hotPosts.subList(0, 10) : hotPosts;

        redisTemplate.opsForValue().set(HOT_POSTS_CACHE_KEY, topHotPosts, HOT_POSTS_EXPIRE_MINUTES, TimeUnit.MINUTES);
        System.out.println("✓ 热门帖子已存入 Redis 缓存（30分钟）");

        return Result.success(topHotPosts);
    }

    @Operation(summary = "发布帖子")
    @PostMapping("/add")
    public Result addPost(
            @RequestBody Post post,
            HttpServletRequest request
    ){

        try {
            String token = request.getHeader("Authorization");

            if(token == null || !token.startsWith("Bearer ")){
                return Result.error("未登录");
            }

            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);
            User user = userMapper.selectById(userId);

            if(user == null){
                return Result.error("用户不存在");
            }

            post.setUserId(userId);
            post.setUsername(user.getUsername());
            post.setCreateTime(LocalDateTime.now());

            postMapper.insert(post);

            redisTemplate.delete(redisTemplate.keys(POST_LIST_CACHE_KEY + "*"));
            redisTemplate.delete(HOT_POSTS_CACHE_KEY);
            System.out.println("发布帖子后清除缓存");

            return Result.success("发布成功");

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发布失败");
        }
    }

    @Operation(summary = "上传帖子图片")
    @PostMapping("/uploadImage")
    public Result<String> uploadImage(
            @RequestParam("image") MultipartFile file,
            HttpServletRequest request
    ) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = "post_" + userId + "_" + System.currentTimeMillis() + suffix;

            String uploadDir = System.getProperty("user.dir") + "/upload/post/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            String imageUrl = "/upload/post/" + newFilename;
            return Result.success(imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if(token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }

            token = token.replace("Bearer ", "");
            Long currentUserId = JwtUtil.getUserId(token);

            Post post = postMapper.selectById(id);
            if(post == null) {
                return Result.error("帖子不存在");
            }

            if(!post.getUserId().equals(currentUserId)) {
                return Result.error("只有帖子作者才能删除");
            }

            postMapper.deleteById(id);

            redisTemplate.delete(redisTemplate.keys(POST_LIST_CACHE_KEY + "*"));
            redisTemplate.delete(POST_DETAIL_CACHE_KEY + id);
            redisTemplate.delete(POST_LIKES_CACHE_KEY + id);
            redisTemplate.delete(POST_LIKES_CACHE_KEY + id + ":*");
            redisTemplate.delete(HOT_POSTS_CACHE_KEY);
            System.out.println("删除帖子后清除缓存");

            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除出错：" + e.getMessage());
        }
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/detail/{id}")
    public Result<Post> getPostDetail(@PathVariable Long id, HttpServletRequest request){
        System.out.println("收到帖子ID = " + id);
        
        String cacheKey = POST_DETAIL_CACHE_KEY + id;
        
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            System.out.println("✓ 从 Redis 缓存获取帖子详情");
            return Result.success((Post) cachedData);
        }
        
        System.out.println("从 MySQL 数据库查询帖子详情");
        Post post = postMapper.selectById(id);
        
        if(post == null){
            return Result.error("帖子不存在");
        }
        
        String likeKey = POST_LIKES_CACHE_KEY + id;
        Object cachedLikeCount = redisTemplate.opsForValue().get(likeKey);
        
        if (cachedLikeCount != null) {
            post.setLikeCount(Integer.parseInt(cachedLikeCount.toString()));
        } else {
            Long likeCount = likeMapper.selectCount(
                    new QueryWrapper<Like>().eq("post_id", id)
            );
            post.setLikeCount(likeCount.intValue());
            redisTemplate.opsForValue().set(likeKey, likeCount, 1, TimeUnit.DAYS);
        }
        
        Long commentCount = commentMapper.selectCount(
                new QueryWrapper<Comment>()
                        .eq("post_id", id)
        );
        post.setCommentCount(commentCount.intValue());
        
        User author = userMapper.selectById(post.getUserId());
        if (author != null && author.getAvatar() != null) {
            post.setUserAvatar(author.getAvatar());
        }
        
        Long currentUserId = null;
        try {
            String token = request.getHeader("Authorization");
            if(token != null && token.startsWith("Bearer ")) {
                token = token.replace("Bearer ", "");
                currentUserId = JwtUtil.getUserId(token);
            }
        } catch (Exception e) {
        }
        
        if (currentUserId != null) {
            String userLikeKey = likeKey + ":user:" + currentUserId;
            Object isLiked = redisTemplate.opsForValue().get(userLikeKey);
            post.setLikedByCurrentUser(isLiked != null);
        } else {
            post.setLikedByCurrentUser(false);
        }
        
        redisTemplate.opsForValue().set(cacheKey, post, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        System.out.println("✓ 帖子详情已存入 Redis 缓存");
        
        return Result.success(post);
    }

    @Operation(summary = "点赞帖子")
    @PostMapping("/like/{postId}")
    public Result likePost(@PathVariable Long postId, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if(token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            String likeKey = POST_LIKES_CACHE_KEY + postId;
            String userLikeKey = likeKey + ":user:" + userId;

            Object isLiked = redisTemplate.opsForValue().get(userLikeKey);
            
            if (isLiked != null) {
                redisTemplate.delete(userLikeKey);
                redisTemplate.opsForValue().decrement(likeKey);
                System.out.println("取消点赞，帖子 ID: " + postId);
                return Result.success("取消点赞");
            } else {
                redisTemplate.opsForValue().set(userLikeKey, "1", 7, TimeUnit.DAYS);
                redisTemplate.opsForValue().increment(likeKey);
                System.out.println("点赞成功，帖子 ID: " + postId);
                return Result.success("点赞成功");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败");
        }
    }

    @Operation(summary = "获取帖子点赞数")
    @GetMapping("/likes/{postId}")
    public Result getLikeCount(@PathVariable Long postId) {
        String likeKey = POST_LIKES_CACHE_KEY + postId;
        Object count = redisTemplate.opsForValue().get(likeKey);
        
        if (count == null) {
            Long dbCount = likeMapper.selectCount(
                    new QueryWrapper<Like>().eq("post_id", postId)
            );
            redisTemplate.opsForValue().set(likeKey, dbCount, 1, TimeUnit.DAYS);
            return Result.success(dbCount);
        }
        
        return Result.success(Long.parseLong(count.toString()));
    }

    @Operation(summary = "搜索帖子")
    @GetMapping("/search")
    public Result<List<Post>> search(
            @RequestParam(defaultValue = "") String keyword
    ){
        if(keyword == null || keyword.trim().isEmpty()){
            return Result.error("搜索关键词不能为空");
        }

        List<Post> list = postMapper.selectList(
                new QueryWrapper<Post>()
                        .like("title", keyword)
                        .or()
                        .like("content", keyword)
                        .orderByDesc("id")
        );

        for(Post post : list){
            Long count = likeMapper.selectCount(
                    new QueryWrapper<Like>()
                            .eq("post_id", post.getId())
            );
            post.setLikeCount(count.intValue());
        }

        return Result.success(list);
    }

    @Operation(summary = "我发布的帖子")
    @GetMapping("/myPosts")
    public Result<List<Post>> getMyPosts(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            List<Post> posts = postMapper.selectList(
                    new QueryWrapper<Post>()
                            .eq("user_id", userId)
                            .orderByDesc("id")
            );

            for (Post post : posts) {
                Long likeCount = likeMapper.selectCount(
                        new QueryWrapper<Like>()
                                .eq("post_id", post.getId())
                );
                post.setLikeCount(likeCount.intValue());

                Long commentCount = commentMapper.selectCount(
                        new QueryWrapper<Comment>()
                                .eq("post_id", post.getId())
                );
                post.setCommentCount(commentCount.intValue());
            }

            return Result.success(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "我点赞的帖子")
    @GetMapping("/likedPosts")
    public Result<List<Post>> getLikedPosts(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            List<Like> likes = likeMapper.selectList(
                    new QueryWrapper<Like>()
                            .eq("user_id", userId)
            );

            List<Long> postIds = likes.stream()
                    .map(Like::getPostId)
                    .collect(Collectors.toList());

            if (postIds.isEmpty()) {
                return Result.success(new ArrayList<>());
            }

            List<Post> posts = postMapper.selectList(
                    new QueryWrapper<Post>()
                            .in("id", postIds)
                            .orderByDesc("id")
            );

            for (Post post : posts) {
                Long likeCount = likeMapper.selectCount(
                        new QueryWrapper<Like>()
                                .eq("post_id", post.getId())
                );
                post.setLikeCount(likeCount.intValue());

                Long commentCount = commentMapper.selectCount(
                        new QueryWrapper<Comment>()
                                .eq("post_id", post.getId())
                );
                post.setCommentCount(commentCount.intValue());
            }

            return Result.success(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取失败：" + e.getMessage());
        }
    }
}