/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package io.nuls.consensus.poc.cache;

import io.nuls.consensus.poc.container.TxContainer;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.model.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by ln on 2018/4/13.
 */
public final class TxMemoryPool {

    private final static TxMemoryPool INSTANCE = new TxMemoryPool();

    private Map<NulsDigestData, TxContainer> container;
    private Queue<NulsDigestData> txHashQueue;

    private Map<NulsDigestData, TxContainer> orphanContainer;
    private Queue<NulsDigestData> orphanTxHashQueue;

    private TxMemoryPool() {
        txHashQueue = new LinkedBlockingDeque<>();
        container = new ConcurrentHashMap<>();

        orphanTxHashQueue = new LinkedBlockingDeque<>();
        orphanContainer = new ConcurrentHashMap<>();
    }

    public static TxMemoryPool getInstance() {
        return INSTANCE;
    }

    public boolean add(TxContainer tx, boolean isOrphan) {
        try {
            if(tx == null || tx.getTx() == null) {
                return false;
            }
            //check Repeatability
            NulsDigestData hash = tx.getTx().getHash();
            if (orphanContainer.containsKey(hash) || container.containsKey(hash)) {
                return false;
            }
            if (isOrphan) {
                orphanContainer.put(hash, tx);
                orphanTxHashQueue.offer(hash);
            } else {
                container.put(hash, tx);
                txHashQueue.offer(hash);
            }
            return true;
        } finally {
        }
    }

    /**
     * Get a TxContainer through hash, do not removeSmallBlock the memory pool after obtaining
     *
     * 通过hash获取某笔交易，获取之后不移除内存池
     * @param hash
     * @return TxContainer
     */
    public TxContainer get(NulsDigestData hash) {
        try {
            TxContainer tx = container.get(hash);
            if (tx == null) {
                tx = orphanContainer.get(hash);
            }
            return tx;
        } finally {
        }
    }

    /**
     * Get a TxContainer, the first TxContainer received, removed from the memory pool after acquisition
     *
     * 获取一笔交易，最先收到的交易，获取之后从内存池中移除
     * @return TxContainer
     */
    public TxContainer get() {
        TxContainer tx = null;
        NulsDigestData hash = txHashQueue.poll();
        if(hash != null) {
            tx = container.get(hash);
        } else {
            hash = orphanTxHashQueue.poll();
            if (hash != null) {
                tx = orphanContainer.get(hash);
            }
        }
        if(tx != null) {
            remove(tx.getTx().getHash());
        }

        return tx;
    }

    /**
     * Get a TxContainer, removed from the memory pool after acquisition
     *
     * 获取一笔交易，获取之后从内存池中移除
     * @return TxContainer
     */
    public TxContainer getAndRemove(NulsDigestData hash) {
        TxContainer tx = get(hash);
        if(tx != null) {
            remove(hash);
        }
        return tx;
    }

    public List<Transaction> getAll() {
        List<Transaction> txs = new ArrayList<>();
        Collection<TxContainer> list = container.values();
        for(TxContainer txContainer : list) {
            txs.add(txContainer.getTx());
        }
        return txs;
    }

    public List<Transaction> getAllOrphan() {
        List<Transaction> txs = new ArrayList<>();
        Collection<TxContainer> list = orphanContainer.values();
        for(TxContainer txContainer : list) {
            txs.add(txContainer.getTx());
        }
        return txs;
    }

    public boolean remove(NulsDigestData hash) {
        try {
            if (container.containsKey(hash)) {
                container.remove(hash);
                txHashQueue.remove(hash);
                return true;
            } else if (orphanContainer.containsKey(hash)) {
                orphanContainer.remove(hash);
                orphanTxHashQueue.remove(hash);
                return true;
            }
            return false;
        } finally {
        }
    }

    public boolean exist(NulsDigestData hash) {
        return container.containsKey(hash) || orphanContainer.containsKey(hash);
    }

    public void clear() {
        try {
            txHashQueue.clear();
            container.clear();

            orphanTxHashQueue.clear();
            orphanContainer.clear();
        } finally {
        }
    }
}
