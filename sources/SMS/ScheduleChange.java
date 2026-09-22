package com.tem.msg.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebffschedulechangesms.EbFfScheduleChangeSmsRecord;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffschedulechangesms.EbFfScheduleChangeSmsTable;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

public class ScheduleChange extends ActivityLifecycle {

    private static final FusionFileLogger logger = FusionFileLogger.getLogger(ScheduleChange.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    String mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFfGenericParamSmsRecord genParam = null;
    EbFfScheduleChangeSmsRecord esc = new EbFfScheduleChangeSmsRecord(this);
    EbFfScheduleChangeSmsTable esct = new EbFfScheduleChangeSmsTable(this);
    String apiStatus = "";
    String description = "";
    String mobileNo = "";
    String loanAcct = "";
    String customerId = "";
    String aaId = "";
    String changeDate = "";
    String dueDate = "";
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);

    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure aaRecord,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {

        if (arrangementContext.getActivityStatus().equalsIgnoreCase("AUTH") && "LENDING-CHANGE-PAYMENT.SCHEDULE"
                .equalsIgnoreCase(arrangementActivityRecord.getActivity().getValue())) {
            aaId = arrangementActivityRecord.getArrangement().getValue();
            customerId = arrangementRecord.getCustomer().get(0).getCustomer().getValue();
            loanAcct = arrangementRecord.getLinkedAppl().get(0).getLinkedApplId().getValue();
            CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            try {
                String chgPayDateUpdDate = arrangementActivityRecord.getRemarks().getValue();
                changeDate = chgPayDateUpdDate.split("-")[1];
                dueDate = chgPayDateUpdDate.split("-")[0];
                mobileNo = cus.getPhone1().get(0).getSms1().getValue();
                logger.info("mobileNo" + mobileNo);

                String tempType = "LOAN_REVISED_SCHEDULE";
                String smsReqCreate = "";

                int cusId = 0;

                cusId = Integer.parseInt(customerId);

                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));

                String userId = "USERID";
                String userName = "USERNAME";

                userId = processParam(userId, genParam);
                userName = processParam(userName, genParam);

                FfMessageRequestApi requestApi = new FfMessageRequestApi();
                requestApi.setTemplateType(tempType);
                requestApi.setMobileNumber(mobileNo);
                requestApi.setLoanAccountNumber(loanAcct);
                requestApi.setCustomerId(cusId);
                requestApi.setUserId(userId);
                requestApi.setUserName(userName);

                Params params = new Params();
                LocalDate billdate = LocalDate.parse(dueDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                String orgBillDates = billdate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                params.setArg1(orgBillDates);
                LocalDate chgbilldate = LocalDate.parse(changeDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                String orgChgbilldate = chgbilldate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                params.setArg2(orgChgbilldate);

                requestApi.setParams(params);

                ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();

                smsReqCreate = ow.writeValueAsString(requestApi);

                String apiUrl = genParam.getUrl().getValue();

                SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);

                logTab.write(aaId + "-" + "Schedule.Change", logRec);

            } catch (Exception e12) {
                logger.info("e12" + e12);
            }

        }

    }

    private static String processParam(String payMethod, EbFfGenericParamSmsRecord genParam) {
        String tempType = "";
        for (KeyNameClass temp : genParam.getKeyName()) {
            if (temp.getKeyName().getValue().equals(payMethod)) {
                tempType = temp.getKeyValue().getValue();
                logger.info("tempType" + tempType);
                break;
            }

        }

        return tempType;
    }

}
