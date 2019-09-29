package respy.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ParsingHandler extends ByteToMessageDecoder {

    private final static Logger LOG = LogManager.getLogger();
    private final Resp3Parser resp3Parser = new Resp3Parser();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        msg.markReaderIndex();
        msg.markWriterIndex();

        try {
            Resp3Response response = resp3Parser.doProcess(msg);
            out.add(response);
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to parse ! postpone ...");
            }
            msg.resetReaderIndex();
            msg.resetWriterIndex();
        }
    }

}
