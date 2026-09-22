package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbmreportext.EbFfBmReportExtRecord;
import com.temenos.t24.api.records.enquiryreport.EnquiryClass;
import com.temenos.t24.api.records.enquiryreport.EnquiryReportRecord;
import com.temenos.t24.api.records.enquiryreport.SelectionClass;

public class FfVerAuthBMService extends RecordLifecycle {
    String branch = "";
    String groupBy = "";
    String ro = "";
    String dateFrom = "";
    String dateTo = "";
    String product = "";
    String center = "";
    String village = "";
    String district = "";
    String cycle = "";
    String purpose = "";
    String religion = "";
    String caste = "";
    String loanWise = "";
    String asOnDate = "";
    int valCount = 0;

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        try {
            String[] curRecIdPart = currentRecordId.split("-");
            String reportName = curRecIdPart[0];
            String userId = curRecIdPart[1];

            EbFfBmReportExtRecord ffBmReportExtRec = new EbFfBmReportExtRecord(currentRecord);
            branch = ffBmReportExtRec.getBranch().getValue();
            groupBy = ffBmReportExtRec.getGroupBy().getValue();
            dateFrom = ffBmReportExtRec.getDateFrom().getValue();
            dateTo = ffBmReportExtRec.getDateTo().getValue();
            ro = ffBmReportExtRec.getRo().getValue();
            product = ffBmReportExtRec.getProduct().getValue();
            center = ffBmReportExtRec.getCenter().getValue();
            village = ffBmReportExtRec.getVillage().getValue();
            district = ffBmReportExtRec.getDistrict().getValue();
            cycle = ffBmReportExtRec.getCycle().getValue();
            purpose = ffBmReportExtRec.getPurpose().getValue();
            religion = ffBmReportExtRec.getReligion().getValue();
            caste = ffBmReportExtRec.getCaste().getValue();
            loanWise = ffBmReportExtRec.getLoanWise().getValue();
            asOnDate = ffBmReportExtRec.getAsOnDate().getValue();

            EnquiryReportRecord enqReportRec = new EnquiryReportRecord();
            EnquiryClass enqClass = new EnquiryClass();

            addSelection(enqClass, "USER", userId);
            addSelection(enqClass, "BRANCH", branch);
            addSelection(enqClass, "GROUP.BY", groupBy);
            addSelection(enqClass, "RO", ro);
            addSelection(enqClass, "DATE.FROM", dateFrom);
            addSelection(enqClass, "DATE.TO", dateTo);
            addSelection(enqClass, "PRODUCT", product);
            addSelection(enqClass, "CENTER", center);
            addSelection(enqClass, "VILLAGE", village);
            addSelection(enqClass, "DISTRICT", district);
            addSelection(enqClass, "CYCLE", cycle);
            addSelection(enqClass, "PURPOSE", purpose);
            addSelection(enqClass, "RELIGION", religion);
            addSelection(enqClass, "CASTE", caste);
            addSelection(enqClass, "LOAN.WISE", loanWise);
            addSelection(enqClass, "AS.ON.DATE", asOnDate);
            enqReportRec.setEnquiry(enqClass, 0);

            TransactionData txnData = new TransactionData();
            txnData.setVersionId("ENQUIRY.REPORT,FF.BM.INPUT");
            txnData.setTransactionId(reportName);
            txnData.setFunction("INPUT");
            txnData.setSourceId("FF.BM.RPT");
            txnData.setNumberOfAuthoriser("0");
            transactionData.add(txnData);
            currentRecords.add(enqReportRec.toStructure());
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void addSelection(EnquiryClass enqClass, String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            SelectionClass sel = new SelectionClass();
            sel.setSelection(fieldName);
            sel.setOperand("EQ");
            sel.setList(value);
            enqClass.setSelection(sel, valCount++);
        }
    }

}
