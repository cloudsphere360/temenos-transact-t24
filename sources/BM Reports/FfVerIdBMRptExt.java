package com.temenos.fusion;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.Session;

public class FfVerIdBMRptExt extends RecordLifecycle {

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        Session session = new Session(this);
        String newRecId = "";
        String currFunction = transactionContext.getCurrentFunction();
        if ("INPUT".equals(currFunction)) {
            newRecId = currentRecordId + "-" + session.getUserId();
        }
        return newRecId;
    }
}
