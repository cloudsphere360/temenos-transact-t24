package com.temenos.fusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.tables.ebffeodscreen.EbFfEodScreenTable;

public class FFEODEnqBuildRtn extends Enquiry {

    private String ytoday = "";
    Session sess = new Session(this);

    private static final FusionFileLogger LOG = FusionFileLogger.getLogger(FFEODEnqBuildRtn.class);

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        LOG.info("===== START: FFEODEnqBuildRtn =====");

        try {
            ytoday = sess.getCurrentVariable("!TODAY");

            String companyID = extractCompanyId(filterCriteria);
            List<String> pendingIds;

            if (isInputProvided(companyID)) {
                pendingIds = processInputCompanies(companyID);
            } else {
                pendingIds = processAllCompanies();
            }

            validatePendingIds(pendingIds, filterCriteria);

            applyFilter(filterCriteria, pendingIds);

        } catch (T24CoreException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected exception: " + e);
        }

        LOG.info("===== END: FFEODEnqBuildRtn =====");
        return filterCriteria;
    }

    // =========================
    // Helper Methods
    // =========================

    private String extractCompanyId(List<FilterCriteria> filterCriteria) {
        for (FilterCriteria criteria : filterCriteria) {
            if ("@ID".equals(criteria.getFieldname())) {
                return criteria.getValue();
            }
        }
        return null;
    }

    private boolean isInputProvided(String companyID) {
        return companyID != null && !companyID.trim().isEmpty();
    }

    private List<String> processInputCompanies(String companyID) {
        List<String> companyList = splitCompanyIds(companyID);
        return getPendingCompanies(companyList);
    }

    private List<String> processAllCompanies() {
        DataAccess da = new DataAccess(this);
        List<String> companies = da.selectRecords("", "COMPANY", "", "");
        return getPendingCompanies(companies);
    }

    private List<String> splitCompanyIds(String companyID) {
        if (companyID.contains(" ")) {
            return new ArrayList<>(Arrays.asList(companyID.split(" ")));
        }
        List<String> list = new ArrayList<>();
        list.add(companyID);
        return list;
    }

    private List<String> getPendingCompanies(List<String> companies) {

        List<String> pendingList = new ArrayList<>();
        EbFfEodScreenTable ebodrectab = new EbFfEodScreenTable(this);

        for (String compId : companies) {
            if (!isEodDone(ebodrectab, compId)) {
                pendingList.add(compId);
            }
        }

        return pendingList;
    }

    private boolean isEodDone(EbFfEodScreenTable table, String compId) {
        String eodId = compId + "-" + ytoday;
        try {
            table.read(eodId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validatePendingIds(List<String> pendingIds, List<FilterCriteria> filterCriteria) {

        if (pendingIds.isEmpty()) {
            LOG.info("No pending records. Throwing EB-NO.REC.SELECT");
            filterCriteria.clear();
            throw new T24CoreException("", "EB-NO.REC.SELECT");
        }
    }

    private void applyFilter(List<FilterCriteria> filterCriteria, List<String> pendingIds) {

        FilterCriteria criteria = new FilterCriteria();
        criteria.setFieldname("@ID");
        criteria.setOperand("EQ");
        criteria.setValue(String.join(" ", pendingIds));

        filterCriteria.add(criteria);
    }
}