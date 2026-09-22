package com.temenos.fusion;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.ebffparameter.EbFfParameterRecord;
import com.temenos.t24.api.records.ebffparameter.ParamDescClass;
import com.temenos.t24.api.system.DataAccess;

public class FfNofileGetBMReportFileNames extends Enquiry {
    public static final String NO_REC_ERR = "EB-NO.REC.SELECT";
    List<String> retValues = new ArrayList<>();
    String selUser = "";
    String filePath = "";

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        for (FilterCriteria filter : filterCriteria) {
            if (filter.getFieldname().equals("USER")) {
                selUser = filter.getValue();
            }
        }
        DataAccess da = new DataAccess(this);

        String paramId = "FF.BM.REPORT.EXTRACT";
        EbFfParameterRecord paramRec = new EbFfParameterRecord(da.getRecord("EB.FF.PARAMETER", paramId));
        for (ParamDescClass paramDesc : paramRec.getParamDesc()) {
            if (paramDesc.getParamName().getValue().equals("Path")) {
                filePath = paramDesc.getParamValue().getValue();
            }
        }

        if (selUser != null && !selUser.isEmpty()) {
            getFileNameByUserName(filePath, selUser);
        }

        if (retValues.isEmpty()) {
            throw new T24CoreException("", NO_REC_ERR);
        } else {
            return retValues;
        }

    }

    public void getFileNameByUserName(String filePath, String user) {
        try {
            File dir = new File(filePath);
            if (dir.exists() && dir.isDirectory()) {
                String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                File[] fileList = dir.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        String fileName = file.getName();
                        if (fileName.endsWith(".csv") && fileName.contains(user) && fileName.contains(todayDate)) {
                            retValues.add(fileName);
                        }
                    }
                    Collections.reverse(retValues);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

}
