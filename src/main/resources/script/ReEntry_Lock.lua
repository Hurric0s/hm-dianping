---@diagnostic disable: undefined-global
---
--- Created by 衷进之
--- DateTime: 2025/12/17 17:29
---
local key = KEYS[1]
local threadId = ARGV[1]
local releaseTime = tonumber(ARGV[2])

if redis.call("hexists", key, threadId) == 0 then
    redis.call("hset", key, threadId, 1)
    redis.call("expire", key, releaseTime)
else
    redis.call("hincrby", key, threadId,'1')-- 重入次数加1
    redis.call("expire", key, releaseTime)-- 重新设置过期时间
end
