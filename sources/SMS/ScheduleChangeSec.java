package com.tem.msg.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebffschedulechangesms.EbFfScheduleChangeSmsRecord;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

public class ScheduleChangeSec extends RecordLifecycle {
    
    private static final FusionFileLogger logger = FusionFileLogger.getLogger(ScheduleChangeSec.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    String mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateRecord logRec =  new EbFusionLogSmsUpdateRecord(this);
    EbFfGenericParamSmsRecord genParam = null;
    String apiStatus = "";
    String description = "";
    String loanAcct = "";
    String mobileNo = "";
    String customerId = "";
    String aaId = "";
    String changeDate = "";
    String dueDate = "";
    String effectiveDate = "";
    String year = "yyyyMMdd";
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            EbFfScheduleChangeSmsRecord esc = new EbFfScheduleChangeSmsRecord(currentRecord);
        aaId = esc.getArrangement().getValue();
        logger.info("aaId" + aaId);
        AaArrangementRecord arrangementRecord = new AaArrangementRecord(da.getRecord(mne, "AA.ARRANGEMENT", "", aaId));
        changeDate = esc.getChangeDate().getValue();
        logger.info("changeDate" + changeDate);
        effectiveDate = esc.getUpdatedDate().getValue();
        
        /*String t24DateStr = sn.getCurrentVariable("!TODAY");
        DateTimeFormatter yformatter = DateTimeFormatter.ofPattern(year);
        LocalDate localTodayDate = LocalDate.parse(t24DateStr, yformatter);   
        for(int i=0; i<esc.getDueDate().size(); i++) {
            String tempDate = esc.getDueDate(i).getValue();
            LocalDate localTempDate = LocalDate.parse(tempDate, yformatter);
            if(localTempDate.isAfter(localTodayDate)) 
            {*/
                dueDate = esc.getDueDate().get(0).getValue();
                logger.info("DueDate" + dueDate);
                loanAcct  = arrangementRecord.getLinkedAppl().get(0).getLinkedApplId().getValue();
                logger.info("loanAcct" + loanAcct);
                customerId = arrangementRecord.getCustomer().get(0).getCustomer().getValue();
                logger.info("customerId" + customerId);
                CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
                

                    mobileNo = getmobile(cus);
                   
                String tempType = "LOAN_REVISED_SCHEDULE";
                String smsReqCreate = "";
            
                int cusId = 0;
                cusId = getcusId(customerId);
                
                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
                
                String userId = "USERID";
                String userName = "USERNAME";
                
                userId = processParam(userId, genParam);
                userName = processParam(userName, genParam);
                
                FfMessageRequestApi requestApi = new FfMessageRequestApi();
                requestApi.setTemplateType(tempType);
                requestApi.setMobileNumber(mobileNo);
                requestApi.setLoanAccountNumber(aaId);
                requestApi.setCustomerId(cusId);
                requestApi.setUserId(userId);
                requestApi.setUserName(userName);

                Params params = new Params();
                LocalDate billdate = LocalDate.parse(dueDate, DateTimeFormatter.ofPattern(year));
                String orgBillDates = billdate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                params.setArg1(orgBillDates);
                LocalDate chgbilldate = LocalDate.parse(changeDate, DateTimeFormatter.ofPattern(year));
                String orgChgbilldate = chgbilldate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                params.setArg2(orgChgbilldate);

                requestApi.setParams(params);

                ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
                smsReqCreate = ow.writeValueAsString(requestApi);
                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
                String apiUrl = genParam.getUrl().getValue();
                
                
                SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                logTab.write(aaId+"-"+"Schedule.Change", logRec);
               
        }catch(Exception e101) {
            logger.info("e101" + e101);
        }
    }

    /**
     * @param customerId
     * @return
     */
    private int getcusId(String customerId) {
        try {
            return  Integer.parseInt(customerId);
        } catch (Exception e1) {
            logger.info("e1" + e1);
        }
        return 0;
    }

    /**
     * @param cus 
     * @return
     */
    private String getmobile(CustomerRecord cus) {
        try {
            return cus.getPhone1().get(0).getSms1().getValue();
        }catch(Exception e12) {
            logger.info("e12" + e12);
        }
        return "";
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
        
        return  tempType;
    }
    
}
