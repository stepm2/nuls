/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.protocol.base.event.handler;

import io.nuls.poc.service.intf.ConsensusService;
import io.nuls.core.chain.entity.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.SeverityLevelEnum;
import io.nuls.core.constant.TransactionConstant;
import io.nuls.core.context.NulsContext;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.ValidateResult;
import io.nuls.db.entity.NodePo;
import io.nuls.event.bus.handler.AbstractEventHandler;
import io.nuls.event.bus.service.intf.EventBroadcaster;
import io.nuls.ledger.event.TransactionEvent;
import io.nuls.ledger.service.intf.LedgerService;
import io.nuls.network.service.NetworkService;

/**
 * @author Niels
 * @date 2018/1/8
 */
public class NewTxEventHandler extends AbstractEventHandler<TransactionEvent> {

    private static NewTxEventHandler INSTANCE = new NewTxEventHandler();

    private NetworkService networkService = NulsContext.getServiceBean(NetworkService.class);
    private EventBroadcaster eventBroadcaster = NulsContext.getServiceBean(EventBroadcaster.class);
    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private ConsensusService consensusService = NulsContext.getServiceBean(ConsensusService.class);

    private NewTxEventHandler() {
    }

    public static NewTxEventHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEvent(TransactionEvent event, String fromId) {
        Transaction tx = event.getEventBody();
        if (null == tx) {
            return;
        }
        if (tx.getType() == TransactionConstant.TX_TYPE_COIN_BASE || tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH || tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
            return;
        }
//        if (TxCacheManager.TX_CACHE_MANAGER.getTx(tx.getHash()) != null) {
//            return;
//        }
//        Log.info("receive tx:("+tx.getType()+"):["+fromId+"]"+tx.getHash());
        ValidateResult result = tx.verify();
        if (result.isFailed()) {
            if (result.getErrorCode() == ErrorCode.ORPHAN_TX) {
                eventBroadcaster.broadcastHashAndCacheAysn(event, fromId);
                return;
            }
            if (result.getLevel() == SeverityLevelEnum.NORMAL_FOUL) {
                networkService.removeNode(fromId);
            } else if (result.getLevel() == SeverityLevelEnum.FLAGRANT_FOUL) {
                networkService.blackNode(fromId, NodePo.BLACK);
            }
            return;
        }
        boolean isMine = ledgerService.checkTxIsMySend(tx);
        try {
            if (isMine) {
                ledgerService.approvalTx(tx, null);
            }
            consensusService.newTx(tx);
            if (isMine) {
                eventBroadcaster.broadcastAndCache(event);
            } else {
                eventBroadcaster.broadcastHashAndCacheAysn(event, fromId);
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

}
