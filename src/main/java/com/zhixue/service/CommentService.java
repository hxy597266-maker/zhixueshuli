package com.zhixue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhixue.entity.Comment;

public interface CommentService extends IService<Comment> {

    // 定义自定义删除方法
    boolean deleteComment(Long id);

}