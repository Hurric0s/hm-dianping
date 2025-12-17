---@diagnostic disable: undefined-global
---
--- Created by 衷进之
--- DateTime: 2025/12/17 17:42
---
local key = KEYS[1]
local threadId = ARGV[1]
local releaseTime = tonumber(ARGV[2])
if (redis.call("hexists", key, threadId) == 1) then
    local count = tonumber(redis.call("hget", key, threadId))
    if (count > 1) then
        redis.call("hincrby", key, threadId, -1) -- 重入次数减1
        redis.call("expire", key, releaseTime)   -- 重新设置过期时间
        return 0
    else
        return redis.call("del", key)
    end
else
    return 0
end
