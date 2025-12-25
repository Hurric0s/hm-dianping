package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate  stringRedisTemplate;
    @Autowired
    private IUserService  userService;

    @Override
    public Result follow(Long id, Boolean isFollow) {// 关注或取关
        Long userId = UserHolder.getUser().getId();
        if (isFollow) {
            // 用户为关注，则关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            boolean success= save(follow);
            if(success){
                stringRedisTemplate.opsForSet().add(RedisConstants.USER_FOLLOW_KEY+userId.toString(),id.toString());
            }
        } else {
            // 用户已关注，删除关注信息
            boolean success= remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", id));
            if(success){
                stringRedisTemplate.opsForSet().remove(RedisConstants.USER_FOLLOW_KEY+userId.toString(),id.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result followOrNot(Long id) {// 查询是否关注
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("follow_user_id", id).count();
        return Result.ok(count > 0);

    }

    @Override
    public Result commonFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(RedisConstants.USER_FOLLOW_KEY + userId.toString(), RedisConstants.USER_FOLLOW_KEY + id.toString());
        if (Objects.isNull(intersect) || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 查询共同关注的用户信息
        List<UserDTO> userDTOList = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOList);
    }
}
