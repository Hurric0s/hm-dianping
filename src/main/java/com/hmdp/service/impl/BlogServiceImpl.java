package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private StringRedisTemplate  stringRedisTemplate;


    @Resource
    private IUserService userService;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        getBlogUser(blog);//查询笔记相关的用户
        isLiked(blog);//查询用户点赞
        return Result.ok(blog);
    }
    private void isLiked(Blog blog) {
        String key=RedisConstants.BLOG_LIKED_KEY+blog.getId();
        Long userId= UserHolder.getUser().getId();//当前用户id,不一定是发布笔记的人
        Boolean isliked=stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        blog.setIsLike(isliked);
    }
    private void getBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            getBlogUser(blog);
            isLiked(blog);//查询用户点赞
        });
        return Result.ok(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likeBlog(Long id) {
        Long userId= UserHolder.getUser().getId();
        String key=RedisConstants.BLOG_LIKED_KEY+id;
        if(Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, userId.toString()))){
            stringRedisTemplate.opsForSet().remove(key,userId.toString());
            update().setSql("liked=liked-1").eq("id", id).update();
            return Result.ok("取消点赞成功！");
        }
        else {
            stringRedisTemplate.opsForSet().add(key,userId.toString());
            update().setSql("liked=liked+1").eq("id", id).update();
            return Result.ok("点赞成功！");
        }

    }
}
