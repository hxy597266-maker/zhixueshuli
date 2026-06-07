package com.zhixue.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`like`")
public class Like {
    @TableId
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private LocalDateTime createTime;
}