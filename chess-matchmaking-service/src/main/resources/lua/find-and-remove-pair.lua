-- Атомарный поиск и удаление пары игроков из Redis Sorted Set
-- KEYS[1] - ключ пула (matchmaking:queue:{timeControl})
-- ARGV[1] - user_id, ARGV[2] - rating (число), ARGV[3] - delta (диапазон ±)
local pool_key = KEYS[1]
local user_id = ARGV[1]
local rating = tonumber(ARGV[2])
local delta = tonumber(ARGV[3])

-- 1. Добавляем игрока в пул, если его ещё нет (NX)
redis.call('ZADD', pool_key, 'NX', rating, user_id)

-- 2. Ищем оппонентов в диапазоне [rating - delta, rating + delta]
local opponents = redis.call('ZRANGEBYSCORE', pool_key, rating - delta, rating + delta, 'LIMIT', 0, 10)

-- 3. Первый оппонент, не совпадающий с текущим игроком
local opponent_id = nil
for i = 1, #opponents do
    if opponents[i] ~= user_id then
        opponent_id = opponents[i]
        break
    end
end

if opponent_id then
    -- 4. Оба ещё в пуле — атомарно удаляем
    local user_exists = redis.call('ZSCORE', pool_key, user_id)
    local opponent_exists = redis.call('ZSCORE', pool_key, opponent_id)

    if user_exists and opponent_exists then
        redis.call('ZREM', pool_key, user_id)
        redis.call('ZREM', pool_key, opponent_id)
        return {user_id, opponent_id}
    end
end

return nil
