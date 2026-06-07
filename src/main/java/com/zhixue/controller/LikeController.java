package com.zhixue.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhixue.entity.Like;
import com.zhixue.entity.User;
import com.zhixue.mapper.LikeMapper;
import com.zhixue.mapper.UserMapper; // 确保导入 UserMapper
import com.zhixue.utils.JwtUtil;
import com.zhixue.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private UserMapper userMapper; // 注入 UserMapper 用于查询用户名

    @PostMapping("/add")
    public Result addLike(
            @RequestBody Like like,
            HttpServletRequest request
    ){
        try {
            String token = request.getHeader("Authorization");
            if(token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);
            
            like.setUserId(userId);
            
            // 检查是否已点赞
            List<Like> existingLikes = likeMapper.selectList(
                    new QueryWrapper<Like>()
                            .eq("user_id", userId)
                            .eq("post_id", like.getPostId())
            );
            
            if (!existingLikes.isEmpty()) {
                // 已点赞，执行取消点赞（删除记录）
                likeMapper.delete(
                        new QueryWrapper<Like>()
                                .eq("user_id", userId)
                                .eq("post_id", like.getPostId())
                );
                return Result.success("取消点赞");
            } else {
                // 未点赞，执行点赞（添加记录）
                
                // 修复报错：设置 username 字段
                User user = userMapper.selectById(userId);
                if (user != null) {
                    like.setUsername(user.getUsername());
                }
                
                like.setCreateTime(LocalDateTime.now());
                likeMapper.insert(like);
                return Result.success("点赞成功");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败");
        }
    }
}