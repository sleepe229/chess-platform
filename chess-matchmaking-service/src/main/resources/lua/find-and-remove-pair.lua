local pool_key = KEYS[1]
local user_id = ARGV[1]
local rating = tonumber(ARGV[2])
local delta = tonumber(ARGV[3])

redis.call('ZADD', pool_key, 'NX', rating, user_id)

local opponents = redis.call('ZRANGEBYSCORE', pool_key, rating - delta, rating + delta, 'LIMIT', 0, 10)

local opponent_id = nil
for i = 1, #opponents do
    if opponents[i] ~= user_id then
        opponent_id = opponents[i]
        break
    end
end

if opponent_id then
    local user_exists = redis.call('ZSCORE', pool_key, user_id)
    local opponent_exists = redis.call('ZSCORE', pool_key, opponent_id)

    if user_exists and opponent_exists then
        redis.call('ZREM', pool_key, user_id)
        redis.call('ZREM', pool_key, opponent_id)
        return {user_id, opponent_id}
    end
end

return nil
