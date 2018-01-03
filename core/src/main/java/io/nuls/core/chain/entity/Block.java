package io.nuls.core.chain.entity;

import io.nuls.core.chain.intf.NulsCloneable;
import io.nuls.core.chain.manager.BlockValidatorManager;
import io.nuls.core.chain.manager.TransactionManager;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.crypto.Utils;
import io.nuls.core.utils.io.NulsByteBuffer;
import io.nuls.core.utils.io.NulsOutputStreamBuffer;
import io.nuls.core.utils.log.Log;
import io.nuls.core.validate.NulsDataValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author win10
 * @date 2017/10/30
 */
public class Block extends BaseNulsData implements NulsCloneable {

    private BlockHeader header;

    private List<Transaction> txs;

    public Block() {
        initValidators();
    }

    private void initValidators() {
        List<NulsDataValidator> list = BlockValidatorManager.getValidators();
        for (NulsDataValidator<Block> validator : list) {
            this.registerValidator(validator);
        }
    }

    @Override
    public int size() {
        int size = header.size();

        for (Transaction tx : txs) {
            size += tx.size();
        }
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        header.serializeToStream(stream);

        for (Transaction tx : txs) {
            stream.write(tx.serialize());
        }
    }

    @Override
    protected void parse(NulsByteBuffer byteBuffer) throws NulsException {
        header = new BlockHeader();
        header.parse(byteBuffer);
        try {
            txs = TransactionManager.getInstances(byteBuffer);
        } catch (Exception e) {
            throw new NulsRuntimeException(ErrorCode.PARSE_OBJECT_ERROR);
        }
    }

    public List<Transaction> getTxs() {
        return txs;
    }

    public void setTxs(List<Transaction> txs) {
        this.txs = txs;
    }

    public BlockHeader getHeader() {
        return header;
    }

    public void setHeader(BlockHeader header) {
        this.header = header;
    }


    @Override
    public Object copy() {
        //Temporary non realization
        try {
            return this.clone();
        } catch (CloneNotSupportedException e) {
            Log.error(e);
            return null;
        }
    }

    public List<NulsDigestData> getTxHashList() {
        List<NulsDigestData> list = new ArrayList<>();
        for (Transaction tx : txs) {
            list.add(tx.getHash());
        }
        return list;
    }
}
