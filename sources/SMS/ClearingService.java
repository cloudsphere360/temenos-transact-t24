package com.tem.msg.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebffgenericparamsms.EbFfGenericParamSmsRecord;
import com.temenos.t24.api.records.ebffgenericparamsms.KeyNameClass;
import com.temenos.t24.api.records.ebfusionlogsmsclear.EbFusionLogSmsClearRecord;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebfusionlogsmsclear.EbFusionLogSmsClearTable;
import com.temenos.t24.api.tables.ebfusionlogsmsupdate.EbFusionLogSmsUpdateTable;



public class ClearingService extends ServiceLifecycle{
    private static final FusionFileLogger lOGGER = FusionFileLogger.getLogger(ClearingService.class);
    DataAccess da = new DataAccess(this);
    Session sn = new Session(this);
    EbFfGenericParamSmsRecord genParam = null;
    String eb = "EB.FUSION.LOG.SMS.UPDATE";
    
    String apiUrl = "";
    String description = "";
    String apiStatus = "";
    
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        
        int tempType = 0;
        List<String> updSelectionList = new ArrayList<>();
        try {
        LocalDate todayDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String todayLocalDate =todayDate.format(formatter);
        lOGGER.info("todayLocalDate: " + todayLocalDate);
        List<String> selectionList = da.selectRecords("", eb, "", " WITH STATUS NE PENDING AND STATUS NE REVERSE");
        lOGGER.info("selectionList"+selectionList);
        for(int i = 0; i<selectionList.size(); i++) {
            String iD = selectionList.get(i) ;
            EbFusionLogSmsUpdateRecord loopoff = new EbFusionLogSmsUpdateRecord(da.getRecord(eb, iD));
            String tempStatus = loopoff.getStatus().getValue();
            if(tempStatus.equals("FAILED")) {
                String retryCount= loopoff.getRetryCount().getValue();
                String dateTriggeredStr = loopoff.getDateTriggered().getValue();
                LocalDate triggeredDate = LocalDate.parse(dateTriggeredStr, formatter);
                Integer count = Integer.valueOf(retryCount);
                genParam = new EbFfGenericParamSmsRecord(da.getRecord("EB.FF.GENERIC.PARAM.SMS", "FF.SMS.URL"));
                for (KeyNameClass temp : genParam.getKeyName()) {
                    if (temp.getKeyName().getValue().equals("RETRY_COUNT")) {
                        tempType = Integer.valueOf(temp.getKeyValue().getValue());
                        lOGGER.info("tempType: " + tempType);
                        break;
                    }
                }
                
                
                LocalDate sixtyDaysAgo = todayDate.minusDays(60);
                if( count>tempType ) {
                    if (triggeredDate.isBefore(sixtyDaysAgo) && !triggeredDate.isAfter(todayDate)) {

                            updSelectionList.add(iD);
                    }
                }
            }else {
                updSelectionList.add(iD);
            }
            
        }
        lOGGER.info("updSelectionList: " + updSelectionList.toString());
                  
    }catch(Exception e) {
        lOGGER.info("exception85: " + e);
    }
        return updSelectionList;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        lOGGER.info("id: " + id);
        
        String status = "";
        String request = "";
        String dateTriggered = "";
        String retryCount = "";
        String response = "";
        
        EbFusionLogSmsUpdateRecord logRec = new EbFusionLogSmsUpdateRecord(da.getRecord(eb, id));
        if(logRec.getStatus().getValue().equals("SUCCESS")) {
            status = logRec.getStatus().getValue();
            request = logRec.getRequest().getValue();
            dateTriggered = logRec.getDateTriggered().getValue();
            retryCount = logRec.getRetryCount().getValue();
            response = logRec.getResponse().getValue();
            
        }else {
            logRec.getStatus().setValue("SUSPEND");
            status = logRec.getStatus().getValue();
            request = logRec.getRequest().getValue();
            dateTriggered = logRec.getDateTriggered().getValue();
            retryCount = logRec.getRetryCount().getValue();
            response = logRec.getResponse().getValue();
        }
        
        EbFusionLogSmsClearRecord logRecsms = new EbFusionLogSmsClearRecord(this);
        EbFusionLogSmsClearTable logTabsms = new EbFusionLogSmsClearTable(this);
        EbFusionLogSmsUpdateTable logTab = new EbFusionLogSmsUpdateTable(this);
        
        logRecsms.getStatus().setValue(status);
        logRecsms.getRequest().setValue(request);
        logRecsms.getDateTriggered().setValue(dateTriggered);
        logRecsms.getRetryCount().setValue(retryCount);
        logRecsms.getResponse().setValue(response);
        
        try {
            logTabsms.write(id, logRecsms);
            logTab.delete(id);
        } catch (Exception e) {
            lOGGER.info("exception75: " + e);
        }
       
    }
    
}

