package com.absinthe.libchecker.utils;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import androidx.annotation.IdRes;

/**
 * @author heruoxin @ CatchingNow.
 * @since 2019-07-26
 */
public final class CacheFunctionUtil {

    public static ICacheFunction get() {
        return get(0);
    }

    /**
     *
     * @param id Different IDs generate different instances
     */
    public static ICacheFunction get(@IdRes int id) {
        return computeIfAbsent(sInstances, id, key -> new CacheFunctionImpl());
    }

    public interface ICacheFunction {

        /**
         * Clear all cache
         */
        void clear();

        /**
         * cache items statically
         */
        <T> T staticCache(Supplier<T> func, Object... keys);

        /**
         * cache items with WeakHashMap
         */
        <T> T weakCache(Supplier<T> func, Object... key);

    }

    //------ Implementation ------

    private CacheFunctionUtil() {}

    @SuppressLint("UseSparseArrays")
    private static final Map<Integer, ICacheFunction> sInstances = new HashMap<>();

    private static class CacheFunctionImpl implements ICacheFunction {

        private final Map<String, Object> sStaticCache = new HashMap<>();

        private final Map<String, Object> sWeakCache = new WeakHashMap<>();

        public void clear() {
            sStaticCache.clear();
            sWeakCache.clear();
        }

        public <T> T staticCache(Supplier<T> func, Object... keys) {
            //noinspection unchecked
            return (T) computeIfAbsent(sStaticCache, calcKey(func, keys), k -> func.get());
        }

        public <T> T weakCache(Supplier<T> func, Object... keys) {
            //noinspection unchecked
            return (T) computeIfAbsent(sWeakCache, calcKey(func, keys), k -> func.get());
        }

        private String calcKey(Object func, Object... keys) {
            StringBuilder stringBuilder = new StringBuilder(func.getClass().getName());
            for (Object key : keys) stringBuilder.append(key == null ? 0 : key.hashCode());
            return stringBuilder.toString();
        }

    }

    public interface Supplier<T> { T get();}

    private interface Function<T, R> { R apply(T t);}

    // Copied from Maps.computeIfAbsent
    private static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null) throw new NullPointerException();
        V v;
        if ((v = map.get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }
        return v;
    }
}
