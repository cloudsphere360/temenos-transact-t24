package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffloannetoffft.EbFfLoanNetoffFtRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffloannetoffft.EbFfLoanNetoffFtTable;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfUpdateConcatForNetOff extends RecordLifecycle {
    Session ses = new Session(this);
    String ymnemonic = ses.getCompanyRecord().getFinancialMne().toString();
    private static final String L3API = "L3API";
    private static final Logger Logger = LoggerFactory.getLogger(L3API);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        try {
            Logger.info("Current Record Id: " + currentRecordId);
            EbFfLoanNetoffFtTable updNetOff = new EbFfLoanNetoffFtTable(this);
            EbFfLoanNetoffFtRecord updNetOffRec = new EbFfLoanNetoffFtRecord(this);
            // updNetOff.write("FF.NETOFF.FIRST", updNetOffRec);
            FundsTransferRecord fundsTransferObj = new FundsTransferRecord(currentRecord);
            Logger.info("FundsTransfer Record: " + fundsTransferObj.toString());
            String ffNfLoan = fundsTransferObj.getLocalRefField("FF.NETOFF.LOAN").getValue(); // arrId
            Logger.info("The original arrangement Id: "+ffNfLoan);
            String ffNfFirst = fundsTransferObj.getLocalRefField("FF.NETOFF.FIRST").getValue(); // @Id
            Logger.info("The original FT Id: "+ffNfFirst);
            updNetOffRec.setTransReference(currentRecordId, 0);
            updNetOffRec.setArrangementId(ffNfLoan);
            updNetOff.write(ffNfFirst, updNetOffRec);

        } catch (Exception e) {
            e.getMessage();
        }

    }

}
