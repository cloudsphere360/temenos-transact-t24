package com.tem.msg.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.temenos.api.TDate;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentDueType;
import com.temenos.t24.api.complex.aa.contractapi.RepaymentSchedule;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

/**
 * TODO: Document me!
 *
 * @author lm116299
 *
 */
public class ScheduleSms extends ServiceLifecycle {

    private static final FusionFileLogger LOGGER = FusionFileLogger.getLogger(ScheduleSms.class);

    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    EbFfGenericParamSmsRecord genParam = null;
    String description = "";
    String processingDate = "";
    String apiStatus = "";
    EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(this);
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    String Mne =  sn.getCompanyRecord().getFinancialMne().toString();

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        
        List<String>selectionRecord = new ArrayList<>();
        List<String>updateRecord = new ArrayList<>();
            selectionRecord = da.selectRecords(Mne, "AA.ARRANGEMENT", "",
                    "WITH ARR.STATUS EQ CURRENT AND PRODUCT.LINE EQ LENDING");
            updateRecord.addAll(selectionRecord);

        LOGGER.info("MNE " + Mne);
        LOGGER.info("updateRecord " + updateRecord);
        return updateRecord;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        try {
        Contract api = new Contract(this);
        LOGGER.info("id" + id);
        AaArrangementRecord arr = new AaArrangementRecord(da.getRecord(Mne, "AA.ARRANGEMENT", "" , id));
        api.setContractId(id);
        DateTimeFormatter yformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        //LocalDate Todaydate = LocalDate.now();
        //String STodayDate = Todaydate.format(yformatter);
        String STodayDate = sn.getCurrentVariable("!TODAY");
        TDate OrgDate = new TDate();
        OrgDate.set(STodayDate);
        LOGGER.info("OrgDate" + OrgDate);
        TDate DueDate = new TDate();
        String DueAmount = "";
        
        for (RepaymentSchedule DueInfo : api.getRepaymentSchedule(OrgDate, DueDate)) {
            String TempNextDueDate = DueInfo.getDueDate().toString();
            LOGGER.info("TempNextDueDate" + TempNextDueDate);
            LocalDate TempNextDate = LocalDate.parse(TempNextDueDate, yformatter);
            LOGGER.info("TempNextDate" + TempNextDate);
            LocalDate TodayDate1 = LocalDate.parse(STodayDate, yformatter);
            LocalDate NextDate = TempNextDate.minusDays(2);
            LOGGER.info("TempNextDate" + TempNextDate);
            if (TodayDate1.isEqual(NextDate)) {
                for(RepaymentDueType rpt : DueInfo.getRepaymentDueType()) {
                    String repaymentMethod = rpt.getDueType().toString();
                    if(repaymentMethod.equals("CONSTANT")) {
                        DueAmount = rpt.getDueTypeAmount().toString();
                        LOGGER.info("DueAmount" + DueAmount);
                    }
                }
                
                String LoanAcct = arr.getLinkedAppl().get(0).getLinkedApplId().getValue();
                String customerId = arr.getCustomer().get(0).getCustomer().getValue();
                LOGGER.info("customerId" + customerId);
                CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
                String mobileNo = "";
                try {
                    mobileNo = cus.getPhone1().get(0).getSms1().getValue();
                LOGGER.info("mobileNo" + mobileNo);
                }catch(Exception e10) {
                    LOGGER.info("Exception e10" + e10);
                }
                String tempType = "EMI_DUE";
                String smsReqCreate = "";
                
                int cusId = 0;
                try {
                    cusId = Integer.parseInt(customerId);
                } catch (Exception e1) {

                }
                
                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
                
                String userId = "USERID";
                String userName = "USERNAME";
                
                userId = processParam(userId, genParam);
                userName = processParam(userName, genParam);
                
                
                FfMessageRequestApi requestApi = new FfMessageRequestApi();
                requestApi.setTemplateType(tempType);
                requestApi.setMobileNumber(mobileNo);
                requestApi.setLoanAccountNumber(LoanAcct);
                requestApi.setCustomerId(cusId);
                requestApi.setUserId(userId);
                requestApi.setUserName(userName);

                Params params = new Params();
                params.setArg1(DueAmount);
                params.setArg2(id);
                DateTimeFormatter dformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate TempLocalDueDate = LocalDate.parse(TempNextDueDate, yformatter);
                String LocalDueDate = TempLocalDueDate.format(dformatter);
                params.setArg3(LocalDueDate);
                params.setArg4("");

                requestApi.setParams(params);

                ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
                try {
                    smsReqCreate = ow.writeValueAsString(requestApi);
                } catch (Exception e) {
                    
                }
             
                String apiUrl = genParam.getUrl().getValue();
                
                
                SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                //SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
                
                String TableId = id+"-"+"Schedule";

                try {
                    logTab.write(TableId, logRec);
                } catch (Exception e) {
                   
                }
                break;
            } else {
                continue;
            }
        }

    }catch(Exception e5){
        LOGGER.info("e5" + e5);
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
        
        return  tempType;
    }

}
