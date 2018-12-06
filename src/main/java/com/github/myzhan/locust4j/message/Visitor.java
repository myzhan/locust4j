package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.msgpack.core.MessagePacker;

/**
 * @author vrajat
 * @date 2018/12/04
 */
public class Visitor {

    final MessagePacker packer;

    Visitor(MessagePacker packer) {
        this.packer = packer;
    }

    void visit(Object value) throws IOException {
        if (null == value) {
            visitNull(value);
        } else if (value instanceof String) {
            visitString(value);
        } else if (value instanceof Integer) {
            visitInt(value);
        } else if (value instanceof Long) {
            visitLong(value);
        } else if (value instanceof Boolean) {
            visitBool(value);
        } else if (value instanceof Float) {
            visitFloat(value);
        } else if (value instanceof Double) {
            visitDouble(value);
        } else if (value instanceof Map) {
            visitMap(value);
        } else if (value instanceof List) {
            visitList(value);
        } else if (value instanceof LongIntMap) {
            visitRps(value);
        } else {
            throw new IOException("Cannot pack type unknown type:" + value.getClass().getSimpleName());
        }
    }

    void visitNull(Object value) throws IOException {
        this.packer.packNil();
    }

    void visitString(Object value) throws IOException {
        packer.packString((String)value);
    }

    void visitInt(Object value) throws IOException {
        packer.packInt((Integer)value);
    }

    void visitLong(Object value) throws IOException {
        packer.packLong((Long)value);
    }

    void visitBool(Object value) throws IOException {
        packer.packBoolean((Boolean)value);
    }

    void visitFloat(Object value) throws IOException {
        packer.packFloat((Float)value);
    }

    void visitDouble(Object value) throws IOException {
        packer.packDouble((Double)value);
    }

    void visitMap(Object value) throws IOException {
        Map<String, Object> map = (Map<String, Object>)value;
        packer.packMapHeader(map.size());
        for (Map.Entry entry : map.entrySet()) {
            this.visitString(entry.getKey());
            this.visit(entry.getValue());
        }
    }

    void visitList(Object value) throws IOException {
        List<Object> list = (List<Object>)value;
        packer.packArrayHeader(list.size());
        for (Object object : list) {
            this.visit(object);
        }
    }

    void visitRps(Object value) throws IOException {
        LongIntMap longIntMap = (LongIntMap)value;
        packer.packMapHeader(longIntMap.internalStore.size());

        for (Map.Entry entry : longIntMap.internalStore.entrySet()) {
            packer.packLong((Long)entry.getKey());
            packer.packInt((Integer)entry.getValue());
        }
    }

}
