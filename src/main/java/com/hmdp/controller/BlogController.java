package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IFollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean success = blogService.save(blog);
        if (success) {
            List<Follow> followUsers = followService.query().eq("follow_user_id", user.getId()).list();
            if (!followUsers.isEmpty()) {
                followUsers.forEach(followUser -> {
                    stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY + followUser.getUserId().toString(), blog.getId().toString(), System.currentTimeMillis());
                });
            }
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @GetMapping("likes/{id}")
    public Result blogLikesList(@PathVariable("id") Long id) {//查询一篇博客的点赞列表
        return blogService.blogLikesList(id);
    }

    @GetMapping("/of/{id}")
    public Result queryOtherBlog(@RequestParam(value = "current", defaultValue = "1") Integer current, @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 实现关注推送页面的分页查询
     *
     * @param lastId 上次查询的id
     * @param offset 查询的博客数目
     * @return
     *
     */
    @GetMapping("/of/follow")
    public Result queryFollowListBlogs(@RequestParam("lastId") Long lastId, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryFollowListBlogs(lastId, offset);
    }
}
