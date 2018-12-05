package com.github.myzhan.locust4j.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

/**
 * @author vrajat
 * @date 2018/12/05
 */
public class Message {

    private String type;
    private Map<String, Object> data;
    private String nodeID;

    public Message(byte[] bytes) throws IOException {

        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);

        int arrayHeader = unpacker.unpackArrayHeader();
        this.type = unpacker.unpackString();
        // unpack data
        if (unpacker.getNextFormat() != MessageFormat.NIL) {
            int mapSize = unpacker.unpackMapHeader();
            this.data = new HashMap<String, Object>(6);
            while (mapSize > 0) {
                String key = null;
                // unpack key
                if (unpacker.getNextFormat() == MessageFormat.NIL) {
                    unpacker.unpackNil();
                } else {
                    key = unpacker.unpackString();
                }
                // unpack value
                MessageFormat messageFormat = unpacker.getNextFormat();
                Object value;

                switch (messageFormat.getValueType()) {
                    case BOOLEAN:
                        value = unpacker.unpackBoolean();
                        break;
                    case FLOAT:
                        value = unpacker.unpackFloat();
                        break;
                    case INTEGER:
                        value = unpacker.unpackInt();
                        break;
                    case NIL:
                        value = null;
                        unpacker.unpackNil();
                        break;
                    case STRING:
                        value = unpacker.unpackString();
                        break;
                    default:
                        throw new IOException("Message received unsupported type: " + messageFormat.getValueType());
                }
                if (null != key) {
                    this.data.put(key, value);
                }
                mapSize--;
            }

        } else {
            this.data = null;
        }
        if (unpacker.getNextFormat() != MessageFormat.NIL) {
            this.nodeID = unpacker.unpackString();
        } else {
            unpacker.unpackNil();
            this.nodeID = null;
        }
        unpacker.close();
    }

    public Message(String type, Map data, String nodeID) {
        this.type = type;
        this.data = data;
        this.nodeID = nodeID;
    }

    public String getType() {
        return this.type;
    }

    public Map getData() {
        return this.data;
    }

    public byte[] getBytes() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        // a message contains three fields, (type & data & nodeID)
        packer.packArrayHeader(3);
        packer.packString(this.type);
        if (this.data != null) {
            packer.packMapHeader(this.data.size());
            for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                packer.packString(entry.getKey());
                visitor.visit(entry.getValue());
            }
        } else {
            packer.packNil();
        }
        packer.packString(this.nodeID);
        byte[] bytes = packer.toByteArray();
        packer.close();
        return bytes;
    }

}