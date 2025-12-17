---@diagnostic disable: undefined-global
---
--- Created by 衷进之
--- DateTime: 2025/12/16 22:23
---

if(redis.call("GET", KEYS[1])==ARGV[1]) then
    return redis.call("DEL", KEYS[1])
else
    return 0
end

