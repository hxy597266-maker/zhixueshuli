package com.zhixue.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhixue.common.Result;
import com.zhixue.entity.Comment;
import com.zhixue.entity.Post;
import com.zhixue.entity.User;
import com.zhixue.mapper.CommentMapper;
import com.zhixue.mapper.PostMapper;
import com.zhixue.mapper.UserMapper;
import com.zhixue.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private UserMapper userMapper;

    // 评论列表
    @GetMapping("/list")
    public Result<List<Comment>> list(Long postId){

        List<Comment> list =
                commentMapper.selectList(
                        new QueryWrapper<Comment>()
                                .eq("post_id", postId)
                                .orderByAsc("id")
                );
        
        // 确保每条评论都包含 userId（MyBatis Plus 默认会返回所有字段）
        // 打印调试信息
        System.out.println("查询到的评论数量: " + list.size());
        for (Comment c : list) {
            System.out.println("评论ID: " + c.getId() + ", 用户ID: " + c.getUserId() + ", 用户名: " + c.getUsername());
        }

        return Result.success(list);
    }

    // 获取所有评论（用于后台管理）
    @GetMapping("/all")
    public Result<List<Comment>> all() {
        List<Comment> list = commentMapper.selectList(
                new QueryWrapper<Comment>().orderByDesc("id")
        );
        return Result.success(list);
    }

    // 删除评论（只有评论作者或帖子作者可以删除）
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        try {
            // 获取当前登录用户
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            
            token = token.replace("Bearer ", "");
            Long currentUserId = JwtUtil.getUserId(token);
            
            // 查询评论
            Comment comment = commentMapper.selectById(id);
            if (comment == null) {
                return Result.error("评论不存在");
            }
            
            // 查询帖子信息，获取帖子作者
            Post post = postMapper.selectById(comment.getPostId());
            if (post == null) {
                return Result.error("帖子不存在");
            }
            
            // 验证权限：只有评论作者或帖子作者可以删除
            if (!comment.getUserId().equals(currentUserId) && !post.getUserId().equals(currentUserId)) {
                return Result.error("只有评论作者或帖子作者才能删除");
            }
            
            // 删除评论
            int result = commentMapper.deleteById(id);
            if (result > 0) {
                return Result.success(true);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除出错：" + e.getMessage());
        }
    }

    // 发布评论
    @PostMapping("/add")
    public Result<String> add(
            @RequestBody Comment comment,
            HttpServletRequest request
    ){

        String token =
                request.getHeader("Authorization");

        token = token.replace("Bearer ", "");

        Long userId =
                JwtUtil.getUserId(token);

        User user =
                userMapper.selectById(userId);

        comment.setUserId(userId);

        comment.setUsername(user.getUsername());

        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);

        return Result.success("评论成功");
    }
}