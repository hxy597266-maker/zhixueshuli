package com.zhixue.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("user")
public class User {
    @TableId
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    
    @TableField(insertStrategy = FieldStrategy.NOT_NULL)
    private String username;
    
    @TableField(insertStrategy = FieldStrategy.NOT_NULL)
    private String password;
    
    private Integer courseNum;
    private Integer postNum;
    private Integer aiNum;
    private Timestamp createTime;
    private String avatar;
    private Integer role;
}