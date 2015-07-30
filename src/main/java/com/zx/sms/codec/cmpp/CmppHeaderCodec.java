package com.zx.sms.codec.cmpp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.DefaultHeader;
import com.zx.sms.codec.cmpp.msg.DefaultMessage;
import com.zx.sms.codec.cmpp.msg.Header;
import com.zx.sms.codec.cmpp.msg.Message;
import com.zx.sms.codec.cmpp.packet.CmppHead;
import com.zx.sms.common.GlobalConstance;
import com.zx.sms.connect.manager.cmpp.CMPPEndpointEntity;

/**
 *
 * @author huzorro(huzorro@gmail.com)
 * @author Lihuanghe(18852780@qq.com)
 */
public class CmppHeaderCodec extends MessageToMessageCodec<ByteBuf, Message> {
	private final Logger logger = LoggerFactory.getLogger(CmppHeaderCodec.class);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> list) throws Exception {

		Header header = new DefaultHeader();
		header.setPacketLength(bytebuf.readUnsignedInt());
		header.setCommandId(bytebuf.readUnsignedInt());
		header.setSequenceId(bytebuf.readUnsignedInt());
		header.setHeadLength(CmppHead.COMMANDID.getHeadLength());
		header.setBodyLength(header.getPacketLength() - header.getHeadLength());

		Message message = new DefaultMessage();
		if (header.getBodyLength() > 0) {
			ByteBuf bodyBuffer = bytebuf.readSlice((int) header.getBodyLength());
			message.setBodyBuffer(bodyBuffer);
			// 增加引用记数，因为MessageToMessageCodec会释放掉bytebuf
			ReferenceCountUtil.retain(bodyBuffer);
		} else {
			message.setBodyBuffer(GlobalConstance.emptyByte);
		}
		
		message.setHeader(header);
		list.add(message);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Message message, List<Object> list) throws Exception {
		ByteBuf bodybuf = message.getBodyBuffer();
		int headerLength = CmppHead.COMMANDID.getHeadLength();
		int packetLength = bodybuf.readableBytes() + headerLength;

		// buf由netty写channel的时候释放
		ByteBuf buf = ctx.alloc().buffer(packetLength);
		buf.writeInt(packetLength);
		buf.writeInt(((Long) message.getHeader().getCommandId()).intValue());
		buf.writeInt(((Long) message.getHeader().getSequenceId()).intValue());
		if (packetLength > headerLength) {
			buf.writeBytes(bodybuf);
		}
		list.add(buf);
		// 手动释放bodybuf
		ReferenceCountUtil.release(bodybuf);
	}

}
