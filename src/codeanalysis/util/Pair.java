/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;
import java.io.Serializable;

public class Pair<K,V> implements Serializable {
    private static final long serialVersionUID = 1L;
    private K key;
    public K getKey() { return key; }

    private V value;
    public V getValue() { return value; }

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Pair) {
            Pair pair = (Pair) o;
            if (key != null ? !key.equals(pair.key) : pair.key != null) return false;
            if (value != null ? !value.equals(pair.value) : pair.value != null) return false;
            return true;
        }
        return false;
    }
}