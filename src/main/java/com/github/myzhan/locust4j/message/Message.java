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
 */
public class Message {

    private final String type;
    private final Map<String, Object> data;
    private String version;
    private final String nodeID;
    private static final String TYPE_CLIENT_READY = "client_ready";

    public Message(String type, Map<String, Object> data, String version, String nodeID) {
        this.type = type;
        this.data = data;
        this.version = version;
        this.nodeID = nodeID;
    }

    public Message(byte[] bytes) throws IOException {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);

        int arrayHeader = unpacker.unpackArrayHeader();
        this.type = unpacker.unpackString();

        // unpack data
        if (unpacker.getNextFormat() != MessageFormat.NIL) {
            this.data = Message.unpackMap(unpacker);
        } else {
            unpacker.unpackNil();
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

    public static Map<String, Object> unpackMap(MessageUnpacker unpacker) throws IOException {
        int mapSize = unpacker.unpackMapHeader();
        Map<String, Object> result = new HashMap<>(6);
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
                case MAP:
                    value = unpackMap(unpacker);
                    break;
                default:
                    throw new IOException("Message received unsupported type: " + messageFormat.getValueType());
            }
            if (null != key) {
                result.put(key, value);
            }
            mapSize--;
        }
        return result;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public String getNodeID() {
        return this.nodeID;
    }

    public byte[] getBytes() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        // a message contains three fields, (type & data & nodeID)
        packer.packArrayHeader(3);

        // pack the first field
        packer.packString(this.type);

        // pack the second field
        if (Message.TYPE_CLIENT_READY.equals(this.type)) {
            if (this.version != null) {
                packer.packString(this.version);
            } else {
                packer.packNil();
            }
        } else {
            if (this.data != null) {
                packer.packMapHeader(this.data.size());
                for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                    packer.packString(entry.getKey());
                    visitor.visit(entry.getValue());
                }
            } else {
                packer.packNil();
            }
        }

        // pack the third field
        packer.packString(this.nodeID);
        byte[] bytes = packer.toByteArray();
        packer.close();
        return bytes;
    }

    @Override
    public String toString() {
        return String.format("%s-%s-%s", nodeID, type, data);
    }

}