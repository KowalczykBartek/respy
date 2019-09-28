package respy.core;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Resp3Parser {

    public Resp3Response process(ByteBuf byteBuf) {
        Resp3Response resp3Response = null;

        byte current = byteBuf.getByte(0); //read, but do not modify index

        if (current == '*') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == '$') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == '#') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == ':') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == '+') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == '%') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3SimpleResponse(resp3Object);
        } else if (current == '>') {
            Resp3Object resp3Object = process0(byteBuf);
            resp3Response = new Resp3PushResponse(resp3Object);
        } else {
            throw new RuntimeException("Mi scusi !");
        }

        return resp3Response;
    }

    public Resp3Object process0(ByteBuf byteBuf) {
        byte current = byteBuf.readByte();
        if (current == '*' || current == '>') {
            return new Resp3Object(processArray(byteBuf), null);
        } else if (current == '$') {
            //simple string
            int length = getInt(byteBuf);
            ignoreTrailingCRLF(byteBuf);
            byte[] simpleString = new byte[length];
            byteBuf.readBytes(simpleString);
            ignoreTrailingCRLF(byteBuf);
            return new Resp3Object((new String(simpleString)), null);
        } else if (current == '#') {
            byte booleanValue = byteBuf.readByte();
            ignoreTrailingCRLF(byteBuf);
            return new Resp3Object(booleanValue != 'f', null);
        } else if (current == ':') {
            long value = getLong(byteBuf);
            ignoreTrailingCRLF(byteBuf);
            return new Resp3Object(value, null);
        } else if (current == '+') {
            byte[] almostString = getBytesUntilCRLF(byteBuf);
            ignoreTrailingCRLF(byteBuf);
            return new Resp3Object(new String(almostString), null);
        } else if (current == '%') {
            int mapLength = getInt(byteBuf);
            ignoreTrailingCRLF(byteBuf);
            Map map = new HashMap(mapLength);
            for (int i = 0; i < mapLength; i++) {
                Object key = process(byteBuf);
                Object value = process(byteBuf);
                map.put(key, value);
            }
            return new Resp3Object(map, null);
        } else {
            throw new RuntimeException("Mi scusi !");
        }
    }

    private List<Object> processArray(ByteBuf byteBuf) {
        int count = getInt(byteBuf);
        List<Object> array = new ArrayList<>(count);
        ignoreTrailingCRLF(byteBuf);
        for (int i = 0; i < count; i++) {
            array.add(process(byteBuf));
        }

        return array;
    }

    private byte[] getBytesUntilCRLF(ByteBuf byteBuf) {
        int clrn = byteBuf.indexOf(byteBuf.readerIndex(), byteBuf.writerIndex(), (byte) '\r');
        if (byteBuf.getByte(clrn) == '\r' && byteBuf.getByte(clrn + 1) == '\n') {
            int bytesCount = clrn - byteBuf.readerIndex();
            byte[] bytes = new byte[bytesCount];
            byteBuf.readBytes(bytes);
            return bytes;
        } else {
            throw new RuntimeException("Mi scusi !");
        }
    }

    //RESP spec tells that it should be long but it makes no sense, i will never have an array bigger than Int.MAX XD
    private int getInt(ByteBuf byteBuf) {
        //i hope that escape analysis will take place and JIT will replace that shit with on stack operations.
        return Integer.valueOf(new String(getBytesUntilCRLF(byteBuf)));
    }

    private long getLong(ByteBuf byteBuf) {
        return Long.valueOf(new String(getBytesUntilCRLF(byteBuf)));
    }

    private void ignoreTrailingCRLF(ByteBuf byteBuf) {
        byteBuf.readerIndex(byteBuf.readerIndex() + 2);
    }
}
