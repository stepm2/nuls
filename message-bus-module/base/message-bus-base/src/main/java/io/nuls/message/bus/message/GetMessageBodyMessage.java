package io.nuls.message.bus.message;

import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.utils.NulsByteBuffer;
import io.nuls.message.bus.constant.MessageBusConstant;
import io.nuls.protocol.message.base.BaseMessage;

/**
 * @author: Charlie
 * @date: 2018/5/6
 */
public class GetMessageBodyMessage extends BaseMessage<NulsDigestData> {

    public GetMessageBodyMessage() {
        super(MessageBusConstant.MODULE_ID_MESSAGE_BUS, MessageBusConstant.MSG_TYPE_GET_MSG_BODY_MSG);
    }

    @Override
    protected NulsDigestData parseMessageBody(NulsByteBuffer byteBuffer) throws NulsException {
        return byteBuffer.readHash();
    }

}
