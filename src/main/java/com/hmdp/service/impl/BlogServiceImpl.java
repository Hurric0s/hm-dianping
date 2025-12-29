package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    private StringRedisTemplate stringRedisTemplate;


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
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return;//如果用户未登录就直接返回
        }
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Long userId = UserHolder.getUser().getId();//当前用户id,不一定是发布笔记的人
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    private void getBlogUser(Blog blog) {//查询笔记相关的用户
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
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score != null) {
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            update().setSql("liked=liked-1").eq("id", id).update();
            return Result.ok("取消点赞成功！");
        } else {
            stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());//用时间戳作为分数
            update().setSql("liked=liked+1").eq("id", id).update();
            return Result.ok("点赞成功！");
        }

    }

    @Override
    public Result blogLikesList(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (set == null || set.isEmpty()) {
            return Result.ok(Collections.emptyList());//如果没查到就返回空的集合
        } else {
            List<Long> collect = set.stream().map(Long::valueOf).collect(Collectors.toList());//将字符串集合转换为Long类型集合
            //按照collect的顺序从数据库查询用户，用for循环
            List<User> users = new ArrayList<>();
            collect.forEach(userid -> {
                User user = userService.getById(userid);
                if (user != null) {
                    users.add(user);
                }
            });

            List<UserDTO> userDTOS = BeanUtil.copyToList(users, UserDTO.class);
            return Result.ok(userDTOS);//返回前五个点赞用户
        }
    }

    @Override
    /**
     * 在 ZSet 滚动分页中，
     * count 决定单次查询数量，
     * offset 仅用于同一 score 下的去重，
     * 两者逻辑独立，
     * offset 的计算不依赖 count
     */
    public Result queryFollowListBlogs(Long lastId, Integer offset) {//实现关注推送页面的分页查询,redis zset实现
        //stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY+followUser.getUserId().toString(),blog.getId().toString(),System.currentTimeMillis());
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, lastId, offset, 2);//这一行获取分数小于等于lastId的前offset+2个元素
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(Collections.emptyList());//如果没有查到就返回空集合
        }
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;//用于记录本次查询的最小时间戳
        int os = 1;//offset,用于解决时间戳相同的分页问题
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD (id," + idStr + ")").list();
        for (Blog blog : blogs) {
            getBlogUser(blog);
            isLiked(blog);
        }
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(os);
        return Result.ok(scrollResult);
    }

}
