package com.zhixue.task;

import com.zhixue.entity.Post;
import com.zhixue.mapper.LikeMapper;
import com.zhixue.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LikeSyncTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private PostMapper postMapper;

    @Scheduled(fixedRate = 300000)
    public void syncLikes() {
        System.out.println("=================================");
        System.out.println("开始同步点赞数据...");

        Set<String> keys = redisTemplate.keys("post:likes:*");

        if (keys != null) {
            for (String key : keys) {
                if (key.contains(":user:")) {
                    continue;
                }

                try {
                    Object countObj = redisTemplate.opsForValue().get(key);
                    if (countObj != null) {
                        Long count = Long.parseLong(countObj.toString());
                        
                        String postIdStr = key.replace("post:likes:", "");
                        Long postId = Long.parseLong(postIdStr);

                        Post post = new Post();
                        post.setId(postId);
                        post.setLikeCount(count.intValue());

                        postMapper.updateById(post);

                        System.out.println("帖子 ID:" + postId + " 点赞数已同步: " + count);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("点赞数据同步完成");
        System.out.println("=================================");
    }
}