package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustomerdetails.EbFfCustomerDetailsRecord;
import com.temenos.t24.api.records.ebffcustomerdetails.OccupationClass;
import com.temenos.t24.api.tables.ebffcustomerdetails.EbFfCustomerDetailsTable;

public class VerAuthUpdFfCustDets extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        EbFfCustomerDetailsRecord ffCustRec = new EbFfCustomerDetailsRecord();
        EbFfCustomerDetailsTable ffCustTable = new EbFfCustomerDetailsTable(this);
        CustomerRecord currRec = new CustomerRecord(currentRecord);
        String ffCusRecId = currRec.getCoCode() + "." + currentRecordId;
        String currFun = transactionContext.getCurrentFunction();

        try {
            ffCustRec.setBranchCode(currRec.getCompanyBook().getValue());
            ffCustRec.setGroupCode(currRec.getLocalRefField("FF.GROUP.CODE").getValue());
            ffCustRec.setCustomerNumber(currentRecordId);
            ffCustRec.setLegalName(currRec.getGivenNames().getValue());
            ffCustRec.setDateOfBirth(currRec.getDateOfBirth().getValue());
            ffCustRec.setEnrollmentDate(currRec.getLocalRefField("EM.BUS.START.DATE").getValue());
            ffCustRec.setVillageId(currRec.getLocalRefField("FF.VILLAGE.ID").getValue());
            ffCustRec.setPincode(currRec.getLocalRefField("FF.CUS.PINCODE").getValue());
            ffCustRec.setDateOfDeath(currRec.getDeathDate().getValue());
            ffCustRec.setCustomerStatus(currRec.getLocalRefField("FF.CUST.STATUS").getValue());
            ffCustRec.setLatitude(currRec.getLocalRefField("FF.LATITUDE").getValue());
            ffCustRec.setLongitude(currRec.getLocalRefField("FF.LONGITUDE").getValue());
            ffCustRec.setCenterCode(currRec.getLocalRefField("FF.CENTRE").getValue());
            ffCustRec.setGender(currRec.getGender().getValue());

            setOccupationDetails(currRec, ffCustRec);

            ffCustRec.setEducationLevel(currRec.getLocalRefField("FF.EDU.LEVEL").getValue());
            ffCustRec.setMaritalStatus(currRec.getMaritalStatus().getValue());
            ffCustRec.setIsAlive(currRec.getLocalRefField("FF.IS.ALIVE").getValue());
            ffCustRec.setReligiousGroup(currRec.getLocalRefField("FF.RELIG.GROUP").getValue());
            ffCustRec.setCaste(currRec.getLocalRefField("FF.CASTE").getValue());
            ffCustRec.setLandHolding(currRec.getLocalRefField("FF.LAND.HOLD").getValue());
            ffCustRec.setPropertyType(currRec.getLocalRefField("FF.PROPE.TYPE").getValue());
            ffCustRec.setVillageName(currRec.getLocalRefField("FF.VILLAGE.NAME").getValue());
            ffCustRec.setBranchName(currRec.getLocalRefField("FF.BRANCH.NAME").getValue());
            ffCustRec.setRationCard(currRec.getLocalRefField("FF.RATION.CARD").getValue());           

        } catch (Exception e) {
            e.getMessage();
        }
        try {
            ffCustTable.write(ffCusRecId, ffCustRec);
        } catch (T24IOException e) {
            e.getMessage();
        }

        if (currFun.equals("REVERSE")) {
            try {
                ffCustTable.delete(ffCusRecId);
            } catch (T24IOException e) {
                e.getMessage();
            }
        }
    }

    public void setOccupationDetails(CustomerRecord currRec, EbFfCustomerDetailsRecord ffCustRec) {

        try {
            OccupationClass occupationList = new OccupationClass();
            occupationList.setOccupation(currRec.getLocalRefField("FF.FM.OCCUP").getValue());
            occupationList.setOccupationType(currRec.getLocalRefField("FF.OCCUP.TYPE").getValue());
            ffCustRec.setOccupation(occupationList, 0);
        } catch (Exception e) {
            e.getMessage();
        }

    }
}
