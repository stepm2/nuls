package io.nuls.consensus.poc.rpc.cmd;

import io.nuls.kernel.model.RpcClientResult;
import io.nuls.kernel.utils.CommandBuilder;
import io.nuls.kernel.utils.CommandHelper;
import io.nuls.kernel.model.CommandResult;
import io.nuls.kernel.processor.CommandProcessor;
import io.nuls.kernel.utils.RestFulUtils;

import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018/5/28
 */
public class GetConsensusProcessor implements CommandProcessor {

    private RestFulUtils restFul = RestFulUtils.getInstance();

    @Override
    public String getCommand() {
        return "getconsensus";
    }

    @Override
    public String getHelp() {
        CommandBuilder bulider = new CommandBuilder();
        bulider.newLine(getCommandDescription());
        return bulider.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getconsensus --get the whole network consensus infomation";
    }

    @Override
    public boolean argsValidate(String[] args) {
        if(args.length != 1){
            return false;
        }
        if (!CommandHelper.checkArgsIsNull(args)) {
            return false;
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        RpcClientResult result = restFul.get("/consensus",null);
        if (result.isFailed()) {
            return CommandResult.getFailed(result.getMsg());
        }
        Map<String, Object> map = (Map)result.getData();
        map.put("rewardOfDay", CommandHelper.naToNuls(map.get("rewardOfDay")));
        map.put("totalDeposit", CommandHelper.naToNuls(map.get("totalDeposit")));
        result.setData(map);
        return CommandResult.getResult(result);
    }
}
