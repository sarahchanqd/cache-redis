package com.dxy.library.cache.memory.guava;

import com.dxy.library.cache.memory.IMemory;
import com.dxy.library.util.common.config.ConfigUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于guava的内存缓存器
 * @author duanxinyuan
 * 2018/8/8 19:48
 */
@Slf4j
public class GuavaCache implements IMemory {

    private LoadingCache<String, Optional<Object>> cache;

    public GuavaCache() {
        cache = CacheBuilder.newBuilder()
                .initialCapacity(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.key.capacity.initial"), 1000))
                .maximumSize(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.key.capacity.max"), 5_0000))
                .expireAfterWrite(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.expire.seconds.after.write"), 5), TimeUnit.MINUTES)
                .expireAfterAccess(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.expire.seconds.after.access"), 5), TimeUnit.MINUTES)
                .refreshAfterWrite(NumberUtils.toInt(ConfigUtils.getConfig("cache.memory.refresh.seconds.after.write"), 5), TimeUnit.MINUTES)
                .recordStats()//开启Guava Cache的统计功能
                .removalListener((RemovalListener<String, Optional<Object>>) removalNotification -> {
                    if (log.isDebugEnabled()) {
                        log.debug("guava cache removal success, key: {}, value: {}", removalNotification.getKey(), removalNotification.getValue());
                    }
                })
                .build(new CacheLoader<String, Optional<Object>>() {
                    @Override
                    public Optional<Object> load(String key) {
                        return Optional.ofNullable(get(key));
                    }
                });
    }

    @Override
    public <T> void set(String key, T value) {
        if (null == key) {
            return;
        }
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        cache.put(key, Optional.ofNullable(value));
    }

    @Override
    public <T> T get(String key) {
        if (null == key) {
            return null;
        }
        Optional<Object> optional = cache.getIfPresent(key);
        if (!Objects.requireNonNull(optional).isPresent()) {
            return null;
        }
        return (T) optional.orElse(null);
    }

    @Override
    public void del(String key) {
        if (key == null) {
            return;
        }
        cache.invalidate(key);
    }

    @Override
    public void del(String... keys) {
        if (keys == null) {
            return;
        }
        cache.invalidateAll(Lists.newArrayList(keys));
    }

}
