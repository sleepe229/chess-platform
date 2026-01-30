local queue_key = KEYS[1]
local queue_ts_key = KEYS[2]

local request_id = ARGV[1]
local rating = tonumber(ARGV[2])
local now_ms = tonumber(ARGV[3])
local initial_range = tonumber(ARGV[4])
local range_increment = tonumber(ARGV[5])
local max_range = tonumber(ARGV[6])
local expansion_interval_ms = tonumber(ARGV[7])
local req_key_prefix = ARGV[8] -- e.g. "mm:req:"
local max_candidates = tonumber(ARGV[9]) or 50

local function req_key(id)
    return req_key_prefix .. id
end

local my_key = req_key(request_id)
local my_status = redis.call('HGET', my_key, 'status')
if (not my_status) or my_status ~= 'QUEUED' then
    return nil
end

local my_queued_at = tonumber(redis.call('HGET', my_key, 'queuedAtMs') or now_ms)

-- ensure membership exists
redis.call('ZADD', queue_key, 'NX', rating, request_id)
redis.call('ZADD', queue_ts_key, 'NX', my_queued_at, request_id)

local my_expansions = math.floor((now_ms - my_queued_at) / expansion_interval_ms)
local my_range = initial_range + (my_expansions * range_increment)
if my_range > max_range then
    my_range = max_range
end

local candidates = redis.call('ZRANGEBYSCORE', queue_key, rating - my_range, rating + my_range, 'LIMIT', 0, max_candidates)
for i = 1, #candidates do
    local opp_request_id = candidates[i]
    if opp_request_id ~= request_id then
        local opp_key = req_key(opp_request_id)
        local opp_status = redis.call('HGET', opp_key, 'status')
        if opp_status == 'QUEUED' then
            local opp_rating = tonumber(redis.call('HGET', opp_key, 'rating') or '0')
            local opp_queued_at = tonumber(redis.call('HGET', opp_key, 'queuedAtMs') or now_ms)

            local opp_expansions = math.floor((now_ms - opp_queued_at) / expansion_interval_ms)
            local opp_range = initial_range + (opp_expansions * range_increment)
            if opp_range > max_range then
                opp_range = max_range
            end

            local diff = math.abs(rating - opp_rating)
            local allowed = my_range
            if opp_range < allowed then
                allowed = opp_range
            end

            if diff <= allowed then
                redis.call('ZREM', queue_key, request_id, opp_request_id)
                redis.call('ZREM', queue_ts_key, request_id, opp_request_id)
                return {request_id, opp_request_id}
            end
        end
    end
end

return nil

