package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

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

    @Override
    public Result follow(Long id, Boolean isFollow) {// 关注或取关
        Long userId = UserHolder.getUser().getId();
        if (isFollow) {
            // 用户为关注，则关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            save(follow);
        } else {
            // 用户已关注，删除关注信息
            remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", id));

        }
        return Result.ok();
    }

    @Override
    public Result followOrNot(Long id) {// 查询是否关注
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("follow_user_id", id).count();
        return Result.ok(count > 0);

    }
}
