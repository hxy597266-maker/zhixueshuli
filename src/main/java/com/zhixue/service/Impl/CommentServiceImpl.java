package com.zhixue.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhixue.entity.Comment;
import com.zhixue.mapper.CommentMapper;
import com.zhixue.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl
        extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    @Override
    public boolean deleteComment(Long id) {

        return this.removeById(id);

    }
}