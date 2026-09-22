package com.tem.msg.fusion;

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
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;

public class WriteOffSms extends ActivityLifecycle {
    
    
    private static final FusionFileLogger logger = FusionFileLogger.getLogger(WriteOffSms.class);
    Session sn = new Session(this);
    DataAccess da = new DataAccess(this);
    Contract api = new Contract(this);
    String mne = sn.getCompanyRecord().getFinancialMne().toString();
    EbFusionLogSmsUpdateRecord logRec =  new EbFusionLogSmsUpdateRecord(this);
    EbFfGenericParamSmsRecord genParam = null;
    String apiStatus = "";
    String description = "";
    String mobileNo = "";
    String loanAcct = "";
    String customerId = "";
    String aaId = "";
    EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
    @Override
    public void postCoreTableUpdate(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure reecord,
            List<TransactionData> transactionData, List<TStructure> transactionRecord) {
        logger.info("arrangementActivityRecord"+ arrangementActivityRecord);
        logger.info("arrangementContext"+ arrangementContext);
        String masteraaa = masterActivityRecord.getActivity().getValue();
        logger.info("masteraaa"+ masteraaa);
        
        if(!masteraaa.equalsIgnoreCase("LENDING-SETTLE-FORECLOSURE")&& arrangementActivityRecord.getActivity().getValue().equals("LENDING-WRITE.OFF-BAL.MAINTAIN") && arrangementContext.getActivityStatus().equals("AUTH")) {
            aaId = arrangementActivityRecord.getArrangement().getValue();
            customerId = arrangementRecord.getCustomer().get(0).getCustomer().getValue();
            loanAcct  = arrangementRecord.getLinkedAppl().get(0).getLinkedApplId().getValue();
            CustomerRecord cus = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            logger.info("arrangementContext" + arrangementContext.toString());
           try {
               mobileNo = cus.getPhone1().get(0).getSms1().getValue();
               logger.info("mobileNo" + mobileNo);
               }catch(Exception e12) {
                   logger.info("e12" + e12);
               }
           String tempType = "LOAN_SETTLEMENT";
           String smsReqCreate = "";
       
           int cusId = 0;
           try {
               cusId = Integer.parseInt(customerId);
           } catch (Exception e1) {
               logger.info("e1" + e1);
           }
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
           params.setArg1(aaId);

           requestApi.setParams(params);

           ObjectWriter ow = (new ObjectMapper()).writer().withDefaultPrettyPrinter();
           try {
               smsReqCreate = ow.writeValueAsString(requestApi);
           } catch (Exception e4) {
               logger.info("e4" + e4);
           }
           
           String apiUrl = genParam.getUrl().getValue();
           
           
           SendSmsUtil.SendSms(smsReqCreate, apiUrl, logRec, description, apiStatus);
           
           
           try {
               logTab.write(loanAcct+"-"+"Writeoff", logRec);
           } catch (Exception e3) {
               logger.info("e3" + e3);
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
        
        return  tempType;
    }
        
}
