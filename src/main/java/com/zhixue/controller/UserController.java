package com.zhixue.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhixue.common.Result;
import com.zhixue.entity.Comment;
import com.zhixue.entity.Like;
import com.zhixue.entity.Post;
import com.zhixue.entity.User;
import com.zhixue.mapper.CommentMapper;
import com.zhixue.mapper.LikeMapper;
import com.zhixue.mapper.PostMapper;
import com.zhixue.service.UserService;
import com.zhixue.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 1. 确保导入 MultipartFile
import java.io.File; // 2. 确保导入 File
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private LikeMapper likeMapper;

    @GetMapping("/list")
    public Result<List<User>> getAll() {
        return Result.success(userService.list());
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {

            // 当前登录用户ID
            Long userId = JwtUtil.getUserId(token);

            User user = userService.getById(userId);

            if (user == null) {
                return Result.error("用户不存在");
            }

            // =========================
            // 查询当前用户发布的所有帖子
            // =========================
            List<Post> postList = postMapper.selectList(
                    new QueryWrapper<Post>()
                            .eq("user_id", userId)
            );

            // 发布帖子数
            Long postCount = (long) postList.size();

            // 点赞数
            Long likeCount = 0L;

            // 评论数
            Long commentCount = 0L;

            // =========================
            // 遍历帖子统计点赞和评论
            // =========================
            for (Post post : postList) {

                Long postId = post.getId();

                // 当前帖子获得点赞数
                Long postLikeCount = likeMapper.selectCount(
                        new QueryWrapper<Like>()
                                .eq("post_id", postId)
                );

                // 当前帖子获得评论数
                Long postCommentCount = commentMapper.selectCount(
                        new QueryWrapper<Comment>()
                                .eq("post_id", postId)
                );

                likeCount += postLikeCount;

                commentCount += postCommentCount;
            }

            // =========================
            // 返回数据
            // =========================
            Map<String, Object> map = new HashMap<>();

            map.put("userId", user.getId());

            map.put("username", user.getUsername());

            map.put("role", user.getRole());
            

            map.put("avatar", user.getAvatar()); 

            map.put("postCount", postCount);

            map.put("likeCount", likeCount);

            map.put("commentCount", commentCount);

            return Result.success(map);

        } catch (Exception e) {

            e.printStackTrace();

            return Result.error("Token无效或已过期");
        }
    }

    @GetMapping("/{id:\\d+}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping("/add")
    public Result<Boolean> addUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return new Result<>(400, "用户名不能为空", false);
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return new Result<>(400, "密码不能为空", false);
        }
        return Result.success(userService.save(user));
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            boolean ok = userService.removeById(id);
            
            if (ok) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败，用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除出错：" + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {

        // 判断用户名是否存在
        User dbUser = userService.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();

        if (dbUser != null) {
            return Result.error("用户名已存在");
        }

        boolean save = userService.save(user);

        return Result.success(save);
    }
    @PostMapping("/login")
    public Result<String> login(@RequestBody User user){

        User dbUser = userService.lambdaQuery()
                .eq(User::getUsername,user.getUsername())
                .eq(User::getPassword,user.getPassword())
                .one();

        if(dbUser == null){
            return Result.error("用户名或密码错误");
        }

        String token = JwtUtil.createToken(
                dbUser.getId(),
                dbUser.getUsername()
        );

        return Result.success(token);
    }

    @PostMapping("/admin/login")
    public Result<String> adminLogin(@RequestBody User user){

        User dbUser = userService.lambdaQuery()
                .eq(User::getUsername,user.getUsername())
                .eq(User::getPassword,user.getPassword())
                .one();

        if(dbUser == null){
            return Result.error("账号或密码错误");
        }

        // 管理员校验（兼容 role 为 null 的情况）
        if(dbUser.getRole() == null || dbUser.getRole() != 1){
            return Result.error("你不是管理员，无权访问后台");
        }

        String token = JwtUtil.createToken(
                dbUser.getId(),
                dbUser.getUsername()
        );

        return Result.success(token);
    }

    //  添加这个上传接口
    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            HttpServletRequest request
    ) {
        try {
            // 1. 获取 Token
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            // 2. 文件名处理
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = userId + "_" + System.currentTimeMillis() + suffix;

            // 3. 保存路径（使用项目根目录下的 upload 文件夹）
            // 请确保你的 WebConfig 中配置了 addResourceLocations("file:./upload/")
            String uploadDir = System.getProperty("user.dir") + "/upload/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 4. 保存文件
            File dest = new File(uploadDir + newFilename);
            file.transferTo(dest);

            // 5. 更新数据库中的头像路径
            String imageUrl = "/upload/" + newFilename;
            User user = userService.getById(userId);
            user.setAvatar(imageUrl); 
            
            userService.updateById(user);

            return Result.success(imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    // 修改用户名
    @PutMapping("/updateUsername")
    public Result<String> updateUsername(
            @RequestBody Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            // 获取当前用户
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            String newUsername = params.get("username");
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }

            System.out.println("====== 开始修改用户名 ======");
            System.out.println("用户ID: " + userId);
            System.out.println("新用户名: " + newUsername);

            // 检查用户名是否已存在
            User existUser = userService.lambdaQuery()
                    .eq(User::getUsername, newUsername)
                    .ne(User::getId, userId)
                    .one();
            
            if (existUser != null) {
                return Result.error("用户名已存在");
            }

            // 更新用户名
            User user = userService.getById(userId);
            String oldUsername = user.getUsername();
            System.out.println("旧用户名: " + oldUsername);
            
            user.setUsername(newUsername);
            userService.updateById(user);
            System.out.println("用户表更新成功");
            
            // 同步更新所有已发布帖子的用户名
            int postUpdated = postMapper.update(
                    null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Post>()
                            .set("username", newUsername)
                            .eq("user_id", userId)
            );
            System.out.println("更新了 " + postUpdated + " 条帖子记录");
            
            // 同步更新所有评论的用户名
            int commentUpdated = commentMapper.update(
                    null,
                    new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Comment>()
                            .set("username", newUsername)
                            .eq("user_id", userId)
            );
            System.out.println("更新了 " + commentUpdated + " 条评论记录");
            System.out.println("====== 修改用户名完成 ======");

            return Result.success("修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改失败：" + e.getMessage());
        }
    }

    // 修改密码
    @PutMapping("/updatePassword")
    public Result<String> updatePassword(
            @RequestBody Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            // 获取当前用户
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            token = token.replace("Bearer ", "");
            Long userId = JwtUtil.getUserId(token);

            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");

            if (oldPassword == null || oldPassword.trim().isEmpty()) {
                return Result.error("旧密码不能为空");
            }
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return Result.error("新密码不能为空");
            }

            // 验证旧密码
            User user = userService.getById(userId);
            if (!user.getPassword().equals(oldPassword)) {
                return Result.error("旧密码错误");
            }

            // 更新密码
            user.setPassword(newPassword);
            userService.updateById(user);

            return Result.success("密码修改成功，请重新登录");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改失败：" + e.getMessage());
        }
    }
}