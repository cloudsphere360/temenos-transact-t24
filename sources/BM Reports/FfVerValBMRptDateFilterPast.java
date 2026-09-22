package com.temenos.fusion;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebffbmreportext.EbFfBmReportExtRecord;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfVerValBMRptDateFilterPast extends RecordLifecycle {

    public static final String DATE_RANGE_ERR = "EB-FF.BM.PAST.MAX.DT.RANGE";
    public static final String DATE_FILTER_ERR = "EB-FF.BM.DATE.FILTER.RANGE";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    boolean dateRangeErrFlag = false;
    boolean dateFilterErrFlag = false;

    String dateRangeVal = "";
    String daetFilterVal = "";
    String todayDate = "";
    String rptName = "";

    DataAccess da = new DataAccess(this);

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        Session session = new Session(this);
        todayDate = session.getCurrentVariable("!TODAY");

        String userRemoved = currentRecordId.substring(0, currentRecordId.lastIndexOf("-"));
        rptName = userRemoved.substring(0, userRemoved.lastIndexOf("."));

        EbFfBmReportExtRecord ffBmRptExtRec = new EbFfBmReportExtRecord(currentRecord);
        String dateFrom = ffBmRptExtRec.getDateFrom().getValue();
        String dateTo = ffBmRptExtRec.getDateTo().getValue();

        processDateValidation(dateFrom, dateTo);

        if (dateFilterErrFlag) {
            throw new T24CoreException(covertParamValue(daetFilterVal), DATE_FILTER_ERR);
        }
        if (dateRangeErrFlag) {
            throw new T24CoreException(covertParamValue(dateRangeVal), DATE_RANGE_ERR);
        }

        return ffBmRptExtRec.getValidationResponse();

    }

    public void processDateValidation(String startDate, String endDate) {
        try {

            String dateParamId = "FF.BM.REPORT.DATE.RANGE";
            String dateParamName = rptName;
            String defDtParamId = "FF.BM.REPORT.DATE.DEFAULT";

            dateRangeVal = getEbFfParamRecDets(dateParamId, dateParamName);
            daetFilterVal = getEbFfParamRecDets(defDtParamId, dateParamName);

            if ((startDate != null && !startDate.isEmpty()) && (endDate != null && !endDate.isEmpty())) {
                LocalDate todayDt = LocalDate.parse(todayDate, formatter);
                LocalDate stDt = LocalDate.parse(startDate, formatter);
                LocalDate endDt = LocalDate.parse(endDate, formatter);

                LocalDate expectedStDt = getEndDateBasedOnParamRec(dateRangeVal, todayDt);

                boolean isRangeValid = !stDt.isBefore(expectedStDt) && !endDt.isAfter(todayDt);
                if (!isRangeValid) {
                    dateRangeErrFlag = true;
                }

                LocalDate defExpStdate = getEndDateBasedOnParamRec(daetFilterVal, endDt);

                if (stDt.isBefore(defExpStdate)) {
                    dateFilterErrFlag = true;
                }
            }

        } catch (Exception e) {
            e.getMessage();
        }
    }

    public LocalDate getEndDateBasedOnParamRec(String paramValue, LocalDate baseDate) {
        LocalDate expectedDt = null;
        try {
            if (paramValue.endsWith("D")) {
                int allowedDays = Integer.parseInt(paramValue.replace("D", ""));
                expectedDt = baseDate.minusDays(allowedDays);
            } else if (paramValue.endsWith("M")) {
                int allowedMonths = Integer.parseInt(paramValue.replace("M", ""));
                expectedDt = baseDate.minusMonths(allowedMonths);
            }
        } catch (NumberFormatException e) {
            e.getMessage();
        }
        return expectedDt;
    }

    public String getEbFfParamRecDets(String paramId, String paramName) {
        String paramVal = "";
        try {
            EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
            for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
                if (paramDesc.getParamName().getValue().equals(paramName)) {
                    paramVal = paramDesc.getParamValue().getValue();
                    return paramVal;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return paramVal;
    }

    public String covertParamValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String numberPart = value.replaceAll("\\D", "");
        String unitPart = value.replaceAll("\\d", "");

        if (numberPart.isEmpty() || unitPart.isEmpty()) {
            return value;
        }

        int num = Integer.parseInt(numberPart);
        char unit = Character.toUpperCase(unitPart.charAt(0));

        switch (unit) {
        case 'D':
            return num + (num == 1 ? " Day" : " Days");
        case 'M':
            return num + (num == 1 ? " Month" : " Months");
        default:
            return value;
        }
    }
}
