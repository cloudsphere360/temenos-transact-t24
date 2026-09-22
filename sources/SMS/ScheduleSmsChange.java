package com.tem.msg.fusion;

import java.util.List;

import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.complex.aa.activityhook.TransactionData;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffschedulechangesms.EbFfScheduleChangeSmsRecord;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffschedulechangesms.EbFfScheduleChangeSmsTable;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

public class ScheduleSmsChange extends ActivityLifecycle {

    private static final FusionFileLogger logger = FusionFileLogger.getLogger(ScheduleSmsChange.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    EbFfGenericParamSmsRecord genParam = null;
    String description = "";
    String processingDate = "";
    String apiStatus = "";
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    EbFfScheduleChangeSmsRecord esc = new EbFfScheduleChangeSmsRecord(this);
    EbFfScheduleChangeSmsTable esct = new EbFfScheduleChangeSmsTable(this);
    String mne = sn.getCompanyRecord().getFinancialMne().toString();

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure currecord,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
        try {
            if (("LENDING-CHANGE-SCHD.FULL".equalsIgnoreCase(arrangementActivityRecord.getActivity().getValue())
                    || ("LENDING-RENEGOTIATE-ARRANGEMENT"
                            .equalsIgnoreCase(arrangementActivityRecord.getActivity().getValue())
                            && !arrangementActivityRecord.getLocalRefField("FF.MSG.ID").getValue().equals("")))&& arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH")) {

                String aaId = arrangementContext.getArrangementId();
                logger.info("aaId" + aaId);
                String effectiveDate = arrangementActivityRecord.getEffectiveDate().getValue();
                logger.info("effectiveDate" + effectiveDate);
                String remarks = arrangementActivityRecord.getRemarks().getValue();
                String changePaymentDate = remarks.split("-")[1];
                String dueDate = remarks.split("-")[0];

                api.setContractId(aaId);
                TDate orgDate = new TDate();
                orgDate.set(effectiveDate);
                logger.info("OrgDate" + orgDate);
               
                esc.addDueDate(dueDate);
                esc.setArrangement(aaId);
                esc.setChangeDate(changePaymentDate);
                esc.setMnemonic(mne);
                esc.setUpdatedDate(effectiveDate);
                transactionRecord.add(esc.toStructure());
                TransactionData transactionDataObj = new TransactionData();
                transactionDataObj.setVersionId("EB.FF.SCHEDULE.CHANGE.SMS,INPUT");
                transactionDataObj.setFunction("INPUT");
                transactionDataObj.setSourceId("GL.ENTRY");
                transactionDataObj.setNumberOfAuthoriser("0");
                transactionDataObj.setTransactionId(aaId + "*" + "SCHEDULE-CHANGE");
                transactionData.add(transactionDataObj);

            }
        } catch (Exception e10) {
            logger.info("e10" + e10);
        }

    }

}
