package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffcreditbureaureport.EbFfCreditBureauReportRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/*-----------------------------------------------------------------------------
* @author Deepakumar S
* Date Created: 23-07-2026 
* Attached as : Verification job in
* VERSION>EB.FF.CREDIT.BUREAU.REPORT,INTERIM
* EB.API>FF.V.CREDIT.BUREAU
* 
* Description: Extract the CRB Report based on the Required field mapping
*-----------------------------------------------------------------------------*/
public class FfValCreditBureauBIncrementalRpt extends RecordLifecycle {
    DataAccess da = new DataAccess(this);
    Session ss = new Session(this);
    String todayDate = ss.getCurrentVariable("!TODAY");
    String fromDate;
    String toDate;
    String cBReportId = "";
    EbFfCreditBureauReportRecord ffCBRec = null;

    @Override
    public TValidationResponse validateField(String application, String recordId, String fieldData, TStructure record) {
        try {
            ffCBRec = new EbFfCreditBureauReportRecord(record);
            String cbrGen = ffCBRec.getCbrGeneration().getValue();
            if ("RANGE".equalsIgnoreCase(cbrGen)) {
                fromDate = ffCBRec.getFromDate().getValue();
                toDate = ffCBRec.getToDate().getValue();
                if (fromDate != null && !fromDate.isEmpty() && fromDate.compareTo(todayDate) >= 0) {
                    ffCBRec.getFromDate().setError("From Date must be less than today");
                }

                if (toDate != null && !toDate.isEmpty() && toDate.compareTo(todayDate) >= 0) {
                    ffCBRec.getToDate().setError("To Date must be less than today");
                }
            }

        } catch (Exception e) {
            ffCBRec.getFromDate().setError("Error : " + e.getMessage());
        }

        return ffCBRec.getValidationResponse();
    }
}